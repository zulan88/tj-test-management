package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.trajectory.TaskRedisTrajectoryConsumer;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author guowenhao
 * @description 针对表【tj_task_case(测试任务-用例详情表)】的数据库操作Service实现
 * @createDate 2023-08-31 17:39:16
 */
@Service
public class TjTaskCaseServiceImpl extends ServiceImpl<TjTaskCaseMapper, TjTaskCase>
        implements TjTaskCaseService {

    @Autowired
    private RestService restService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjTaskMapper taskMapper;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @Autowired
    private TaskRedisTrajectoryConsumer taskRedisTrajectoryConsumer;

    @Override
    public TaskCaseVerificationPageVo getStatus(Integer taskCaseId) throws BusinessException {
        TaskCaseInfoBo caseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCaseId);
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
        List<TaskCaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.toList());
        for (TaskCaseConfigBo caseConfigBo : configs) {
            // todo 设备信息查询逻辑待实现
            Map<String, Object> map = restService.searchDeviceInfo(caseConfigBo.getIp(), HttpMethod.POST);
            caseConfigBo.setStatus((int) map.get("status"));
            String start = partStartMap.get(caseConfigBo.getParticipatorId());
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
        Map<String, List<TaskCaseConfigBo>> statusMap = configs.stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getType));
        TaskCaseVerificationPageVo result = new TaskCaseVerificationPageVo();
        result.setCaseId(caseInfoBo.getId());
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(statusMap);
        result.setChannels(configs.stream().map(TaskCaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(Integer taskCaseId) throws BusinessException {
        TaskCaseInfoBo caseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCaseId);
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("查询用例失败");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(caseInfoBo.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
        Map<String, List<TaskCaseConfigBo>> partMap = caseInfoBo.getCaseConfigs().stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getSupportRoles));
        int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
        int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
        int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        caseTrajectoryDetailBo.setSceneDesc(caseInfoBo.getSceneName());

        QueryWrapper<TjTaskCaseRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseInfoBo.getId());
        taskCaseRecordMapper.delete(queryWrapper);

        TjTaskCaseRecord tjTaskCaseRecord = new TjTaskCaseRecord();
        tjTaskCaseRecord.setTaskId(caseInfoBo.getTaskId());
        tjTaskCaseRecord.setCaseId(caseInfoBo.getId());
        tjTaskCaseRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjTaskCaseRecord.setStatus(TestingStatus.NOT_START);
        taskCaseRecordMapper.insert(tjTaskCaseRecord);

        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(tjTaskCaseRecord, caseRealTestVo);
        caseRealTestVo.setChannels(caseInfoBo.getCaseConfigs().stream().map(TaskCaseConfigBo::getDataChannel)
                .collect(Collectors.toList()));
        caseRealTestVo.setSceneName(caseInfoBo.getSceneName());
        caseRealTestVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, caseInfoBo.getTestType()));
        return caseRealTestVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException, IOException {
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(taskCaseRecord) || taskCaseRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
        TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
        TaskCaseInfoBo caseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCase.getId());
        this.validConfig(caseInfoBo);
        List<TaskCaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
