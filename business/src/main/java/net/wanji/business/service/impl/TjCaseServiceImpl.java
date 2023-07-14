package net.wanji.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.domain.bo.CaseDetailBo;
import net.wanji.business.domain.bo.VehicleTrajectoryBo;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneBaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
@Service
public class TjCaseServiceImpl extends ServiceImpl<TjCaseMapper, TjCase> implements TjCaseService {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private TjFragmentedScenesService scenesService;

    @Autowired
    private TjFragmentedSceneDetailService sceneDetailService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Override
    public Map<String, Object> init() {
        List<SysDictData> sysDictData = dictTypeService.selectDictDataByType(SysType.SCENE_TREE_TYPE);
        Map<String, Object> result = new HashMap<>(1);
        result.put("sceneTreeType", sysDictData);
        return result;
    }

    @Override
    public List<TjFragmentedScenes> selectScenesInCase(String testType, String type) {
        List<TjFragmentedScenes> scenes = caseMapper.selectSceneIdInCase(testType, type);
        List<Integer> pIds = new ArrayList<>();
        List<TjFragmentedScenes> result = new ArrayList<>();
        for (TjFragmentedScenes item : scenes) {
            result.addAll(getAllParentNode(item, pIds));
        }
        return result;
    }

    private List<TjFragmentedScenes> getAllParentNode(TjFragmentedScenes scenes, List<Integer> pIds) {
        List<TjFragmentedScenes> result = new ArrayList<>();
        result.add(scenes);
        if (!pIds.contains(scenes.getParentId()) && hasParent(scenes)) {
            pIds.add(scenes.getParentId());
            TjFragmentedScenes parentScene = scenesService.getById(scenes.getParentId());
            result.addAll(getAllParentNode(parentScene, pIds));
        }
        return result;
    }

    private boolean hasParent(TjFragmentedScenes scenes) {
        return !ObjectUtils.isEmpty(scenes) && scenes.getParentId() > 0;
    }

    @Override
    public SceneBaseVo getSceneBaseInfo(Integer fragmentedSceneId) throws BusinessException {
        FragmentedScenesDetailVo detailVo = sceneDetailService.getDetailVo(fragmentedSceneId);
        SceneBaseVo sceneBaseVo = new SceneBaseVo();
        BeanUtils.copyBeanProp(sceneBaseVo, detailVo);
        return sceneBaseVo;
    }

