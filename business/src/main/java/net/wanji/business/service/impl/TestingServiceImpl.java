package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
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
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TestingService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.ImitateRedisTrajectoryConsumer;
import net.wanji.business.util.TrajectoryUtils;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
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
    private ISysDictDataService dictDataService;

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private TjFragmentedSceneDetailMapper sceneDetailMapper;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Autowired
    private ImitateRedisTrajectoryConsumer imitateRedisTrajectoryConsumer;

    @Override
    public RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        if (caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例未进行设备配置");
        }
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 获取参与者起始点
        Map<String, String> partStartMap = TrajectoryUtils.getStartPoint(trajectoryDetail.getParticipantTrajectories());
        // 重复设备过滤
        List<CaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() ->
                                new TreeSet<>(Comparator.comparing(CaseConfigBo::getDeviceId))), ArrayList::new));
        for (CaseConfigBo caseConfigBo : caseConfigs) {
            // 查询设备状态
            Integer status = deviceDetailService.selectDeviceState(caseConfigBo.getDeviceId(), caseConfigBo.getCommandChannel());
            caseConfigBo.setStatus(status);
            if (ObjectUtils.isEmpty(status) || status == 0) {
                // 不在线无需确认准备状态
                continue;
            }
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam();
            stateParam.setCaseId(caseId);
            stateParam.setDeviceId(caseConfigBo.getDeviceId());
            stateParam.setType(1);
            stateParam.setTimestamp(System.currentTimeMillis());
            if (PartRole.AV.equals(caseConfigBo.getSupportRoles())) {
                // av车需要主车全部轨迹
                List<String> participantTrajectories = null;
                try {
                    List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(caseInfoBo.getRouteFile(),
                            caseConfigBo.getBusinessId());
                    participantTrajectories = mainSimulations.stream().map(JSONObject::toJSONString).collect(Collectors.toList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stateParam.setParams(new ParamsDto(caseId, participantTrajectories));
            }
            Integer readyState = restService.selectDeviceReadyState(stateParam);
            caseConfigBo.setPositionStatus(readyState);
        }
        // todo  设备去重
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setCaseId(caseId);
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(caseConfigs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole)));
        result.setChannels(caseConfigs.stream().map(CaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        if (caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例未进行设备配置");
        }
        TjFragmentedSceneDetail sceneDetail = sceneDetailMapper.selectById(caseInfoBo.getSceneDetailId());
        if (ObjectUtils.isEmpty(sceneDetail) || StringUtils.isEmpty(sceneDetail.getTrajectoryInfo())) {
            throw new BusinessException("请先配置轨迹信息");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
        Map<String, List<CaseConfigBo>> partMap = caseInfoBo.getCaseConfigs().stream().collect(
                Collectors.groupingBy(CaseConfigBo::getSupportRoles));
        int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
        int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
        int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        caseTrajectoryDetailBo.setSceneDesc(caseInfoBo.getSceneName());

        QueryWrapper<TjCaseRealRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        caseRealRecordMapper.delete(queryWrapper);

        TjCaseRealRecord tjCaseRealRecord = new TjCaseRealRecord();
        tjCaseRealRecord.setCaseId(caseId);
        tjCaseRealRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjCaseRealRecord.setStatus(TestingStatus.NOT_START);
        caseRealRecordMapper.insert(tjCaseRealRecord);

        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(tjCaseRealRecord, caseRealTestVo);
        caseRealTestVo.setChannels(caseInfoBo.getCaseConfigs().stream().map(CaseConfigBo::getDataChannel)
                .collect(Collectors.toList()));
        caseRealTestVo.setSceneName(caseInfoBo.getSceneName());
        caseRealTestVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, caseInfoBo.getTestType()));
        return caseRealTestVo;
    }

    @Override
    public CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException, IOException {
        // todo recordId可以换成caseId
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(caseRealRecord) || caseRealRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        Integer caseId = caseRealRecord.getCaseId();
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        if (caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例未进行设备配置");
        }
        if (!restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(), String.valueOf(caseId), action,
                generateDeviceConnRules(caseInfoBo)))) {
            throw new BusinessException("主控响应异常");
        }
        TjCaseRealRecord realRecord = caseInfoBo.getCaseRealRecord();
        realRecord.setStatus(TestingStatus.RUNNING);
        realRecord.setStartTime(LocalDateTime.now());
        caseRealRecordMapper.updateById(realRecord);
        // 开始监听所有数据通道
        imitateRedisTrajectoryConsumer.subscribeAndSend(caseInfoBo);
        // 开启模拟客户端
