package net.wanji.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.ModelEnum;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.domain.vo.CasePartConfigVo;
import net.wanji.business.domain.vo.CaseVerificationVo;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
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
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.RedisTrajectoryConsumer;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.core.domain.SimpleSelect;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictDataService;
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
import java.util.Map.Entry;
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
    private ISysDictDataService dictDataService;

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

    @Autowired
    private SceneLabelMap sceneLabelMap;

    @Override
    public Map<String, List<SimpleSelect>> init() {
        List<SysDictData> testStatus = dictTypeService.selectDictDataByType(SysType.CASE_STATUS);
        Map<String, List<SimpleSelect>> result = new HashMap<>(1);
        result.put(SysType.CASE_STATUS, CollectionUtils.emptyIfNull(testStatus).stream()
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
    public List<CasePageVo> pageList(CaseQueryDto caseQueryDto) {
        List<CasePageVo> caseVos = caseMapper.selectCases(caseQueryDto);
        for (CasePageVo casePageVo : caseVos) {
            // 状态名称
            casePageVo.setStatusName(dictDataService.selectDictLabel(SysType.CASE_STATUS, casePageVo.getStatus()));
            // 场景分类
            if (StringUtils.isNotEmpty(casePageVo.getLabel())) {
                StringBuilder labelSort = new StringBuilder();

                for (String str : casePageVo.getLabel().split(",")) {
                    try {
                        long intValue = Long.parseLong(str);
                        String labelName = sceneLabelMap.getSceneLabel(intValue);
                        if (StringUtils.isNotEmpty(labelName)) {
                            if (labelSort.length() > 0) {
                                labelSort.append(",").append(labelName);
                            } else {
                                labelSort.append(labelName);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 处理无效的整数字符串
                    }
                }
                casePageVo.setSceneSort(labelSort.toString());
            }
            // 角色配置
            if (CollectionUtils.isNotEmpty(casePageVo.getPartConfigs())) {
                StringBuilder roleConfigSort = new StringBuilder();
                Map<String, List<TjCasePartConfig>> roleGroup = casePageVo.getPartConfigs().stream().collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole));
                for (Entry<String, List<TjCasePartConfig>> entry : roleGroup.entrySet()) {
                    String roleName = dictDataService.selectDictLabel(SysType.PART_ROLE, entry.getKey());
                    if (entry.getValue().stream().anyMatch(item -> !ObjectUtils.isEmpty(item.getDeviceId()))) {
                        Map<Integer, List<TjCasePartConfig>> deviceGroup = entry.getValue().stream().collect(Collectors.groupingBy(TjCasePartConfig::getDeviceId));
                        for (Entry<Integer, List<TjCasePartConfig>> deviceEntry : deviceGroup.entrySet()) {
                            TjDeviceDetail deviceDetail = deviceDetailService.getById(deviceEntry.getKey());
                            if (ObjectUtils.isEmpty(deviceDetail)) {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, deviceEntry.getValue().size()));
                            } else {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_DEVICE_TEMPLATE, deviceDetail.getDeviceName(), roleName, deviceEntry.getValue().size()));
                            }
                        }
                    } else {
                        roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, entry.getValue().size()));
                    }
                }
                casePageVo.setRoleConfigSort(roleConfigSort.toString());
            }
        }

        return caseVos;
    }