//        List<DeviceConnRule> deviceConnRules = generateDeviceConnRules(configs);
//        restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(), String.valueOf(caseId), action,
//                deviceConnRules));
        taskCaseRecord.setStatus(TestingStatus.RUNNING);
        taskCaseRecord.setStartTime(LocalDateTime.now());
        taskCaseRecordMapper.updateById(taskCaseRecord);
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(taskCaseRecord, caseRealTestVo);
        caseRealTestVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);

        // 开始监听所有数据通道
        taskRedisTrajectoryConsumer.subscribeAndSend(taskCaseRecord, caseInfoBo.getCaseConfigs());
        // 开启模拟客户端
        restService.taskClientUrl(configs);
        Date date = new Date();
        taskCase.setStartTime(date);
        taskCase.setStatus("测试中");
        taskCaseMapper.updateById(taskCase);

        TjTask tjTask = taskMapper.selectById(taskCaseRecord.getTaskId());
        if (ObjectUtils.isEmpty(tjTask.getStartTime())) {
            tjTask.setStartTime(date);
            taskMapper.updateById(tjTask);
        }
        return caseRealTestVo;
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(taskCaseRecord)) {
            throw new BusinessException("未查询到实车验证记录");
        }
        if (TestingStatus.FINISHED > taskCaseRecord.getStatus() || StringUtils.isEmpty(taskCaseRecord.getRouteFile())) {
            throw new BusinessException("实车验证未完成");
        }
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
        TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
        TaskCaseInfoBo caseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCase.getId());
        List<TaskCaseConfigBo> configs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(configs)) {
            throw new BusinessException("请先进行角色配置");
        }
        // 点位
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 设备配置
        List<TaskCaseConfigBo> configBos = caseInfoBo.getCaseConfigs();
        // av类型设备配置
        List<TaskCaseConfigBo> avConfigs = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles())).collect(Collectors.toList());
        // 主车配置
        TaskCaseConfigBo caseConfigBo = avConfigs.get(0);
        // av类型通道和业务车辆ID映射
        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
                TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorId));
        // av类型通道和业务车辆名称映射
        Map<String, String> avChannelAndNameMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .collect(Collectors.toMap(TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorName));
        // 主车点位映射
        Map<String, List<TrajectoryDetailBo>> avBusinessIdPointsMap = originalTrajectory.getParticipantTrajectories()
                .stream().filter(item ->
                        avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        // 主车全部点位
        List<TrajectoryDetailBo> avPoints = avBusinessIdPointsMap.get(caseConfigBo.getParticipatorId());
        // 读取仿真验证主车轨迹
        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(caseInfoBo.getRouteFile(),
                caseConfigBo.getParticipatorName());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        String key = StringUtils.format(ContentTemplate.REAL_KEY_TEMPLATE, caseInfoBo.getId(), SecurityUtils.getUsername());
        switch (action) {
            case PlaybackAction.START:
                List<RealTestTrajectoryDto> realTestTrajectories =
                        routeService.readRealTrajectoryFromRouteFile(taskCaseRecord.getRouteFile());
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
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(taskCaseRecord) || ObjectUtils.isEmpty(taskCaseRecord.getDetailInfo())) {
            throw new BusinessException("未进行实车验证");
        }
        if (TestingStatus.FINISHED > taskCaseRecord.getStatus()) {
            return null;
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
                .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
        caseTrajectoryDetailBo.setParticipantTrajectories(trajectoryBos);
        RealTestResultVo realTestResultVo = new RealTestResultVo();
        BeanUtils.copyProperties(caseTrajectoryDetailBo, realTestResultVo);
        realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
        realTestResultVo.setId(taskCaseRecord.getId());
        realTestResultVo.setStartTime(taskCaseRecord.getStartTime());
        realTestResultVo.setEndTime(taskCaseRecord.getEndTime());

        TjCase tjCase = caseMapper.selectById(taskCaseRecord.getCaseId());
        realTestResultVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, tjCase.getTestType()));
        return realTestResultVo;
    }

    @Override
    public CommunicationDelayVo communicationDelayVo(Integer recordId) {
        List<Map<String, Object>> infos = taskCaseRecordMapper.recordPartInfo(
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
            String role = String.valueOf(info.get("TYPE"));
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

    private void validStatus(TaskCaseVerificationPageVo pageVo) {
        Map<String, List<TaskCaseConfigBo>> statusMap = pageVo.getStatusMap();
        List<TaskCaseConfigBo> configs = new ArrayList<>();
        statusMap.values().stream().flatMap(List::stream).forEach(configs::add);
        StringBuilder messageBuilder = new StringBuilder();
        for (TaskCaseConfigBo config : configs) {
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
    private void validConfig(TaskCaseInfoBo caseInfoBo) throws BusinessException {
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("查询异常");
        }
        List<TaskCaseConfigBo> casePartConfigs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(casePartConfigs)) {
            throw new BusinessException("未进行角色配置");
        }
        List<TaskCaseConfigBo> configs = casePartConfigs.stream().filter(config ->
                !StringUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());
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




