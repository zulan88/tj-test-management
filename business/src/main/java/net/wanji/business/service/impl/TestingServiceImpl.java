package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.*;
import net.wanji.business.domain.bo.*;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TestingService;
import net.wanji.business.trajectory.ImitateRedisTrajectoryConsumer;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.xmlbeans.impl.regex.Match;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    private ISysDictDataService dictDataService;

    @Autowired
    private TjFragmentedScenesMapper scenesMapper;

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
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        Map<String, String> partStartMap =
                CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories()).stream().collect(
                        Collectors.toMap(
                                ParticipantTrajectoryBo::getId,
                                item -> CollectionUtils.emptyIfNull(item.getTrajectory()).stream()
                                        .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                                        .orElse(new TrajectoryDetailBo()).getPosition()));
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.toList());
        for (CaseConfigBo caseConfigBo : configs) {
            // todo 设备信息查询逻辑待实现
            Map<String, Object> map = restService.searchDeviceInfo(caseConfigBo.getIp(), HttpMethod.POST);
            caseConfigBo.setStatus((int) map.get("status"));
            String start = partStartMap.get(caseConfigBo.getBusinessId());
            if (StringUtils.isEmpty(start)) {
                continue;
            }
            String[] position = start.split(",");
            double longitude = (double) map.get("longitude");
            double latitude = (double) map.get("latitude");
            double courseAngle = (double) map.get("courseAngle");
            caseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
            caseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
            caseConfigBo.setLongitude(longitude);
            caseConfigBo.setLatitude(latitude);
            caseConfigBo.setCourseAngle(courseAngle);
            double v = GeoUtil.calculateDistance(latitude, longitude, Double.parseDouble(position[1]),
                    Double.parseDouble(position[0]));
            if (v > 5) {
                // todo 具体距离待沟通

            }
            caseConfigBo.setPositionStatus(YN.Y_INT);
        }
        Map<String, List<CaseConfigBo>> statusMap = configs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole));
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setCaseId(caseId);
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(statusMap);
        result.setChannels(configs.stream().map(CaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
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
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(caseRealRecord) || caseRealRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        Integer caseId = caseRealRecord.getCaseId();
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
//        List<DeviceConnRule> deviceConnRules = generateDeviceConnRules(configs);
//        restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(), String.valueOf(caseId), action,
//                deviceConnRules));
        caseRealRecord.setStatus(TestingStatus.RUNNING);
        caseRealRecord.setStartTime(LocalDateTime.now());
        caseRealRecordMapper.updateById(caseRealRecord);
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(caseRealRecord, caseRealTestVo);
        caseRealTestVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);

        // 开始监听所有数据通道
        imitateRedisTrajectoryConsumer.subscribeAndSend(caseRealRecord, caseInfoBo.getCaseConfigs());
        // 开启模拟客户端
        restService.imitateClientUrl(configs);
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
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseRealRecord.getCaseId());
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(configs)) {
            throw new BusinessException("请先进行角色配置");
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
                caseConfigBo.getName());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        String key = StringUtils.format(ContentTemplate.REAL_KEY_TEMPLATE, caseInfoBo.getId(), SecurityUtils.getUsername());
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
          if(null == startTime){
            startTime = Date.from(((LocalDateTime) info.get("START_TIME"))
                .atZone(ZoneId.systemDefault()).toInstant());
          }
          if(null == endTime){
            endTime = Date.from(((LocalDateTime) info.get("END_TIME"))
                .atZone(ZoneId.systemDefault()).toInstant());
          }
            String role = String.valueOf(info.get("PARTICIPANT_ROLE"));
            type.add(role);
        }
        if(startTime == null | endTime == null){
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
            typeDelay.add((int)(Math.random() * 100));
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

    private List<DeviceConnRule> generateDeviceConnRules(List<CaseConfigBo> caseConfigs) {
        List<DeviceConnRule> rules = new ArrayList<>();
        for (int i = 0; i < caseConfigs.size(); i++) {
            CaseConfigBo sourceDevice = caseConfigs.get(i);
            for (int j = 0; j < caseConfigs.size(); j++) {
                if (j == i) {
                    continue;
                }
                CaseConfigBo targetDevice = caseConfigs.get(j);
                DeviceConnRule rule = new DeviceConnRule();
                rule.setSource(createConnInfo(sourceDevice));
                rule.setTarget(createConnInfo(targetDevice));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(CaseConfigBo config) {
        DeviceConnInfo deviceConnInfo = new DeviceConnInfo();
        deviceConnInfo.setChannel(config.getDataChannel());
        deviceConnInfo.setControlChannel(config.getCommandChannel());
        deviceConnInfo.setId(String.valueOf(config.getDeviceId()));
        return deviceConnInfo;
    }

    private static List<String> delayTimes(Date startTime, Date endTime){
      ArrayList<String> time = new ArrayList<>();
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
          "HH:mm:ss");
      /*LocalTime localTime = startTime.toInstant().atZone(ZoneId.systemDefault())
          .toLocalTime();
      localTime.plusSeconds(1);
      localTime.format(dateTimeFormatter);*/

      long seconds = Duration.between(startTime.toInstant(),
          endTime.toInstant()).getSeconds();
      for(int i = 1; i < seconds + 1;i++){
        long hours = TimeUnit.SECONDS.toHours(i) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(i) % 60;
        long second = i % 60;
        time.add(String.format("%02d:%02d:%02d", hours, minutes, second));
      }

      return time;
    }
}