//    @Override
    public List<CaseVo> pageList(TjCaseDto caseDto) throws BusinessException {
        return null;
    }

    @Override
    public List<PartConfigSelect> getConfigSelect(Integer caseId,
                                                  SceneTrajectoryBo sceneTrajectoryBo,
                                                  boolean deviceConfig) {
        // 1.查询用例已存在的参与者角色配置
        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        Map<String, List<TjCasePartConfig>> caseRoleConfigMap = casePartConfigService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole));
        // 2.查询设备信息
        Map<String, List<TjDeviceDetail>> devicesMap = deviceDetailService.list()
                .stream().collect(Collectors.groupingBy(TjDeviceDetail::getSupportRoles));
        // 3.将场景参与者转换为配置模板 sceneTrajectoryBo.participantTrajectories -> CasePartConfigVo
        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(sceneTrajectoryBo);
        // 4.根据参与者角色字典进行匹配
        return CollectionUtils.emptyIfNull(dictTypeService.selectDictDataByType(SysType.PART_ROLE)).stream()
                .map(role -> {
                    // 对应参与者类型的所有需要配置的参与者信息
                    List<CasePartConfigVo> casePartConfigVos = configMap.get(role.getCssClass());
                    // 对应参与者类型可选择的设备列表
                    List<TjDeviceDetail> devices = devicesMap.get(role.getDictValue());
                    List<CasePartConfigVo> parts = new ArrayList<>();
                    for (CasePartConfigVo config : CollectionUtils.emptyIfNull(casePartConfigVos)) {
                        // 如果是用例设备配置，那么该参与者类型下若没有对应的配置则跳过
                        List<TjCasePartConfig> businessConfigs = caseRoleConfigMap.get(role.getDictValue());
                        List<String> businessIds = CollectionUtils.emptyIfNull(businessConfigs).stream()
                                .map(TjCasePartConfig::getBusinessId).collect(Collectors.toList());
                        if (deviceConfig && !businessIds.contains(config.getBusinessId())) {
                            continue;
                        }
                        Map<String, TjCasePartConfig> businessConfigMap = CollectionUtils.emptyIfNull(businessConfigs)
                                .stream().collect(Collectors.toMap(TjCasePartConfig::getBusinessId, value -> value));
                        CasePartConfigVo part = new CasePartConfigVo();
                        if (businessIds.contains(config.getBusinessId())) {
                            BeanUtils.copyBeanProp(part, businessConfigMap.get(config.getBusinessId()));
                            part.setSelected(YN.Y_INT);
                        } else {
                            BeanUtils.copyBeanProp(part, config);
                        }
                        part.setModelName(ModelEnum.getDescByCode(part.getModel()));
                        List<DeviceDetailVo> deviceVos = CollectionUtils.emptyIfNull(devices).stream().map(device -> {
                            DeviceDetailVo detailVo = new DeviceDetailVo();
                            BeanUtils.copyBeanProp(detailVo, device);
                            if (detailVo.getDeviceId().equals(part.getDeviceId())) {
                                detailVo.setSelected(YN.Y_INT);
                            }
                            return detailVo;
                        }).collect(Collectors.toList());
                        part.setDevices(deviceVos);
                        parts.add(part);
                    }
                    PartConfigSelect partConfigSelect = new PartConfigSelect();
                    partConfigSelect.setDictCode(role.getDictCode());
                    partConfigSelect.setDictLabel(role.getDictLabel());
                    partConfigSelect.setDictValue(role.getDictValue());
                    partConfigSelect.setSort(role.getDictSort());
                    partConfigSelect.setCssClass(role.getCssClass());
                    if (CollectionUtils.isNotEmpty(parts)) {
                        partConfigSelect.setParts(parts);
                    }
                    return partConfigSelect;
                }).collect(Collectors.toList());
    }

    @Override
    public List<PartConfigSelect> getConfigSelect(Integer caseId,
                                                  SceneTrajectoryBo sceneTrajectoryBo) {
        // 1.查询用例已存在的参与者角色配置
        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        Map<String, List<TjCasePartConfig>> caseRoleConfigMap = casePartConfigService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole));
        // 2.查询设备信息
        Map<String, List<TjDeviceDetail>> devicesMap = deviceDetailService.list()
                .stream().collect(Collectors.groupingBy(TjDeviceDetail::getSupportRoles));
        // 3.将场景参与者转换为配置模板 sceneTrajectoryBo.participantTrajectories -> CasePartConfigVo
        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(sceneTrajectoryBo);
        // 4.根据参与者角色字典进行匹配
        return CollectionUtils.emptyIfNull(dictTypeService.selectDictDataByType(SysType.PART_ROLE)).stream()
                .map(role -> {
                    // 对应参与者类型的所有需要配置的参与者信息
                    List<CasePartConfigVo> casePartConfigVos = configMap.get(role.getCssClass());
                    // 对应参与者类型可选择的设备列表
                    List<TjDeviceDetail> devices = devicesMap.get(role.getDictValue());
                    List<CasePartConfigVo> parts = new ArrayList<>();
                    for (CasePartConfigVo config : CollectionUtils.emptyIfNull(casePartConfigVos)) {
                        // 如果是用例设备配置，那么该参与者类型下若没有对应的配置则跳过
                        List<TjCasePartConfig> businessConfigs = caseRoleConfigMap.get(role.getDictValue());
                        List<String> businessIds = CollectionUtils.emptyIfNull(businessConfigs).stream()
                                .map(TjCasePartConfig::getBusinessId).collect(Collectors.toList());
                        if (!businessIds.contains(config.getBusinessId())) {
                            continue;
                        }
                        Map<String, TjCasePartConfig> businessConfigMap = CollectionUtils.emptyIfNull(businessConfigs)
                                .stream().collect(Collectors.toMap(TjCasePartConfig::getBusinessId, value -> value));
                        CasePartConfigVo part = new CasePartConfigVo();
                        BeanUtils.copyBeanProp(part, config);
                        part.setModelName(ModelEnum.getDescByCode(part.getModel()));
                        List<DeviceDetailVo> deviceVos = CollectionUtils.emptyIfNull(devices).stream().map(device -> {
                            DeviceDetailVo detailVo = new DeviceDetailVo();
                            BeanUtils.copyBeanProp(detailVo, device);
                            return detailVo;
                        }).collect(Collectors.toList());
                        part.setDevices(deviceVos);
                        parts.add(part);
                    }
                    PartConfigSelect partConfigSelect = new PartConfigSelect();
                    partConfigSelect.setDictCode(role.getDictCode());
                    partConfigSelect.setDictLabel(role.getDictLabel());
                    partConfigSelect.setDictValue(role.getDictValue());
                    partConfigSelect.setSort(role.getDictSort());
                    partConfigSelect.setCssClass(role.getCssClass());
                    if (CollectionUtils.isNotEmpty(parts)) {
                        partConfigSelect.setParts(parts);
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
    public List<CaseVo> getCases(CaseQueryDto caseQueryDto) {
//        List<CaseVo> result = caseMapper.selectCases(caseQueryDto);
//        for (CaseVo caseVo : CollectionUtils.emptyIfNull(result)) {
//            caseVo.setCaseNumber(caseVo.getCaseNumber() + "_" + caseVo.getId());
//        }
//        return result;
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer saveCase(TjCaseDto tjCaseDto) throws BusinessException {
        TjFragmentedSceneDetail sceneDetail = sceneDetailService.getById(tjCaseDto.getSceneDetailId());
        if (ObjectUtils.isEmpty(sceneDetail) || StringUtils.isEmpty(sceneDetail.getTrajectoryInfo())) {
            throw new BusinessException("请先配置轨迹信息");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
        int avNum = tjCaseDto.getPartConfig().get(PartRole.AV).size();
        int simulationNum = tjCaseDto.getPartConfig().get(PartRole.MV_SIMULATION).size();
        int pedestrianNum = tjCaseDto.getPartConfig().get(PartRole.SP).size();
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        TjFragmentedScenes scenes = scenesService.getById(sceneDetail.getFragmentedSceneId());
        caseTrajectoryDetailBo.setSceneDesc(scenes.getName());
        Integer caseId = tjCaseDto.getId();
        if (ObjectUtils.isEmpty(caseId)) {
            TjCase tjCase = new TjCase();
            BeanUtils.copyBeanProp(tjCase, tjCaseDto);
            tjCase.setResourcesDetailId(sceneDetail.getResourcesDetailId());
            tjCase.setCaseNumber(this.buildCaseNumber());
            tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            tjCase.setStatus(CaseStatusEnum.TO_BE_SIMULATED.getCode());
            tjCase.setCreatedBy(SecurityUtils.getUsername());
            tjCase.setCreatedDate(LocalDateTime.now());
            this.save(tjCase);
            caseId = tjCase.getId();
        } else {
            TjCase tjCase = caseMapper.selectById(caseId);
            tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
            this.updateById(tjCase);
        }
        Map<String, ParticipantTrajectoryBo> partTrajectoryMap =
                CollectionUtils.emptyIfNull(caseTrajectoryDetailBo.getParticipantTrajectories()).stream()
                        .collect(Collectors.toMap(ParticipantTrajectoryBo::getId, item -> item));
        List<TjCasePartConfig> configs = new ArrayList<>();
        for (Entry<String, List<TjCasePartConfig>> entry : tjCaseDto.getPartConfig().entrySet()) {
            for (TjCasePartConfig config : CollectionUtils.emptyIfNull(entry.getValue())) {
                if (!partTrajectoryMap.containsKey(config.getBusinessId())) {
                    continue;
                }
                config.setCaseId(caseId);
                config.setParticipantRole(entry.getKey());
                config.setName(partTrajectoryMap.get(config.getBusinessId()).getName());
                config.setModel(partTrajectoryMap.get(config.getBusinessId()).getModel());
                configs.add(config);
            }
        }
        boolean saveConfig = casePartConfigService.removeThenSave(caseId, configs);
        if (!saveConfig) {
            throw new BusinessException("保存角色配置失败");
        }
        return sceneDetail.getFragmentedSceneId();
    }

    @Override
    public CaseInfoBo getCaseDetail(Integer caseId) {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
//        if (ObjectUtils.isEmpty(caseInfoBo)) {
//            throw new BusinessException("用例查询异常");
//        }
//        List<CaseConfigBo> casePartConfigs = caseInfoBo.getCaseConfigs();
//        if (CollectionUtils.isEmpty(casePartConfigs)) {
//            throw new BusinessException("用例未进行角色配置");
//        }
        return caseInfoBo;
    }


    @Override
    public void playback(Integer id, String participantId, int action) throws BusinessException, IOException {
        CaseInfoBo caseInfoBo = this.getCaseDetail(id);
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(id), WebSocketManage.SIMULATION, null);
        switch (action) {
            case PlaybackAction.CALL:
                boolean start = restService.start(buildStartParam(caseInfoBo));
                if (!start) {
                    throw new BusinessException("仿真程序连接失败");
                }
                redisTrajectoryConsumer.subscribeAndSend(caseInfoBo, participantId);
                break;
            case PlaybackAction.START:
                if (StringUtils.isEmpty(caseInfoBo.getRouteFile())) {
                    this.playback(id, participantId, PlaybackAction.CALL);
                    break;
                }
                List<List<TrajectoryValueDto>> routeList = routeService.readTrajectoryFromRouteFile(
                        caseInfoBo.getRouteFile(), participantId);
                if (CollectionUtils.isEmpty(routeList)) {
                    throw new BusinessException("未查询到轨迹");
                }
                PlaybackSchedule.startSendingData(key, routeList);
                break;
            case PlaybackAction.SUSPEND:
                PlaybackSchedule.suspend(key);
                break;
            case PlaybackAction.CONTINUE:
                PlaybackSchedule.goOn(key);
                break;
            case PlaybackAction.STOP:
                PlaybackSchedule.stopSendingData(key);
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
        QueryWrapper<TjCasePartConfig> configQueryWrapper = new QueryWrapper<>();
        configQueryWrapper.in(ColumnName.CASE_ID_COLUMN, tjCases.stream().map(TjCase::getId).collect(Collectors.toList()));
        List<TjCasePartConfig> configList = casePartConfigService.list(configQueryWrapper);
        Map<Integer, List<TjCasePartConfig>> configMap = CollectionUtils.emptyIfNull(configList).stream()
                .collect(Collectors.groupingBy(TjCasePartConfig::getCaseId));
        if (StringUtils.equals(tjCaseDto.getStatus(), CaseStatusEnum.TO_BE_SIMULATED.getCode())) {
            for (TjCase caseItem : tjCases) {
                if (!configMap.containsKey(caseItem.getId())) {
                    throw new BusinessException(StringUtils.format("{}未进行角色配置", caseItem.getCaseNumber()));
                }
            }
        }
        // todo 仿真点位验证逻辑
        if (StringUtils.equals(tjCaseDto.getStatus(), CaseStatusEnum.SIMULATION_VERIFICATION.getCode())) {
//            for (TjCase caseItem : tjCases) {
//                CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(caseItem.getDetailInfo(),
//                        CaseTrajectoryDetailBo.class);
//                if (caseTrajectoryDetailBo.getParticipantTrajectories().stream().noneMatch(trajectories ->
//                        routeService.verifyRoute(trajectories.getTrajectory()))) {
//                    throw new BusinessException(StringUtils.format("用例{}仿真校验失败", caseItem.getCaseNumber()));
//                }
//            }
        }
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
            List<List<TrajectoryValueDto>> trajectoryValue = routeService.readRouteFile(tjCase.getRouteFile());
            pointMap = routeService.extractRoute(trajectoryValue);
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

    @Override
    public List<PartConfigSelect> getTaskConfigDetail(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("未查询到对应的测试用例");
        }
        return this.getConfigSelect(caseId,
                JSONObject.parseObject(tjCase.getDetailInfo(), SceneTrajectoryBo.class));
    }

    public synchronized String buildCaseNumber() {
        return StringUtils.format(ContentTemplate.CASE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.CASE_NUMBER_TEMPLATE));
    }

    public TestStartParam buildStartParam(CaseInfoBo caseInfo) {
        // 计算caseInfo.getCaseConfigs()中各个角色的数量
        long avNum = (int) caseInfo.getCaseConfigs().stream().filter(config ->
                PartRole.AV.equals(config.getParticipantRole())).count();
        long simulationNum = caseInfo.getCaseConfigs().stream().filter(config ->
                PartRole.MV_SIMULATION.equals(config.getParticipantRole())).count();
        long pedestrianNum = caseInfo.getCaseConfigs().stream().filter(config ->
                PartRole.SP.equals(config.getParticipantRole())).count();
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(caseInfo.getDetailInfo(), SceneTrajectoryBo.class);
        for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
            for (TrajectoryDetailBo trajectoryDetailBo : participantTrajectory.getTrajectory()) {
                String[] pos = trajectoryDetailBo.getPosition().split(",");
                if (!ObjectUtils.isEmpty(pos)) {
                    trajectoryDetailBo.setLongitude(pos[0]);
                    trajectoryDetailBo.setLatitude(pos[1]);
                }
            }
        }
        return new TestStartParam(caseInfo.getId(),
                WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(caseInfo.getId()),
                        WebSocketManage.SIMULATION, null), (int) avNum, (int) simulationNum,
                (int) pedestrianNum, sceneTrajectoryBo.getParticipantTrajectories());
    }
}