    @Override
    public List<CaseVo> getCases(TjCaseDto tjCaseDto) {
        List<CaseVo> result = caseMapper.selectCases(tjCaseDto);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createCase(TjCaseDto tjCaseDto) {
        if (ObjectUtils.isEmpty(tjCaseDto.getId())) {
            TjCase tjCase = new TjCase();
            BeanUtils.copyBeanProp(tjCase, tjCaseDto);
            tjCase.setCaseNumber(this.buildCaseNumber());
            tjCase.setLabel(String.join(",", tjCaseDto.getLabelList()));
            tjCase.setStatus(CaseStatusEnum.TO_BE_SIMULATED.getCode());
            tjCase.setCreatedBy(SecurityUtils.getUsername());
            tjCase.setCreatedDate(LocalDateTime.now());
            return this.save(tjCase);
        }
        return false;
    }

    @Override
    public Integer saveDetail(TjCaseDto tjCaseDto) throws BusinessException, IOException {
        TjCase tjCase = this.getById(tjCaseDto.getId());
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("用例不存在");
        }
        // 读取文件中的点位信息
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(new File(FileUploadUtils.getAbsolutePathFileName(tjCaseDto.getLocalFile())));
        String detailInfo = objectMapper.writeValueAsString(jsonNode);
        CaseDetailBo caseDetailBo = JSONObject.parseObject(detailInfo, CaseDetailBo.class);
        tjCase.setStatus(CaseStatusEnum.SIMULATION_VERIFICATION.getCode());
        tjCase.setDetailInfo(JSONObject.toJSONString(caseDetailBo));
        tjCase.setTopic(tjCaseDto.getTopic());
        tjCase.setLocalFile(tjCaseDto.getLocalFile());
        tjCase.setUpdatedBy(SecurityUtils.getUsername());
        tjCase.setUpdatedDate(LocalDateTime.now());
        boolean update = this.updateById(tjCase);
        if (!update) {
            throw new BusinessException("修改失败");
        }
        try {
            log.info("开始保存路线文件 ----------> ");
            routeService.saveRouteFile(tjCase.getId(), tjCase.getTopic(), SecurityUtils.getUsername());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return tjCase.getId();
    }


    @Override
    public boolean verifyTrajectory(Integer id) throws IOException {
        TjCase tjCase = this.getById(id);
        CaseDetailBo caseDetailBo = JSONObject.parseObject(tjCase.getDetailInfo(), CaseDetailBo.class);
        List<List<Map>> e1Data = routeService.readRouteFile(tjCase.getRouteFile());
        routeService.verifyRoute(e1Data, caseDetailBo);

        tjCase.setDetailInfo(JSONObject.toJSONString(caseDetailBo));
        tjCase.setUpdatedBy(SecurityUtils.getUsername());
        tjCase.setUpdatedDate(LocalDateTime.now());
        return this.updateById(tjCase);
    }

    @Override
    public void playback(Integer id, String vehicleId, int action) throws BusinessException, IOException {

        switch (action) {
            case PlaybackAction.START:
                TjCase tjCase = this.getById(id);
                if (ObjectUtils.isEmpty(tjCase) || StringUtils.isEmpty(tjCase.getTopic())) {
                    throw new BusinessException("用例不存在或者未录入topic");
                }
                List<List<Map>> e1List = routeService.readVehicleRouteFile(tjCase.getRouteFile(), vehicleId);
                if (CollectionUtils.isEmpty(e1List)) {
                    throw new BusinessException("未查询到轨迹");
                }
                PlaybackSchedule.startSendingData(vehicleId, e1List);
                break;
            case PlaybackAction.SUSPEND:
                PlaybackSchedule.suspend(vehicleId);
                break;
            case PlaybackAction.CONTINUE:
                PlaybackSchedule.goOn(vehicleId);
                break;
            case PlaybackAction.STOP:
                PlaybackSchedule.stopSendingData(vehicleId);
                break;
            default:
                break;

        }

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteCase(TjCaseDto tjCaseDto) {
        if (!ObjectUtils.isEmpty(tjCaseDto.getId())) {
            return this.removeById(tjCaseDto.getId());
        }
        if (!ObjectUtils.isEmpty(tjCaseDto.getIds())) {
            return this.removeByIds(tjCaseDto.getIds());
        }
        return false;
    }

    @Override
    public void exportCases(List<TjCase> cases, String fileName) throws IOException {
        EasyExcel.write(fileName, TjCase.class).sheet("Sheet1").doWrite(cases);
    }

    @Override
    public boolean joinTask(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        int result = caseMapper.updateCaseStatus(ids, CaseStatusEnum.REAL_VERIFICATION.getCode());
        return result == ids.size();
    }

    @Override
    public CaseVerificationVo getDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("测试用例不存在");
        }
        if (ObjectUtils.isEmpty(tjCase.getTopic()) || ObjectUtils.isEmpty(tjCase.getLocalFile())) {
            throw new BusinessException("请先上传仿真信息");
        }
        CaseVerificationVo result = new CaseVerificationVo(tjCase);
        Map<String, List<Map<String, Double>>> pointMap = null;
        try {
            List<List<Map>> e1List = routeService.readRouteFile(tjCase.getRouteFile());
            pointMap = routeService.extractRoute(e1List);
        } catch (IOException e) {
            log.error("文件读取异常");
            return result;
        }
        if (!ObjectUtils.isEmpty(pointMap)) {
            for (VehicleTrajectoryBo trajectoryBo : result.getDetailInfo().getVehicleTrajectory()) {
                if (pointMap.containsKey(trajectoryBo.getId())) {
                    List<Map<String, Double>> list = pointMap.get(trajectoryBo.getId());
                    trajectoryBo.setRoute(list);
                    int sec = (int) Math.ceil((double) list.size() / 10);
                    trajectoryBo.setDuration(DateUtils.secondsToDuration(sec));
                }
            }
        }
        return result;
    }

    public synchronized String buildCaseNumber() {
        return StringUtils.format(ContentTemplate.CASE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber());
    }
}
