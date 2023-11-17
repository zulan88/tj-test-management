package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseTestPrepareVo;
import net.wanji.business.domain.vo.CaseTestStartVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TestingService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.ImitateRedisTrajectoryConsumer;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 10:00
 * @Descriptoin:
 */
@Service
public class TestingServiceImpl implements TestingService {

    @Autowired
    private RestService restService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Autowired
    private ImitateRedisTrajectoryConsumer imitateRedisTrajectoryConsumer;

    @Override
    public RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException {
        // 1.查询用例详情
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        // 2.数据校验
        validConfig(caseInfoBo);
        // 3.轨迹详情
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 4.参与者开始点位
        Map<String, String> partStartMap =
                CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories()).stream().collect(
                        Collectors.toMap(
                                ParticipantTrajectoryBo::getId,
                                item -> CollectionUtils.emptyIfNull(item.getTrajectory()).stream()
                                        .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                                        .orElse(new TrajectoryDetailBo()).getPosition()));
        // 5.重复设备过滤
        List<CaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(CaseConfigBo::getDeviceId))), ArrayList::new));
        // 6.参与者ID和参与者名称匹配map
        Map<String, String> businessIdAndRoleMap = caseInfoBo.getCaseConfigs().stream().collect(Collectors.toMap(
                CaseConfigBo::getBusinessId,
                CaseConfigBo::getParticipantRole));
        // 7.主车轨迹
        // av车需要主车全部轨迹
        List<SimulationTrajectoryDto> participantTrajectories = null;
        try {
            participantTrajectories = routeService.readOriRouteFile(caseInfoBo.getRouteFile());
            participantTrajectories = participantTrajectories.stream().filter(
                    item -> !ObjectUtils.isEmpty(item.getValue())).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 7.状态查询
        for (CaseConfigBo caseConfigBo : caseConfigs) {
            String start = partStartMap.get(caseConfigBo.getBusinessId());
            if (StringUtils.isNotEmpty(start)) {
                String[] position = start.split(",");
                caseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
                caseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
//                double longitude = (double) map.get("longitude");
//                double latitude = (double) map.get("latitude");
//                double courseAngle = (double) map.get("courseAngle");
//                caseConfigBo.setLongitude(longitude);
//                caseConfigBo.setLatitude(latitude);
//                caseConfigBo.setCourseAngle(courseAngle);
            }
            // 查询设备状态
            Integer status = deviceDetailService.selectDeviceState(caseConfigBo.getDeviceId(), caseConfigBo.getCommandChannel(), false);
            caseConfigBo.setStatus(status);
            if (ObjectUtils.isEmpty(status) || status == 0) {
                // 不在线无需确认准备状态
                continue;
            }
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam();
            stateParam.setDeviceId(caseConfigBo.getDeviceId());
            stateParam.setControlChannel(caseConfigBo.getCommandChannel());
            stateParam.setType(1);
            stateParam.setTimestamp(System.currentTimeMillis());
            if (PartRole.AV.equals(caseConfigBo.getSupportRoles())) {
                stateParam.setParams(new ParamsDto(routeService.readOriTrajectoryFromData(participantTrajectories, caseConfigBo.getBusinessId())));
            }
            if (PartRole.MV_SIMULATION.equals(caseConfigBo.getSupportRoles())) {
                SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(caseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);
                Map<String, Object> tessParams = new HashMap<>();
                // gdj edit start 2023-11-17

                List<Map<String, Object>> param1 = new ArrayList<>();
                Map<String, Object> mapParam1 = new HashMap<>();
                mapParam1.put("caseId", caseInfoBo.getId());
                mapParam1.put("avPassTime", CollectionUtils.emptyIfNull(participantTrajectories).size());
                 Label label = new Label();
                 label.setParentId(2L);
                 List<Label> sceneTypeLabelList = labelsService.selectLabelsList(label);
                 List<String> sceneTypes = new ArrayList<>();
                 if (StringUtils.isNotEmpty(caseInfoBo.getAllStageLabel())) {
                     String[] labels = caseInfoBo.getAllStageLabel().split(",");
                     for (String labelId : labels) {
                         for (Label sceneTypeLabel : sceneTypeLabelList) {
                             if (sceneTypeLabel.getId() == Long.parseLong(labelId)) {
                                 sceneTypes.add(sceneTypeLabel.getName());
                             }
                         }
                     }
                 }
                mapParam1.put("type", String.join(",", sceneTypes));
                List<Map<String, Object>> simulationTrajectories = new ArrayList<>();
                for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
                    if (PartRole.AV.equals(businessIdAndRoleMap.get(participantTrajectory.getId()))) {
                        continue;
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", businessIdAndRoleMap.get(participantTrajectory.getId()));
                    map.put("name", participantTrajectory.getName());
                    map.put("model", participantTrajectory.getModel());
                    map.put("id", participantTrajectory.getId());
                    map.put("trajectory", participantTrajectory.getTrajectory().stream().map(item -> {
                        Map<String, Object> t = new HashMap<>();
                        t.put("type", item.getType());
                        t.put("time", item.getTime());
                        t.put("lane", item.getLane());
                        t.put("speed", item.getSpeed());
                        String[] pos = item.getPosition().split(",");
                        t.put("position", Arrays.asList(pos[0], pos[1]));
                        return t;
                    }).collect(Collectors.toList()));
                    simulationTrajectories.add(map);
                }
                mapParam1.put("participantTrajectories", simulationTrajectories);
                param1.add(mapParam1);
                tessParams.put("param1", param1);
                stateParam.setParams(tessParams);
            }
            caseConfigBo.setPositionStatus(deviceDetailService.selectDeviceReadyState(caseConfigBo.getDeviceId(), stateParam, false));
        }
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setCaseId(caseId);
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(caseConfigs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole)));
        result.setChannels(caseConfigs.stream().map(CaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        result.setMessage(validStatus(caseConfigs));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseTestPrepareVo prepare(Integer caseId) throws BusinessException {
        // 1.用例详情
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        // 2.校验数据
        validConfig(caseInfoBo);
        // 3.轨迹详情
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(caseInfoBo.getDetailInfo(), CaseTrajectoryDetailBo.class);
        // 4.角色配置信息
        Map<String, List<CaseConfigBo>> partMap = caseInfoBo.getCaseConfigs().stream().collect(
                Collectors.groupingBy(CaseConfigBo::getSupportRoles));
        // 5.各角色数量
        int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
        int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
        int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        // 6.删除后新增实车测试记录
        QueryWrapper<TjCaseRealRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        caseRealRecordMapper.delete(queryWrapper);

        TjCaseRealRecord tjCaseRealRecord = new TjCaseRealRecord();
        tjCaseRealRecord.setCaseId(caseId);
        tjCaseRealRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjCaseRealRecord.setStatus(TestingStatus.NOT_START);
        caseRealRecordMapper.insert(tjCaseRealRecord);
        // 7.前端结果集
        CaseTestPrepareVo caseTestPrepareVo = new CaseTestPrepareVo();
        BeanUtils.copyProperties(tjCaseRealRecord, caseTestPrepareVo);
        caseTestPrepareVo.setChannels(caseInfoBo.getCaseConfigs().stream().map(CaseConfigBo::getDataChannel).distinct()
                .collect(Collectors.toList()));
        return caseTestPrepareVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaseTestStartVo start(Integer caseId, Integer action) throws BusinessException, IOException {
        // 1.实车测试记录详情

        // 2.用例详情
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        // 3.校验数据
        validConfig(caseInfoBo);
        // 4.更新业务数据
        TjCaseRealRecord realRecord = caseInfoBo.getCaseRealRecord();
        if (ObjectUtils.isEmpty(realRecord) || realRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        realRecord.setStatus(TestingStatus.RUNNING);
        realRecord.setStartTime(LocalDateTime.now());
        caseRealRecordMapper.updateById(realRecord);
        // 5.开始监听所有数据通道
        imitateRedisTrajectoryConsumer.subscribeAndSend(caseInfoBo);
        // 6.向主控发送开始请求
        if (!restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(),
            String.valueOf(caseId), action,
            generateDeviceConnRules(caseInfoBo), null, true))) {
            throw new BusinessException("主控响应异常");
        }
        // 7.前端结果集
        CaseTestStartVo startVo = new CaseTestStartVo();
        BeanUtils.copyProperties(realRecord, startVo);
        startVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(realRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        startVo.setMainTrajectories(mainTrajectoryMap);
        return startVo;
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        // 1.实车测试记录
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        // 2.数据校验
        if (ObjectUtils.isEmpty(caseRealRecord)) {
            throw new BusinessException("未查询到试验记录");
        }
        if (TestingStatus.FINISHED > caseRealRecord.getStatus() || StringUtils.isEmpty(caseRealRecord.getRouteFile())) {
            throw new BusinessException("无完整试验记录");
        }
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseRealRecord.getCaseId());
        if (ObjectUtils.isEmpty(caseInfoBo) || CollectionUtils.isEmpty(caseInfoBo.getCaseConfigs())
                || caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("未进行设备配置");
        }
        // 点位
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 设备配置
        List<CaseConfigBo> configBos = caseInfoBo.getCaseConfigs();
        // av类型设备配置
        List<CaseConfigBo> avConfigs = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles())).collect(Collectors.toList());
        // 主车配置
        CaseConfigBo caseConfigBo = avConfigs.get(0);
        // av类型通道和业务车辆ID映射
        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
                CaseConfigBo::getDataChannel, CaseConfigBo::getBusinessId));
        // av类型通道和业务车辆名称映射
        Map<String, String> avChannelAndNameMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .collect(Collectors.toMap(CaseConfigBo::getDataChannel, CaseConfigBo::getName));
        // 主车点位映射
        Map<String, List<TrajectoryDetailBo>> avBusinessIdPointsMap = originalTrajectory.getParticipantTrajectories()
                .stream().filter(item ->
                        avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        // 主车全部点位
        List<TrajectoryDetailBo> avPoints = avBusinessIdPointsMap.get(caseConfigBo.getBusinessId());
        // 读取仿真验证主车轨迹
        TjCase tjCase = caseMapper.selectById(caseRealRecord.getCaseId());
        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(tjCase.getRouteFile(),
                caseConfigBo.getBusinessId());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(caseInfoBo.getCaseRealRecord().getId()), WebSocketManage.REAL, null);
        switch (action) {
            case PlaybackAction.START:
                List<RealTestTrajectoryDto> realTestTrajectories = routeService.readRealTrajectoryFromRouteFile(caseRealRecord.getRouteFile());
                for (RealTestTrajectoryDto realTestTrajectoryDto : realTestTrajectories) {
                    if (avChannelAndBusinessIdMap.containsKey(realTestTrajectoryDto.getChannel())) {
                        realTestTrajectoryDto.setName(avChannelAndNameMap.get(realTestTrajectoryDto.getChannel()));
                        realTestTrajectoryDto.setMain(true);
                        realTestTrajectoryDto.setMainSimuTrajectories(mainSimuTrajectories);
                        realTestTrajectoryDto.setPoints(JSONObject.toJSONString(avPoints));
                    }
                }
                RealPlaybackSchedule.startSendingData(key, realTestTrajectories);
                break;
            case PlaybackAction.SUSPEND:
                RealPlaybackSchedule.suspend(key);
                break;
            case PlaybackAction.CONTINUE:
                RealPlaybackSchedule.goOn(key);
                break;
            case PlaybackAction.STOP:
                RealPlaybackSchedule.stopSendingData(key);
                break;
            default:
                break;
        }
    }

    @Override
    public RealTestResultVo getResult(Integer recordId) throws BusinessException {
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(caseRealRecord) || ObjectUtils.isEmpty(caseRealRecord.getDetailInfo())) {
            throw new BusinessException("待试验");
        }
        if (TestingStatus.FINISHED > caseRealRecord.getStatus()) {
            return null;
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
                .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
        caseTrajectoryDetailBo.setParticipantTrajectories(trajectoryBos);
        RealTestResultVo realTestResultVo = new RealTestResultVo();
        BeanUtils.copyProperties(caseTrajectoryDetailBo, realTestResultVo);
        realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
        realTestResultVo.setId(caseRealRecord.getId());
        realTestResultVo.setStartTime(caseRealRecord.getStartTime());
        realTestResultVo.setEndTime(caseRealRecord.getEndTime());
        return realTestResultVo;
    }

    @Override
    public CommunicationDelayVo communicationDelayVo(Integer recordId) {
        List<Map<String, Object>> infos = caseRealRecordMapper.recordPartInfo(
                recordId);
        CommunicationDelayVo communicationDelayVo = new CommunicationDelayVo();
        List<String> type = new ArrayList<>();
        Date startTime = null;
        Date endTime = null;
        for (Map<String, Object> info : infos) {
            if (null == startTime) {
                startTime = Date.from(((LocalDateTime) info.get("START_TIME"))
                        .atZone(ZoneId.systemDefault()).toInstant());
            }
            if (null == endTime) {
                endTime = Date.from(((LocalDateTime) info.get("END_TIME"))
                        .atZone(ZoneId.systemDefault()).toInstant());
            }
            String role = String.valueOf(info.get("PARTICIPANT_ROLE"));
            type.add(role);
        }
        if (startTime == null | endTime == null) {
            return null;
        }
        communicationDelayVo.setType(type);
        List<String> times = delayTimes(startTime, endTime);
        communicationDelayVo.setTime(times);

        ArrayList<List<Integer>> delay = new ArrayList<>();
        for (String t : communicationDelayVo.getType()) {
            List<Integer> typeDelay = new ArrayList<>();
            delay.add(typeDelay);
            for (String time : times) {
                typeDelay.add((int) (Math.random() * 100));
            }
        }

        communicationDelayVo.setDelay(delay);

        return communicationDelayVo;
    }

    private String validStatus(List<CaseConfigBo> configs) {
        StringBuilder messageBuilder = new StringBuilder();
        for (CaseConfigBo config : configs) {
            if (ObjectUtils.isEmpty(config.getStatus()) || YN.Y_INT != config.getStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_OFFLINE_TEMPLATE,
                        config.getDeviceName()));
            }
            if (ObjectUtils.isEmpty(config.getPositionStatus()) || YN.Y_INT != config.getPositionStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_POS_ERROR_TEMPLATE,
                        config.getDeviceName()));
            }
        }
        return messageBuilder.toString();
    }

    /**
     * 校验用例信息
     *
     * @param caseInfoBo
     * @throws BusinessException
     */
    private void validConfig(CaseInfoBo caseInfoBo) throws BusinessException {
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("用例异常：查询用例失败");
        }
        if (CollectionUtils.isEmpty(caseInfoBo.getCaseConfigs())) {
            throw new BusinessException("用例异常：用例未进行角色配置");
        }
        if (caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例异常：用例未进行设备配置");
        }
        if (StringUtils.isEmpty(caseInfoBo.getDetailInfo())) {
            throw new BusinessException("用例异常：无路径配置信息");
        }
        if (StringUtils.isEmpty(caseInfoBo.getRouteFile())) {
            throw new BusinessException("场景异常：场景未验证");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(CaseInfoBo caseInfoBo) {
        List<CaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());

        List<Integer> ids = new ArrayList<>();
        List<CaseConfigBo> configs = new ArrayList<>();
        for (CaseConfigBo caseConfig : caseConfigs) {
            if (!ids.contains(caseConfig.getDeviceId())) {
                ids.add(caseConfig.getDeviceId());
                configs.add(caseConfig);
            }
        }

        List<DeviceConnRule> rules = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            CaseConfigBo sourceDevice = configs.get(i);
            for (int j = 0; j < configs.size(); j++) {
                if (j == i) {
                    continue;
                }
                Map<String, Object> sourceParams = new HashMap<>();
                Map<String, Object> targetParams = new HashMap<>();

                CaseConfigBo targetDevice = configs.get(j);

                DeviceConnRule rule = new DeviceConnRule();
                if (PartRole.MV_SIMULATION.equals(sourceDevice.getParticipantRole())
                        && PartRole.AV.equals(targetDevice.getParticipantRole())) {
//                    sourceParams = tessParams;
                }
                rule.setSource(createConnInfo(sourceDevice, sourceParams));
                rule.setTarget(createConnInfo(targetDevice, targetParams));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(CaseConfigBo config, Map<String, Object> params) {
        Map<String, Object> param = new HashMap<>();
        DeviceConnInfo deviceConnInfo = new DeviceConnInfo();
        deviceConnInfo.setChannel(config.getDataChannel());
        deviceConnInfo.setControlChannel(config.getCommandChannel());
        deviceConnInfo.setId(String.valueOf(config.getDeviceId()));
        deviceConnInfo.setParams(params);
        return deviceConnInfo;
    }

    private static List<String> delayTimes(Date startTime, Date endTime) {
        ArrayList<String> time = new ArrayList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
                "HH:mm:ss");
      /*LocalTime localTime = startTime.toInstant().atZone(ZoneId.systemDefault())
          .toLocalTime();
      localTime.plusSeconds(1);
      localTime.format(dateTimeFormatter);*/

        long seconds = Duration.between(startTime.toInstant(),
                endTime.toInstant()).getSeconds();
        for (int i = 1; i < seconds + 1; i++) {
            long hours = TimeUnit.SECONDS.toHours(i) % 24;
            long minutes = TimeUnit.SECONDS.toMinutes(i) % 60;
            long second = i % 60;
            time.add(String.format("%02d:%02d:%02d", hours, minutes, second));
        }

        return time;
    }
}
