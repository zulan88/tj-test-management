package net.wanji.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseConfigDetailVo;
import net.wanji.business.domain.vo.CasePartConfigVo;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.domain.SimpleSelect;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    private TjCasePartConfigService casePartConfigService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Override
    public Map<String, List<SimpleSelect>> init() {
        List<SysDictData> sceneTreeType = dictTypeService.selectDictDataByType(SysType.SCENE_TREE_TYPE);
        Map<String, List<SimpleSelect>> result = new HashMap<>(1);
        result.put(SysType.SCENE_TREE_TYPE, CollectionUtils.emptyIfNull(sceneTreeType).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> initEditPage(Integer sceneDetailId) throws BusinessException {
        Map<String, Object> result = new HashMap<>(3);
        List<SysDictData> partRole = dictTypeService.selectDictDataByType(SysType.PART_ROLE);

        FragmentedScenesDetailVo sceneDetail = sceneDetailService.getDetailVo(sceneDetailId);
        if (ObjectUtils.isEmpty(sceneDetail)) {
            return result;
        }
        result.put("sceneDetail", sceneDetail);
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(sceneDetail.getTrajectoryInfo(),
                SceneTrajectoryBo.class);
        if (ObjectUtils.isEmpty(sceneTrajectoryBo)) {
            throw new BusinessException("请先对场景进行选点");
        }
        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(sceneTrajectoryBo);
        List<SimpleSelect> roleList = CollectionUtils.emptyIfNull(partRole).stream()
                .map(item -> {
                    SimpleSelect simpleSelect = new SimpleSelect();
                    simpleSelect.setDictCode(item.getDictCode());
                    simpleSelect.setDictLabel(item.getDictLabel());
                    simpleSelect.setDictValue(item.getDictValue());
                    simpleSelect.setSort(item.getDictSort());
                    simpleSelect.setCssClass(item.getCssClass());
                    simpleSelect.setParts(configMap.get(item.getCssClass()));
                    return simpleSelect;
                }).collect(Collectors.toList());
        result.put(SysType.PART_ROLE, roleList);
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
    public List<CaseVo> getCases(TjCaseDto tjCaseDto) {
        List<CaseVo> result = caseMapper.selectCases(tjCaseDto);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer saveCase(TjCaseDto tjCaseDto) throws BusinessException {
        Integer caseId = tjCaseDto.getId();
        TjFragmentedSceneDetail sceneDetail = sceneDetailService.getById(tjCaseDto.getSceneDetailId());
        if (ObjectUtils.isEmpty(caseId)) {

            if (ObjectUtils.isEmpty(sceneDetail) || StringUtils.isEmpty(sceneDetail.getTrajectoryInfo())) {
                // todo 等待地图选点开发完成
                sceneDetail.setTrajectoryInfo("{\"evaluationVerify\":\"场景是否触发:触发时间延迟2s；是否发生碰撞:无；直行是否优先:是；PET: 5秒；TTC:5秒\",\"sceneDesc\":\"左转遇对向直行\",\"sceneForm\":\"AV车个数:1；障碍车个数:1；其他背景车个数:20；其他背景车车型组成:小客车: 大客车=9:1；仿真频率:10HZ\",\"vehicleTrajectory\":[{\"id\":\"wanji-001\",\"trajectory\":[{\"coordinate\":[121.2020582,31.2915328],\"frameId\":100,\"lane\":\"1\",\"pass\":true,\"position\":\"距交叉路口300m\",\"reason\":\"已校验完成\",\"speed\":0.0,\"speedUnit\":\"km/h\",\"time\":\"0\",\"type\":\"start\"},{\"coordinate\":[121.2026939,31.292028],\"frameId\":300,\"lane\":\"1\",\"pass\":true,\"position\":\"距交叉路口150m\",\"reason\":\"已校验完成\",\"speed\":30.0,\"speedUnit\":\"km/h\",\"time\":\"20\",\"type\":\"pathway\"},{\"coordinate\":[121.20293699999999,31.292235199999997],\"frameId\":380,\"lane\":\"1\",\"pass\":true,\"position\":\"距离交叉路口时距2s\",\"reason\":\"已校验完成\",\"speed\":30.0,\"speedUnit\":\"km/h\",\"time\":\"28\",\"type\":\"conflict\"},{\"coordinate\":[121.2033036,31.292546899999998],\"frameId\":500,\"lane\":\"1\",\"pass\":true,\"position\":\"交叉路口\",\"reason\":\"已校验完成\",\"speed\":0.0,\"speedUnit\":\"km/h\",\"time\":\"40\",\"type\":\"end\"}],\"type\":\"main\"},{\"id\":\"wanji-002\",\"trajectory\":[{\"coordinate\":[121.20210421141509,31.29156104282954],\"frameId\":100,\"lane\":\"1\",\"pass\":true,\"position\":\"距交叉路口300m\",\"reason\":\"已校验完成\",\"speed\":0.0,\"speedUnit\":\"km/h\",\"time\":\"0\",\"type\":\"start\"},{\"coordinate\":[121.2028383,31.2921552],\"frameId\":300,\"lane\":\"1\",\"pass\":true,\"position\":\"距交叉路口150m\",\"reason\":\"已校验完成\",\"speed\":30.0,\"speedUnit\":\"km/h\",\"time\":\"20\",\"type\":\"pathway\"},{\"coordinate\":[121.20257959999999,31.2919429],\"frameId\":380,\"lane\":\"1\",\"pass\":true,\"position\":\"距离交叉路口时距2s\",\"reason\":\"已校验完成\",\"speed\":30.0,\"speedUnit\":\"km/h\",\"time\":\"28\",\"type\":\"conflict\"},{\"coordinate\":[121.2022514,31.291597799999998],\"frameId\":500,\"lane\":\"1\",\"pass\":true,\"position\":\"交叉路口\",\"reason\":\"已校验完成\",\"speed\":0.0,\"speedUnit\":\"km/h\",\"time\":\"40\",\"type\":\"end\"}],\"type\":\"slave\"}]}");
//            throw new BusinessException("请先配置轨迹信息");
            }
            CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                    JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
            TjCase tjCase = new TjCase();
            BeanUtils.copyBeanProp(tjCase, tjCaseDto);
            tjCase.setResourcesDetailId(sceneDetail.getResourcesDetailId());
            tjCase.setCaseNumber(this.buildCaseNumber());
            tjCase.setLabel(String.join(",", tjCaseDto.getLabelList()));
            tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            tjCase.setStatus(CaseStatusEnum.TO_BE_SIMULATED.getCode());
            tjCase.setCreatedBy(SecurityUtils.getUsername());
            tjCase.setCreatedDate(LocalDateTime.now());
            this.save(tjCase);
            caseId = tjCase.getId();
        } else {
            TjCase tjCase = caseMapper.selectById(caseId);
            tjCase.setTestScene(tjCaseDto.getTestScene());
            tjCase.setEvaObject(tjCaseDto.getEvaObject());
            tjCase.setTestTarget(tjCaseDto.getTestTarget());
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
            this.updateById(tjCase);
        }

        List<TjCasePartConfig> configList = new ArrayList<>();
        for (Map.Entry<String, List<TjCasePartConfig>> partConfigs : tjCaseDto.getPartConfig().entrySet()) {
            if (CollectionUtils.isEmpty(partConfigs.getValue())) {
                continue;
            }
            for(TjCasePartConfig config : partConfigs.getValue()) {
                config.setCaseId(caseId);
                config.setParticipantRole(partConfigs.getKey());
            }
            configList.addAll(partConfigs.getValue());
        }
        if (CollectionUtils.isEmpty(configList)) {
            throw new BusinessException("请进行角色配置后保存");
        }
        QueryWrapper<TjCasePartConfig> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        boolean remove = ObjectUtils.isEmpty(tjCaseDto.getId()) || casePartConfigService.remove(deleteWrapper);
        boolean saveBatch = casePartConfigService.saveBatch(configList);
        if (!remove || !saveBatch) {
            throw new BusinessException("保存角色配置失败");
        }
        return sceneDetail.getFragmentedSceneId();
    }

    @Override
    public List<TjFragmentedSceneDetail> selectSubscenesInCase(String testType, Integer fragmentedSceneId) {
        return caseMapper.selectSubscenesInCase(testType, fragmentedSceneId);
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
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(detailInfo, CaseTrajectoryDetailBo.class);
        tjCase.setStatus(CaseStatusEnum.SIMULATION_VERIFICATION.getCode());
        tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
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
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(tjCase.getDetailInfo(), CaseTrajectoryDetailBo.class);
        List<List<Map>> e1Data = routeService.readRouteFile(tjCase.getRouteFile());
        routeService.verifyRoute(e1Data, caseTrajectoryDetailBo);

        tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
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

    @Override
    public boolean cloneCase(TjCaseDto tjCaseDto) {
        TjCase tjCase = this.getById(tjCaseDto.getId());
        tjCase.setId(null);
        tjCase.setCaseNumber(this.buildCaseNumber());
        tjCase.setCreatedBy(SecurityUtils.getUsername());
        tjCase.setUpdatedDate(LocalDateTime.now());
        tjCase.setUpdatedBy(null);
        tjCase.setUpdatedDate(null);
        return this.save(tjCase);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteCase(TjCaseDto tjCaseDto) {
        QueryWrapper<TjCasePartConfig> deleteWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(tjCaseDto.getId())) {
            deleteWrapper.eq(ColumnName.CASE_ID_COLUMN, tjCaseDto.getId());
            casePartConfigService.remove(deleteWrapper);
            return this.removeById(tjCaseDto.getId());
        }
        if (!ObjectUtils.isEmpty(tjCaseDto.getIds())) {
            deleteWrapper.in(ColumnName.CASE_ID_COLUMN, tjCaseDto.getIds());
            casePartConfigService.remove(deleteWrapper);
            return this.removeByIds(tjCaseDto.getIds());
        }
        return false;
    }

    @Override
    public void exportCases(List<TjCase> cases, String fileName) throws IOException {
        EasyExcel.write(fileName, TjCase.class).sheet("Sheet1").doWrite(cases);
    }

    @Override
    public boolean joinSimulationVerify(Integer id) throws BusinessException {
        TjCase tjCase = this.getById(id);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("未查询到对应测试用例");
        }
        if (!CaseStatusEnum.TO_BE_SIMULATED.getCode().equals(tjCase.getStatus())) {
            throw new BusinessException("状态异常：无法加入仿真验证");
        }
        tjCase.setStatus(CaseStatusEnum.SIMULATION_VERIFICATION.getCode());
        tjCase.setUpdatedDate(LocalDateTime.now());
        tjCase.setUpdatedBy(SecurityUtils.getUsername());
        return this.updateById(tjCase);
    }

    @Override
    public boolean joinTask(List<Integer> ids) throws BusinessException {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        QueryWrapper<TjCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(ColumnName.ID_COLUMN, ids).eq(ColumnName.STATUS_COLUMN, CaseStatusEnum.IN_BASE.getCode());
        long count = this.count(queryWrapper);
        if (count != ids.size()) {
            throw new BusinessException("未入库的测试用例无法加入任务");
        }
        // todo 加入任务池
        return true;
    }

    @Override
    public CaseVerificationVo getSimulationDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("测试用例不存在");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(tjCase.getDetailInfo(), CaseTrajectoryDetailBo.class);

        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        List<TjCasePartConfig> caseConfig = casePartConfigService.list(queryWrapper);
        Map<String, String> configMap = CollectionUtils.emptyIfNull(caseConfig).stream()
                .collect(Collectors.toMap(TjCasePartConfig::getBusinessId, TjCasePartConfig::getParticipantRole));

//        if (ObjectUtils.isEmpty(tjCase.getTopic()) || ObjectUtils.isEmpty(tjCase.getLocalFile())) {
//            throw new BusinessException("请先上传仿真信息");
//        }
        CaseVerificationVo result = new CaseVerificationVo(tjCase);
//        Map<String, List<Map<String, Double>>> pointMap = null;
//        try {
//            List<List<Map>> e1List = routeService.readRouteFile(tjCase.getRouteFile());
//            pointMap = routeService.extractRoute(e1List);
//        } catch (IOException e) {
//            log.error("文件读取异常");
//            return result;
//        }
//        if (!ObjectUtils.isEmpty(pointMap)) {
//            for (VehicleTrajectoryBo trajectoryBo : result.getDetailInfo().getVehicleTrajectory()) {
//                if (pointMap.containsKey(trajectoryBo.getId())) {
//                    List<Map<String, Double>> list = pointMap.get(trajectoryBo.getId());
//                    trajectoryBo.setRoute(list);
//                    int sec = (int) Math.ceil((double) list.size() / 10);
//                    trajectoryBo.setDuration(DateUtils.secondsToDuration(sec));
//                }
//            }
//        }
        return result;
    }

    @Override
    public CaseConfigDetailVo getConfigDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("未查询到对应的测试用例");
        }
        CaseConfigDetailVo caseConfigDetailVo = new CaseConfigDetailVo();
        BeanUtils.copyBeanProp(caseConfigDetailVo, tjCase);
        caseConfigDetailVo.setLabelList(Arrays.stream(tjCase.getLabel().split(",")).collect(Collectors.toList()));
        Map<String, List<CasePartConfigVo>> configInfo = casePartConfigService.getConfigInfo(caseId);
        caseConfigDetailVo.setPartConfig(configInfo);
        return caseConfigDetailVo;
    }

    public synchronized String buildCaseNumber() {
        return StringUtils.format(ContentTemplate.CASE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.CASE_NUMBER_TEMPLATE));
    }
}