//        List<CaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
//                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
//        restService.imitateClientUrl(caseConfigs);
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(realRecord, caseRealTestVo);
        caseRealTestVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(realRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);
        return caseRealTestVo;
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(caseRealRecord)) {
            throw new BusinessException("未查询到实车验证记录");
        }
        if (TestingStatus.FINISHED > caseRealRecord.getStatus() || StringUtils.isEmpty(caseRealRecord.getRouteFile())) {
            throw new BusinessException("实车验证未完成");
        }
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseRealRecord.getCaseId());
        if (caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例未进行设备配置");
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
            throw new BusinessException("未进行实车验证");
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

        TjCase tjCase = caseMapper.selectById(caseRealRecord.getCaseId());
        realTestResultVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, tjCase.getTestType()));
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

    private void validStatus(RealVehicleVerificationPageVo pageVo) {
        Map<String, List<CaseConfigBo>> statusMap = pageVo.getStatusMap();
        List<CaseConfigBo> configs = new ArrayList<>();
        statusMap.values().stream().flatMap(List::stream).forEach(configs::add);
        StringBuilder messageBuilder = new StringBuilder();
        for (CaseConfigBo config : configs) {
            if (YN.Y_INT != config.getStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_OFFLINE_TEMPLATE,
                        config.getDeviceName()));
            }
            if (YN.Y_INT != config.getPositionStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_POS_ERROR_TEMPLATE,
                        config.getDeviceName()));
            }
        }
        String message = messageBuilder.toString();
        if (StringUtils.isNotEmpty(message)) {
            pageVo.setMessage(message);
        } else {
            pageVo.setCanStart(true);
        }
    }

    /**
     * 校验用例信息
     *
     * @param caseInfoBo
     * @throws BusinessException
     */
    private void validConfig(CaseInfoBo caseInfoBo) throws BusinessException {
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("用例查询异常");
        }
        List<CaseConfigBo> casePartConfigs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(casePartConfigs)) {
            throw new BusinessException("用例未进行角色配置");
        }
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(configs)) {
            throw new BusinessException("用例未进行设备配置");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(CaseInfoBo caseInfoBo) {
        List<CaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());
        Map<String, String> businessIdAndRoleMap = caseConfigs.stream().collect(Collectors.toMap(
                CaseConfigBo::getBusinessId,
                CaseConfigBo::getParticipantRole));

        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(caseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);

        Map<String, Object> tessParams = new HashMap<>();
        Map<String, Object> param1 = new HashMap<>();
        param1.put("caseId", caseInfoBo.getId());
        List<Map<String, Object>> participantTrajectories = new ArrayList<>();
        for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", participantTrajectory.getId());
            map.put("model", participantTrajectory.getModel());
            map.put("name", participantTrajectory.getName());
            map.put("role", businessIdAndRoleMap.get(participantTrajectory.getId()));
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
            participantTrajectories.add(map);
        }
        param1.put("participantTrajectories", participantTrajectories);
        tessParams.put("param1", JSONObject.toJSONString(param1));

        List<Integer> ids = new ArrayList<>();
        List<CaseConfigBo> configs = new ArrayList<>();
        for (CaseConfigBo caseConfig : caseConfigs) {
            if (ids.contains(caseConfig.getDeviceId())) {
                continue;
            } else {
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
                    sourceParams = tessParams;
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
