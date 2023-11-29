package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.ModelEnum;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.*;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjResourcesDetailMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseRealRecordService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.socket.WebSocketManage;
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
import java.util.TreeMap;
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
    private ISysDictDataService dictDataService;

    @Autowired
    private TjFragmentedSceneDetailService sceneDetailService;

    @Autowired
    private TjCasePartConfigService casePartConfigService;

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjCaseRealRecordService caseRealRecordService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjResourcesDetailMapper resourcesDetailMapper;

    @Autowired
    private SceneLabelMap sceneLabelMap;

    @Override
    public Map<String, List<SimpleSelect>> init() {
        List<SysDictData> caseStatus = dictTypeService.selectDictDataByType(SysType.CASE_STATUS);
        List<SysDictData> testType = dictTypeService.selectDictDataByType(SysType.TEST_TYPE);
        Map<String, List<SimpleSelect>> result = new HashMap<>(2);
        result.put(SysType.CASE_STATUS, CollectionUtils.emptyIfNull(caseStatus).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        result.put(SysType.TEST_TYPE, CollectionUtils.emptyIfNull(testType).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, List<PartConfigSelect>> initEdit(Integer caseId) {
        Map<String, List<PartConfigSelect>> result = new HashMap<>(1);
        CaseDetailVo caseDetailVo = this.selectCaseDetail(caseId);

//        CaseTrajectoryDetailBo trajectoryDetailBo = JSONObject.parseObject(caseDetailVo.getDetailInfo(), CaseTrajectoryDetailBo.class);
//        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(trajectoryDetailBo);

        handleCasePartConfig(caseDetailVo);
        for (PartConfigSelect partConfigSelect : CollectionUtils.emptyIfNull(caseDetailVo.getPartConfigSelects())) {
            for (CasePartConfigVo part : CollectionUtils.emptyIfNull(partConfigSelect.getParts())) {
                part.setSelected(false);
            }
        }
        result.put(SysType.PART_ROLE, caseDetailVo.getPartConfigSelects());
        return result;
    }

    @Override
    public List<TjCasePartRoleVo> initEditNew(Integer caseId) throws BusinessException {
        List<TjCasePartRoleVo> roleVos = new ArrayList<>();
        List<CasePartConfigVo> casePartConfigVos = casePartConfigService.getConfigInfoByCaseId(caseId);
        TjCasePartRoleVo main = new TjCasePartRoleVo();
        main.setType("main");
        TjCasePartRoleVo slave = new TjCasePartRoleVo();
        slave.setType("slave");
        TjCasePartRoleVo pedestrian = new TjCasePartRoleVo();
        pedestrian.setType("pedestrian");
        if (CollectionUtils.isNotEmpty(casePartConfigVos)) {
            for (CasePartConfigVo casePartConfigVo : casePartConfigVos) {
                switch (casePartConfigVo.getBusinessType()) {
                    case "main":
                        main.getCasePartConfigs().add(casePartConfigVo);
                        break;
                    case "slave":
                        slave.getCasePartConfigs().add(casePartConfigVo);
                        break;
                    case "pedestrian":
                        pedestrian.getCasePartConfigs().add(casePartConfigVo);
                        break;
                }
            }
        }
        roleVos.add(main);
        roleVos.add(slave);
        roleVos.add(pedestrian);
        return roleVos;
    }

    @Override
    public List<CasePageVo> pageList(CaseQueryDto caseQueryDto) {
        caseQueryDto.setUserName(SecurityUtils.getUsername());
        List<CaseDetailVo> caseVos = caseMapper.selectCases(caseQueryDto);
        handleLabels(caseVos);
        List<TjDeviceDetail> deviceDetails = deviceDetailService.list();
        Map<Integer, TjDeviceDetail> deviceMap = CollectionUtils.emptyIfNull(deviceDetails).stream()
                .collect(Collectors.toMap(TjDeviceDetail::getDeviceId, value -> value));
        return CollectionUtils.emptyIfNull(caseVos).stream().map(t -> {
            CasePageVo casePageVo = new CasePageVo();
            BeanUtils.copyBeanProp(casePageVo, t);
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
            if (CollectionUtils.isNotEmpty(casePageVo.getPartConfigs())) {
                TreeMap<String, List<String>> roleConfigDetail = new TreeMap<>();
                StringBuilder roleConfigSort = new StringBuilder();
                TreeMap<String, List<TjCasePartConfig>> roleGroupMap = casePageVo.getPartConfigs().stream()
                        .collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole, TreeMap::new,
                                Collectors.toList()));
                for (Entry<String, List<TjCasePartConfig>> roleItem : roleGroupMap.entrySet()) {
                    String roleName = dictDataService.selectDictLabel(SysType.PART_ROLE, roleItem.getKey());
                    // 角色配置详情
                    roleConfigDetail.put(roleName, roleItem.getValue().stream().map(TjCasePartConfig::getName).collect(Collectors.toList()));
                    // 角色配置简述
                    if (roleItem.getValue().stream().anyMatch(item -> !ObjectUtils.isEmpty(item.getDeviceId()))) {
                        Map<Integer, List<TjCasePartConfig>> deviceGroup = roleItem.getValue().stream().collect(Collectors.groupingBy(TjCasePartConfig::getDeviceId));
                        for (Entry<Integer, List<TjCasePartConfig>> deviceEntry : deviceGroup.entrySet()) {
                            if (!ObjectUtils.isEmpty(deviceEntry.getKey()) && deviceMap.containsKey(deviceEntry.getKey())) {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_DEVICE_TEMPLATE, deviceMap.get(deviceEntry.getKey()).getDeviceName(), roleName, deviceEntry.getValue().size()));
                            } else {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, deviceEntry.getValue().size()));
                            }
                        }
                    } else {
                        roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, roleItem.getValue().size()));
                    }
                }
                casePageVo.setRoleConfigDetail(roleConfigDetail);
                casePageVo.setRoleConfigSort(roleConfigSort.toString());
            }
            return casePageVo;
        }).collect(Collectors.toList());
    }

    @Override
    public CaseDetailVo selectCaseDetail(Integer caseId) {
        CaseQueryDto caseQueryDto = new CaseQueryDto();
        caseQueryDto.setId(caseId);
        caseQueryDto.setUserName(SecurityUtils.getUsername());
        List<CaseDetailVo> caseVos = caseMapper.selectCases(caseQueryDto);
        if (CollectionUtils.isNotEmpty(caseVos)) {
            handleLabels(caseVos);
            CaseDetailVo caseDetailVo = caseVos.get(0);
            handleCasePartConfig(caseDetailVo);
            CollectionUtils.emptyIfNull(caseDetailVo.getPartConfigSelects()).removeIf(partConfigSelect ->
                    CollectionUtils.isEmpty(partConfigSelect.getParts()));
            return caseDetailVo;
        }
        return null;
    }

    private void handleLabels(List<CaseDetailVo> caseDetails) {
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, String> sceneMap = new HashMap<>();
        for (Label tlabel : labelList) {
            Long parentId = tlabel.getParentId();
            String prelabel = null;
            if (parentId != null) {
                prelabel = sceneMap.getOrDefault(parentId, null);
            } else {
                continue;
            }
            if (tlabel.getId().equals(2L)) {
                continue;
            }
            if (prelabel == null) {
                sceneMap.put(tlabel.getId(), tlabel.getName());
            } else {
                sceneMap.put(tlabel.getId(), prelabel + "-" + tlabel.getName());
            }
        }
        for (CaseDetailVo caseDetailVo : CollectionUtils.emptyIfNull(caseDetails)) {
            handleLabel(caseDetailVo, sceneMap);
        }
    }

    private void handleLabel(CaseDetailVo caseDetailVo, Map<Long, String> sceneMap) {
        List<String> data = new ArrayList<>();
        if (StringUtils.isNotEmpty(caseDetailVo.getLabel())) {
            List<String> labels = Arrays.stream(caseDetailVo.getLabel().split(",")).collect(Collectors.toList());
            for (String str : labels) {
                try {
                    long intValue = Long.parseLong(str);
                    data.add(sceneMap.get(intValue));
                } catch (NumberFormatException e) {
                    // 处理无效的整数字符串
                }
            }
        }
        caseDetailVo.setLabelDetail(data);
    }

    private void handleCasePartConfig(CaseDetailVo caseDetailVo) {
        List<PartConfigSelect> configSelect = this.getConfigSelect(caseDetailVo.getId(),
                JSONObject.parseObject(caseDetailVo.getDetailInfo(), CaseTrajectoryDetailBo.class), false);
        caseDetailVo.setPartConfigSelects(configSelect);
    }


    @Override
    public List<PartConfigSelect> getConfigSelect(Integer caseId,
                                                  CaseTrajectoryDetailBo caseTrajectoryDetailBo,
                                                  boolean deviceConfig) {
        // 1.查询用例已存在的参与者角色配置
        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        Map<String, List<TjCasePartConfig>> caseRoleConfigMap = casePartConfigService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole));
        // 2.查询设备信息
        Map<String, List<TjDeviceDetail>> devicesMap = deviceDetailService.list()
                .stream().collect(Collectors.groupingBy(TjDeviceDetail::getSupportRoles));
        // 3.将场景参与者转换为配置模板 caseTrajectoryDetailBo.participantTrajectories -> CasePartConfigVo
        Map<String, List<CasePartConfigVo>> configMap = casePartConfigService.trajectory2Config(caseTrajectoryDetailBo);
        // 4.根据参与者角色字典进行匹配
        return CollectionUtils.emptyIfNull(dictTypeService.selectDictDataByType(SysType.PART_ROLE)).stream()
                .map(role -> {
                    // 对应参与者类型的所有需要配置的参与者信息
                    List<CasePartConfigVo> casePartConfigVos = configMap.get(role.getCssClass());
                    List<CasePartConfigVo> parts = new ArrayList<>();
                    for (CasePartConfigVo config : CollectionUtils.emptyIfNull(casePartConfigVos)) {
                        // 如果是用例设备配置，那么该参与者类型下若没有对应的配置则跳过
                        List<TjCasePartConfig> businessConfigs = caseRoleConfigMap.get(role.getDictValue());
                        List<String> businessIds = CollectionUtils.emptyIfNull(businessConfigs).stream()
                                .map(TjCasePartConfig::getBusinessId).collect(Collectors.toList());
                        CasePartConfigVo part = new CasePartConfigVo();
                        if (!businessIds.contains(config.getBusinessId())) {
                            // 未配置对应角色
                            if (deviceConfig) {
                                // 无法配置设备
                                continue;
                            } else {
                                // 初始化角色配置
                                BeanUtils.copyBeanProp(part, config);
                            }
                        } else {
                            // 使用已有的角色配置
                            Map<String, TjCasePartConfig> businessConfigMap = CollectionUtils.emptyIfNull(businessConfigs)
                                    .stream().collect(Collectors.toMap(TjCasePartConfig::getBusinessId, value -> value));
                            BeanUtils.copyBeanProp(part, businessConfigMap.get(config.getBusinessId()));
                            part.setSelected(Boolean.TRUE);
                            if (deviceConfig) {
                                // 对应参与者类型可选择的设备列表
                                List<TjDeviceDetail> devices = devicesMap.get(role.getDictValue());
                                List<DeviceDetailVo> deviceVos = CollectionUtils.emptyIfNull(devices).stream()
                                        .filter(t -> StringUtils.isEmpty(t.getAttribute2())
                                                || SecurityUtils.getUsername().equals(t.getAttribute2()))
                                        .map(device -> {
                                            DeviceDetailVo detailVo = new DeviceDetailVo();
                                            BeanUtils.copyBeanProp(detailVo, device);
                                            if (detailVo.getDeviceId().equals(part.getDeviceId())) {
                                                detailVo.setSelected(Boolean.TRUE);
                                            }
                                            return detailVo;
                                        }).collect(Collectors.toList());
                                part.setDevices(deviceVos);
                            }
                        }
                        part.setModelName(ModelEnum.getDescByCode(part.getModel()));
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveCase(TjCaseDto tjCaseDto) throws BusinessException {
        TjCase tjCase = new TjCase();
        if (ObjectUtils.isEmpty(tjCaseDto.getId())) {
            BeanUtils.copyBeanProp(tjCase, tjCaseDto);
            tjCase.setCaseNumber(this.buildCaseNumber());
            TjFragmentedSceneDetail sceneDetail = sceneDetailService.getById(tjCaseDto.getSceneDetailId());
            if (ObjectUtils.isEmpty(sceneDetail)) {
                throw new BusinessException("创建失败：场景不存在");
            }
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), SceneTrajectoryBo.class);
            if (StringUtils.isEmpty(sceneDetail.getTrajectoryInfo())) {
                throw new BusinessException("创建失败：未获取到场景点位配置");
            }
            tjCase.setDetailInfo(sceneDetail.getTrajectoryInfo());

            if (StringUtils.isEmpty(sceneDetail.getRouteFile())) {
                throw new BusinessException("创建失败：场景未进行仿真验证");
            }
            tjCase.setRouteFile(sceneDetail.getRouteFile());
            tjCase.setCreatedBy(SecurityUtils.getUsername());
            tjCase.setCreatedDate(LocalDateTime.now());
            StringBuilder labelshows = new StringBuilder();
            for (String str : sceneDetail.getLabel().split(",")) {
                try {
                    long intValue = Long.parseLong(str);
                    String labelshow = sceneLabelMap.getSceneLabel(intValue);
                    if (labelshow != null) {
                        if (labelshows.length() > 0) {
//                            labelshows.append(",").append(labelshow);
                        } else {
                            labelshows.append(labelshow);
                        }
                    }
                } catch (NumberFormatException e) {
                    // 处理无效的整数字符串
                }
            }
            tjCase.setTestScene(labelshows.toString());
            List<ParticipantTrajectoryBo> participantTrajectories = sceneTrajectoryBo.getParticipantTrajectories();
            List<TjCasePartConfig> result = CollectionUtils.emptyIfNull(participantTrajectories).stream().map(part -> {
                TjCasePartConfig config = new CasePartConfigVo();
                config.setBusinessId(part.getId());
                config.setBusinessType(part.getType());
                config.setName(part.getName());
                config.setModel(part.getModel());
                config.setFristSite(part.getTrajectory().get(0).getLongitude()+","+part.getTrajectory().get(0).getLatitude());
                return config;
            }).collect(Collectors.toList());
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
            this.saveOrUpdate(tjCase);
            casePartConfigService.saveAndupdate(tjCase.getId(), result);
            return true;
        } else {
            tjCase = this.getById(tjCaseDto.getId());
            tjCase.setTestTarget(tjCaseDto.getTestTarget());
            tjCase.setRemark(tjCaseDto.getRemark());
            List<TjCasePartConfig> configs = new ArrayList<>();
            for (PartConfigSelect partConfigSelect : tjCaseDto.getPartConfigSelects()) {
                for (int i = 0; i < CollectionUtils.emptyIfNull(partConfigSelect.getParts()).size(); i++) {
                    CasePartConfigVo part = partConfigSelect.getParts().get(i);
//                    if (!PartType.MAIN.equals(part.getBusinessType()) && !part.isSelected()) {
//                        continue;
//                    }
                    TjCasePartConfig config = new TjCasePartConfig();
                    BeanUtils.copyBeanProp(config, part);
//                    config.setCaseId(tjCase.getId());
//                    config.setParticipantRole(part.getParticipantRole());
//                    config.setName(part.getName());
//                    config.setModel(part.getModel());
                    configs.add(config);
                }
            }
            boolean saveConfig = casePartConfigService.saveAndupdate(tjCaseDto.getId(), configs);
            if (!saveConfig) {
                throw new BusinessException("保存配置失败");
            }
            if (tjCase.getStatus().equals(CaseStatusEnum.WAIT_CONFIG.getCode())) {
                tjCase.setStatus(CaseStatusEnum.WAIT_TEST.getCode());
            }
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCase.setUpdatedBy(SecurityUtils.getUsername());
            return this.saveOrUpdate(tjCase);
        }
    }

    @Override
    public CaseInfoBo getCaseDetail(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("用例查询异常");
        }
        List<CaseConfigBo> casePartConfigs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(casePartConfigs)) {
            throw new BusinessException("用例未进行角色配置");
        }
        return caseInfoBo;
    }


    @Override
    public void playback(Integer id, String participantId, int action) throws BusinessException, IOException {
        CaseInfoBo caseInfoBo = this.getCaseDetail(id);
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(id), WebSocketManage.SIMULATION, null);
        switch (action) {
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean batchDelete(List<Integer> caseIds) throws BusinessException {
        if (CollectionUtils.isEmpty(caseIds)) {
            return false;
        }
        QueryWrapper<TjCasePartConfig> configQueryWrapper = new QueryWrapper<>();
        configQueryWrapper.in(ColumnName.CASE_ID_COLUMN, caseIds);
        List<TjCasePartConfig> tjCasePartConfigs = casePartConfigService.list(configQueryWrapper);
        if (CollectionUtils.isNotEmpty(tjCasePartConfigs)) {
            List<Integer> configIds = tjCasePartConfigs.stream().map(TjCasePartConfig::getId).collect(Collectors.toList());
            boolean removeConfig = casePartConfigService.removeByIds(configIds);
            if (!removeConfig) {
                throw new BusinessException("删除配置失败");
            }
        }
        boolean removeCase = this.removeByIds(caseIds);
        if (!removeCase) {
            throw new BusinessException("删除用例失败");
        }
        return true;
    }

    @Override
    public boolean updateStatus(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("用例不存在！");
        }
        if (CaseStatusEnum.WAIT_CONFIG.getCode().equals(tjCase.getStatus())) {
            throw new BusinessException("用例未进行角色配置，无法进行状态操作！");
        }
        if (CaseStatusEnum.WAIT_TEST.getCode().equals(tjCase.getStatus())
                || CaseStatusEnum.INVALID.getCode().equals(tjCase.getStatus())) {
            tjCase.setStatus(CaseStatusEnum.EFFECTIVE.getCode());
            return this.updateById(tjCase);
        }
        if (CaseStatusEnum.EFFECTIVE.getCode().equals(tjCase.getStatus())) {
            tjCase.setStatus(CaseStatusEnum.INVALID.getCode());
            return this.updateById(tjCase);
        }
        return false;
    }

    @Override
    public boolean batchUpdateStatus(List<Integer> caseIds, Integer action) throws BusinessException {
        List<TjCase> cases = this.listByIds(caseIds);
        if (CollectionUtils.isEmpty(cases)) {
            throw new BusinessException("用例信息异常！");
        }
        if (action == 1) {
            for (TjCase tjCase : cases) {
                if (CaseStatusEnum.WAIT_TEST.getCode().equals(tjCase.getStatus())
                        || CaseStatusEnum.INVALID.getCode().equals(tjCase.getStatus())) {
                    tjCase.setStatus(CaseStatusEnum.EFFECTIVE.getCode());
                }
            }
            return this.updateBatchById(cases);
        }
        if (action == 2) {
            for (TjCase tjCase : cases) {
                if (CaseStatusEnum.WAIT_TEST.getCode().equals(tjCase.getStatus())
                        || CaseStatusEnum.EFFECTIVE.getCode().equals(tjCase.getStatus())) {
                    tjCase.setStatus(CaseStatusEnum.INVALID.getCode());
                }
            }
            return this.updateBatchById(cases);
        }
        return false;
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
        return this.getConfigSelect(caseId, JSONObject.parseObject(tjCase.getDetailInfo(), CaseTrajectoryDetailBo.class), true);
    }

    @Override
    public List<TjCasePartRoleVo> getConfigDetailNew(Integer caseId) throws BusinessException {
        TjCase tjCase = this.getById(caseId);
        if (ObjectUtils.isEmpty(tjCase)) {
            throw new BusinessException("未查询到对应的测试用例");
        }
        List<DeviceDetailVo> deviceDetails = deviceDetailService.list().stream().map(device -> {
            DeviceDetailVo detailVo = new DeviceDetailVo();
            BeanUtils.copyBeanProp(detailVo, device);
            return detailVo;
        }).collect(Collectors.toList());
        Map<String, List<DeviceDetailVo>> devicesMap = deviceDetails.stream().collect(Collectors.groupingBy(DeviceDetailVo::getSupportRoles));
        Map<String, List<CasePartConfigVo>> casePartConfigVoMap = casePartConfigService.getConfigInfoByCaseId(caseId).stream().collect(Collectors.groupingBy(CasePartConfigVo::getParticipantRole));
        List<TjCasePartRoleVo> roleVos = new ArrayList<>();
        casePartConfigVoMap.remove("mvSimulation");
        casePartConfigVoMap.remove("sp");
        for (Entry<String, List<CasePartConfigVo>> entry:casePartConfigVoMap.entrySet()){
            TjCasePartRoleVo tjCasePartRoleVo = new TjCasePartRoleVo();
            tjCasePartRoleVo.setType(entry.getKey());
            for (CasePartConfigVo casePartConfigVo:entry.getValue()){
                casePartConfigVo.setDevices(devicesMap.get(entry.getKey()));
                tjCasePartRoleVo.getCasePartConfigs().add(casePartConfigVo);
            }
            roleVos.add(tjCasePartRoleVo);
        }
        return roleVos;
    }

    @Override
    public boolean deleteRecord(Integer recordId) throws BusinessException {
        return caseRealRecordService.removeById(recordId);
    }


    public synchronized String buildCaseNumber() {
        return StringUtils.format(ContentTemplate.CASE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.CASE_NUMBER_TEMPLATE));
    }

    private List<SysDictData> getPartRoleList(List<String> filters) {
        return CollectionUtils.emptyIfNull(dictTypeService.selectDictDataByType(SysType.PART_ROLE)).stream()
                .filter(t -> !filters.contains(t.getDictValue())).collect(Collectors.toList());
    }
}
