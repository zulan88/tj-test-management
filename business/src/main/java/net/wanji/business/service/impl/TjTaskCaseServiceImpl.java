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
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.domain.vo.TaskReportVo;
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
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.TaskRedisTrajectoryConsumer;
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
 * @author guowenhao
 * @description 针对表【tj_task_case(测试任务-用例详情表)】的数据库操作Service实现
 * @createDate 2023-08-31 17:39:16
 */
@Service
public class TjTaskCaseServiceImpl extends ServiceImpl<TjTaskCaseMapper, TjTaskCase>
        implements TjTaskCaseService {

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private RestService restService;

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
        // 1.查询用例详情
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCaseId);
        // 2.数据校验
        validConfig(taskCaseInfoBo);
        // 3.轨迹详情
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(),
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
        List<TaskCaseConfigBo> taskCaseConfigs = taskCaseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(TaskCaseConfigBo::getDeviceId))), ArrayList::new));
        // 6.参与者ID和参与者名称匹配map
        Map<String, String> businessIdAndRoleMap = taskCaseInfoBo.getCaseConfigs().stream().collect(Collectors.toMap(
                TaskCaseConfigBo::getParticipatorId,
                TaskCaseConfigBo::getParticipatorName));
        // 7.状态查询
        for (TaskCaseConfigBo taskCaseConfigBo : taskCaseConfigs) {
            String start = partStartMap.get(taskCaseConfigBo.getParticipatorId());
            if (StringUtils.isNotEmpty(start)) {
                String[] position = start.split(",");
                taskCaseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
                taskCaseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
//                double longitude = (double) map.get("longitude");
//                double latitude = (double) map.get("latitude");
//                double courseAngle = (double) map.get("courseAngle");
//                caseConfigBo.setLongitude(longitude);
//                caseConfigBo.setLatitude(latitude);
//                caseConfigBo.setCourseAngle(courseAngle);
            }
            // 查询设备状态
            Integer status = deviceDetailService.selectDeviceState(taskCaseConfigBo.getDeviceId(), taskCaseConfigBo.getCommandChannel(), false);
            taskCaseConfigBo.setStatus(status);
            if (ObjectUtils.isEmpty(status) || status == 0) {
                // 不在线无需确认准备状态
                continue;
            }
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam();
            stateParam.setCaseId(taskCaseInfoBo.getId());
            stateParam.setDeviceId(taskCaseConfigBo.getDeviceId());
            stateParam.setControlChannel(taskCaseConfigBo.getCommandChannel());
            stateParam.setType(1);
            stateParam.setTimestamp(System.currentTimeMillis());
            if (PartRole.AV.equals(taskCaseConfigBo.getSupportRoles())) {
                // av车需要主车全部轨迹
                List<SimulationTrajectoryDto> participantTrajectories = null;
                try {
                    participantTrajectories = routeService.readOriTrajectoryFromRouteFile(taskCaseInfoBo.getRouteFile(),
                            taskCaseConfigBo.getParticipatorId());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stateParam.setParams(new ParamsDto(taskCaseId, participantTrajectories));
            }
            if (PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getSupportRoles())) {
                SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);
                Map<String, Object> tessParams = new HashMap<>();
                Map<String, Object> param1 = new HashMap<>();
                param1.put("caseId", taskCaseInfoBo.getId());
                List<Map<String, Object>> participantTrajectories = new ArrayList<>();
                for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
                    if (PartRole.AV.equals(businessIdAndRoleMap.get(participantTrajectory.getId()))) {
                        continue;
                    }
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
                tessParams.put("param1", param1);
                stateParam.setParams(tessParams);
            }
            taskCaseConfigBo.setPositionStatus(deviceDetailService.selectDeviceReadyState(taskCaseConfigBo.getDeviceId(), stateParam, false));
        }
        Map<String, List<TaskCaseConfigBo>> statusMap = taskCaseConfigs.stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getType));
        TaskCaseVerificationPageVo result = new TaskCaseVerificationPageVo();
        result.setCaseId(taskCaseInfoBo.getId());
        result.setFilePath(taskCaseInfoBo.getFilePath());
        result.setGeoJsonPath(taskCaseInfoBo.getGeoJsonPath());
        result.setStatusMap(statusMap);
        result.setChannels(taskCaseConfigs.stream().map(TaskCaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(Integer taskCaseId) throws BusinessException {
        // 1.用例详情
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCaseId);
        // 2.校验数据
        validConfig(taskCaseInfoBo);
        // 3.轨迹详情
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(taskCaseInfoBo.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
        // 4.角色配置信息
        Map<String, List<TaskCaseConfigBo>> partMap = taskCaseInfoBo.getCaseConfigs().stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getSupportRoles));
        // 5.各角色数量
        int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
        int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
        int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        caseTrajectoryDetailBo.setSceneDesc(taskCaseInfoBo.getSceneName());
        // 7.删除后新增任务用例测试记录
        QueryWrapper<TjTaskCaseRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseInfoBo.getId());
        taskCaseRecordMapper.delete(queryWrapper);

        TjTaskCaseRecord tjTaskCaseRecord = new TjTaskCaseRecord();
        tjTaskCaseRecord.setTaskId(taskCaseInfoBo.getTaskId());
        tjTaskCaseRecord.setCaseId(taskCaseInfoBo.getId());
        tjTaskCaseRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjTaskCaseRecord.setStatus(TestingStatus.NOT_START);
        taskCaseRecordMapper.insert(tjTaskCaseRecord);
        // 7.前端结果集
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(tjTaskCaseRecord, caseRealTestVo);
        caseRealTestVo.setChannels(taskCaseInfoBo.getCaseConfigs().stream().map(TaskCaseConfigBo::getDataChannel)
                .collect(Collectors.toList()));
        return caseRealTestVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录详情
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(taskCaseRecord) || taskCaseRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        // 2.任务用例详情
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
        TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCase.getId());
        // 3.校验数据
        validConfig(taskCaseInfoBo);
        // 4.更新业务数据
        taskCaseRecord.setStatus(TestingStatus.RUNNING);
        taskCaseRecord.setStartTime(LocalDateTime.now());
        taskCaseRecordMapper.updateById(taskCaseRecord);

        Date date = new Date();
        taskCase.setStartTime(date);
        taskCase.setStatus("测试中");
        taskCaseMapper.updateById(taskCase);

        TjTask tjTask = taskMapper.selectById(taskCaseRecord.getTaskId());
        if (ObjectUtils.isEmpty(tjTask.getStartTime())) {
            tjTask.setStartTime(date);
            taskMapper.updateById(tjTask);
        }
        // 5.开始监听所有数据通道
        taskRedisTrajectoryConsumer.subscribeAndSend(taskCaseRecord, taskCaseInfoBo.getCaseConfigs());
        // 6.向主控发送开始请求
        if (!restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(),
                String.valueOf(taskCaseInfoBo.getId()), action, generateDeviceConnRules(taskCaseInfoBo)))) {
            throw new BusinessException("主控响应异常");
        }
        // 7.前端结果集
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
        return caseRealTestVo;
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        // 2.数据校验
        if (ObjectUtils.isEmpty(taskCaseRecord)) {
            throw new BusinessException("未查询到任务用例测试记录");
        }
        if (TestingStatus.FINISHED > taskCaseRecord.getStatus() || StringUtils.isEmpty(taskCaseRecord.getRouteFile())) {
            throw new BusinessException("无完整试验记录");
        }
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
        TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCase.getId());
        if (ObjectUtils.isEmpty(taskCaseInfoBo) || CollectionUtils.isEmpty(taskCaseInfoBo.getCaseConfigs())
                || taskCaseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("未进行设备配置");
        }
        // 点位
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 设备配置
        List<TaskCaseConfigBo> configBos = taskCaseInfoBo.getCaseConfigs();
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
        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(taskCaseInfoBo.getRouteFile(),
                caseConfigBo.getParticipatorId());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        String key = StringUtils.format(ContentTemplate.REAL_KEY_TEMPLATE, SecurityUtils.getUsername(), taskCaseInfoBo.getId(), WebSocketManage.TASK, recordId);
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
            throw new BusinessException("待开始测试");
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

    @Override
    public List<TaskReportVo> getReport(Integer taskId, Integer taskCaseId) {

//        }
        return new ArrayList<>();
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
     * @param taskCaseInfoBo
     * @throws BusinessException
     */
    private void validConfig(TaskCaseInfoBo taskCaseInfoBo) throws BusinessException {
        if (ObjectUtils.isEmpty(taskCaseInfoBo)) {
            throw new BusinessException("任务异常：查询任务用例失败");
        }
        if (CollectionUtils.isEmpty(taskCaseInfoBo.getCaseConfigs())) {
            throw new BusinessException("任务异常：任务未进行设备配置");
        }
        if (taskCaseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例异常：用例未进行设备配置");
        }
        if (StringUtils.isEmpty(taskCaseInfoBo.getDetailInfo())) {
            throw new BusinessException("用例异常：无路径配置信息");
        }
        if (StringUtils.isEmpty(taskCaseInfoBo.getRouteFile())) {
            throw new BusinessException("场景异常：场景未验证");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(TaskCaseInfoBo caseInfoBo) {
        List<TaskCaseConfigBo> caseConfigs = caseInfoBo.getCaseConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());
        Map<String, String> businessIdAndRoleMap = caseConfigs.stream().collect(Collectors.toMap(
                TaskCaseConfigBo::getParticipatorId,
                TaskCaseConfigBo::getType));

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
        List<TaskCaseConfigBo> configs = new ArrayList<>();
        for (TaskCaseConfigBo caseConfig : caseConfigs) {
            if (!ids.contains(Integer.valueOf(caseConfig.getDeviceId()))) {
                ids.add(Integer.valueOf(caseConfig.getDeviceId()));
                configs.add(caseConfig);
            }
        }


        List<DeviceConnRule> rules = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            TaskCaseConfigBo sourceDevice = configs.get(i);
            for (int j = 0; j < configs.size(); j++) {
                if (j == i) {
                    continue;
                }
                Map<String, Object> sourceParams = new HashMap<>();
                Map<String, Object> targetParams = new HashMap<>();

                TaskCaseConfigBo targetDevice = configs.get(j);

                DeviceConnRule rule = new DeviceConnRule();
                if (PartRole.MV_SIMULATION.equals(sourceDevice.getType())
                        && PartRole.AV.equals(targetDevice.getType())) {
                    sourceParams = tessParams;
                }
                rule.setSource(createConnInfo(sourceDevice, sourceParams));
                rule.setTarget(createConnInfo(targetDevice, targetParams));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(TaskCaseConfigBo config, Map<String, Object> params) {
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




