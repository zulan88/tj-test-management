package net.wanji.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseConfigDetailVo;
import net.wanji.business.domain.vo.CasePartConfigVo;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjResourcesDetailMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.business.trajectory.RedisTrajectoryConsumer;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.core.domain.SimpleSelect;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
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
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private RestService restService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjResourcesDetailMapper resourcesDetailMapper;

    @Autowired
    private RedisTrajectoryConsumer redisTrajectoryConsumer;

    @Override
    public Map<String, List<SimpleSelect>> init() {
        List<SysDictData> sceneTreeType = dictTypeService.selectDictDataByType(SysType.SCENE_TREE_TYPE);
        Map<String, List<SimpleSelect>> result = new HashMap<>(1);
        result.put(SysType.SCENE_TREE_TYPE, CollectionUtils.emptyIfNull(sceneTreeType).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> initEditPage(Integer sceneDetailId, Integer caseId) throws BusinessException {
        Map<String, Object> result = new HashMap<>(3);
        FragmentedScenesDetailVo sceneDetail = sceneDetailService.getDetailVo(sceneDetailId);
        if (ObjectUtils.isEmpty(sceneDetail)) {
            throw new BusinessException("未找到对应场景");
        }
        result.put("sceneDetail", sceneDetail);

        if (!ObjectUtils.isEmpty(caseId)) {
            TjCase tjCase = this.getById(caseId);
            CaseVo caseVo = new CaseVo();
            BeanUtils.copyBeanProp(caseVo, tjCase);
            result.put("caseDetail", caseVo);
        }
        List<PartConfigSelect> configSelect = getConfigSelect(caseId,
                JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), SceneTrajectoryBo.class), false);
        result.put(SysType.PART_ROLE, configSelect);
        return result;
    }

    @Override
    public List<PartConfigSelect> getConfigSelect(Integer caseId,
                                                  SceneTrajectoryBo sceneTrajectoryBo,
                                                  boolean deviceConfig) {
        // 1.查询用例角色配置
        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        List<TjCasePartConfig> caseConfig = casePartConfigService.list(queryWrapper);
        Map<String, List<String>> roleBusinessIdsMap = CollectionUtils.emptyIfNull(caseConfig).stream()
                .collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole,
                        Collectors.mapping(TjCasePartConfig::getBusinessId, Collectors.toList())));
        // 2.转换用例点位配置
        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(sceneTrajectoryBo);
        // 3.查询设备信息
        Map<String, List<TjDeviceDetail>> roleDevicesMap = CollectionUtils.emptyIfNull(deviceDetailService.list())
                .stream().collect(Collectors.groupingBy(TjDeviceDetail::getSupportRoles));
        // 4.查询参与者角色字典
        List<SysDictData> partRole = dictTypeService.selectDictDataByType(SysType.PART_ROLE);
        // 5.配置匹配
        return CollectionUtils.emptyIfNull(partRole).stream()
                .map(item -> {
                    List<CasePartConfigVo> casePartConfigVos = configMap.get(item.getCssClass());
                    List<CasePartConfigVo> parts = new ArrayList<>();
                    for (CasePartConfigVo config : CollectionUtils.emptyIfNull(casePartConfigVos)) {
                        if (deviceConfig && !CollectionUtils.emptyIfNull(roleBusinessIdsMap.get(item.getDictValue()))
                                .contains(config.getBusinessId())) {
                            continue;
                        }
                        CasePartConfigVo part = new CasePartConfigVo();
                        BeanUtils.copyBeanProp(part, config);
                        if (CollectionUtils.emptyIfNull(roleBusinessIdsMap.get(item.getDictValue()))
                                .contains(config.getBusinessId())) {
                            part.setSelected(YN.Y_INT);
                        }
                        parts.add(part);
                    }
                    PartConfigSelect partConfigSelect = new PartConfigSelect();
                    partConfigSelect.setDictCode(item.getDictCode());
                    partConfigSelect.setDictLabel(item.getDictLabel());
                    partConfigSelect.setDictValue(item.getDictValue());
                    partConfigSelect.setSort(item.getDictSort());
                    partConfigSelect.setCssClass(item.getCssClass());
                    if (CollectionUtils.isNotEmpty(parts)) {
                        partConfigSelect.setParts(parts);
                        partConfigSelect.setDevices(roleDevicesMap.get(item.getDictValue()));
                    }
                    return partConfigSelect;
                }).collect(Collectors.toList());
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
                throw new BusinessException("请先配置轨迹信息");
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
            tjCase.setLabel(String.join(",", tjCaseDto.getLabelList()));
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
            this.updateById(tjCase);
        }
        boolean saveConfig = casePartConfigService.removeThenSave(caseId, tjCaseDto.getPartConfig());
        if (!saveConfig) {
            throw new BusinessException("保存角色配置失败");
        }
        return sceneDetail.getFragmentedSceneId();
    }

    @Override
    public List<TjFragmentedSceneDetail> selectSubscenesInCase(String testType, Integer fragmentedSceneId) {
        return caseMapper.selectSubscenesInCase(testType, fragmentedSceneId);
    }

    @Override
    public void playback(Integer id, String participantId, int action) throws BusinessException, IOException {
        TjCase tjCase = this.getById(id);
        if (StringUtils.equals(tjCase.getStatus(), CaseStatusEnum.TO_BE_SIMULATED.getCode())) {
            throw new BusinessException("请先进行导入");
        }
        switch (action) {
            case PlaybackAction.CALL:
                redisTrajectoryConsumer.subscribeAndSend(id, tjCase.getCaseNumber(), participantId);
                boolean start = restService.start(id, tjCase.getCaseNumber());
                if (!start) {
                    throw new BusinessException("仿真失败");
                }
                break;
            case PlaybackAction.START:
                if (ObjectUtils.isEmpty(tjCase) || StringUtils.isEmpty(tjCase.getCaseNumber())) {
                    throw new BusinessException("用例不存在");
                }
                if (StringUtils.isEmpty(tjCase.getRouteFile())) {
                    this.playback(id, participantId, PlaybackAction.CALL);
                    break;
                }
                List<List<TrajectoryValueDto>> e1List = routeService.readTrajectoryFromRouteFile(tjCase.getRouteFile(),
                        participantId);
                if (CollectionUtils.isEmpty(e1List)) {
                    throw new BusinessException("未查询到轨迹");
                }
                PlaybackSchedule.startSendingData(participantId, e1List);
                break;
            case PlaybackAction.SUSPEND:
                PlaybackSchedule.suspend(participantId);
                break;
            case PlaybackAction.CONTINUE:
                PlaybackSchedule.goOn(participantId);
                break;
            case PlaybackAction.STOP:
                PlaybackSchedule.stopSendingData(participantId);
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
    public boolean updateStatus(TjCaseDto tjCaseDto) throws BusinessException {
        QueryWrapper<TjCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(ColumnName.ID_COLUMN, tjCaseDto.getIds()).eq(ColumnName.STATUS_COLUMN, tjCaseDto.getStatus());
        List<TjCase> tjCases = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(tjCases)) {
            throw new BusinessException("操作失败");
        }
        // todo 仿真点位验证逻辑
//        if (StringUtils.equals(tjCaseDto.getStatus(), CaseStatusEnum.TO_BE_SIMULATED.getCode())) {
//            for (TjCase caseItem : tjCases) {
//                CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(caseItem.getDetailInfo(),
//                        CaseTrajectoryDetailBo.class);
//                if (caseTrajectoryDetailBo.getParticipantTrajectories().stream().noneMatch(trajectories ->
//                        routeService.verifyRoute(trajectories.getTrajectory()))) {
//                    throw new BusinessException(StringUtils.format("用例{}仿真校验失败", caseItem.getCaseNumber()));
//                }
//            }
//        }
        String nextStatus = CaseStatusEnum.getNextStatus(String.valueOf(tjCaseDto.getStatus()));
        for (TjCase tjCase : tjCases) {
            tjCase.setStatus(nextStatus);
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
        }
        return this.updateBatchById(tjCases);
    }

    @Override
    public CaseVerificationVo getSimulationDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("测试用例不存在");
        }
        TjResourcesDetail resourcesDetail = resourcesDetailMapper.selectById(tjCase.getResourcesDetailId());
        if (ObjectUtils.isEmpty(resourcesDetail) || StringUtils.isEmpty(resourcesDetail.getAttribute4())) {
            throw new BusinessException("地图资源不存在");
        }
        CaseVerificationVo result = new CaseVerificationVo(tjCase, resourcesDetail.getFilePath(),
                resourcesDetail.getAttribute4());
        if (ObjectUtils.isEmpty(tjCase.getRouteFile())) {
            return result;
        }
        Map<String, List<Map<String, Double>>> pointMap = null;
        try {
            List<List<TrajectoryValueDto>> e1List = routeService.readRouteFile(tjCase.getRouteFile());
            pointMap = routeService.extractRoute(e1List);
        } catch (IOException e) {
            log.error("文件读取异常");
            return result;
        }
        if (!ObjectUtils.isEmpty(pointMap)) {
            for (ParticipantTrajectoryBo trajectoryBo : result.getDetailInfo().getParticipantTrajectories()) {
                if (pointMap.containsKey(trajectoryBo.getId())) {
                    List<Map<String, Double>> list = pointMap.get(trajectoryBo.getId());
                    trajectoryBo.setRoute(list);
                    int sec = (int) Math.ceil((double) list.size() / 10);
                    trajectoryBo.setDuration(DateUtils.secondsToDuration(sec));
                }
            }
        }
        result.setFinished(true);
        return result;
    }

    @Override
    public List<PartConfigSelect> getConfigDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("未查询到对应的测试用例");
        }
        return this.getConfigSelect(caseId,
                JSONObject.parseObject(tjCase.getDetailInfo(), SceneTrajectoryBo.class),
                true);
    }

    public synchronized String buildCaseNumber() {
        return StringUtils.format(ContentTemplate.CASE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.CASE_NUMBER_TEMPLATE));
    }
}
