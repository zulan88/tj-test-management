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
import net.wanji.business.common.Constants.TaskCaseStatusEnum;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.param.CaseSSInfo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjTaskCaseRecordService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskDataConfigService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private TjTaskDataConfigService taskDataConfigService;

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjTaskCaseRecordService taskCaseRecordService;

    @Autowired
    private TjTaskMapper taskMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @Autowired
    private TaskRedisTrajectoryConsumer taskRedisTrajectoryConsumer;


    @Override
    public TaskCaseVerificationPageVo getStatus(TjTaskCase param) throws BusinessException {
        if (ObjectUtils.isEmpty(param.getId()) && ObjectUtils.isEmpty(param.getTaskId())) {
            throw new BusinessException("参数异常");
        }
        // 1.查询用例详情
        // taskId为空时 单个用例测试
        // caseId为空时 连续性测试
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("查询失败，请检查用例是否存在");
        }
        Map<Integer, TaskCaseInfoBo> caseInfoMap = taskCaseInfos.stream().collect(Collectors.toMap(TaskCaseInfoBo::getId, Function.identity()));
        List<TaskCaseConfigBo> taskCaseConfigs = new ArrayList<>();
        Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap = new HashMap<>();
        Map<Integer, Integer> caseMainSize = new HashMap<>();
        Map<String, String> startMap = new HashMap<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 2.数据校验
            validConfig(taskCaseInfoBo);
            // 3.轨迹详情
            CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(),
                    CaseTrajectoryDetailBo.class);
            if (startMap.size() < 1) {
                // 4.参与者开始点位
                startMap = CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories()).stream().collect(
                                Collectors.toMap(
                                        ParticipantTrajectoryBo::getId,
                                        item -> CollectionUtils.emptyIfNull(item.getTrajectory()).stream()
                                                .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                                                .orElse(new TrajectoryDetailBo()).getPosition()));
            }

            // 5.用例配置
            if (CollectionUtils.isNotEmpty(taskCaseInfoBo.getCaseConfigs())) {
                taskCaseConfigs.addAll(taskCaseInfoBo.getCaseConfigs());
            }
            // 6.参与者ID和参与者名称匹配map
            Map<String, String> businessIdAndRoleMap = taskCaseInfoBo.getCaseConfigs().stream().collect(Collectors.toMap(
                    TaskCaseConfigBo::getParticipatorId,
                    TaskCaseConfigBo::getType));
            caseBusinessIdAndRoleMap.put(taskCaseInfoBo.getId(), businessIdAndRoleMap);
            // 7.用例对应主车轨迹长度
            try {
                List<SimulationTrajectoryDto> simulationTrajectoryDtos = routeService.readOriTrajectoryFromRouteFile(taskCaseInfoBo.getRouteFile(), "1");
                simulationTrajectoryDtos = simulationTrajectoryDtos.stream().filter(t -> CollectionUtils.isNotEmpty(t.getValue())).collect(Collectors.toList());
                caseMainSize.put(taskCaseInfoBo.getId(), simulationTrajectoryDtos.size());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        // 5.重复设备过滤
        taskCaseConfigs = taskCaseConfigs.stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(TaskCaseConfigBo::getDeviceId))), ArrayList::new));
        // 6.查询主轨迹
        Map<String, List<SimulationTrajectoryDto>> mainMap = new HashMap<>();
        taskCaseConfigs.stream().filter(t -> PartRole.AV.equals(t.getType())).findFirst().ifPresent(t -> {
            mainMap.put("mainTrajectories", queryMainTrajectories(param.getTaskId(), param.getId(), t.getParticipatorId()));

        });
        Map<Integer, TaskCaseInfoBo> taskCaseInfoMap = taskCaseInfos.stream().collect(Collectors.toMap(TaskCaseInfoBo::getId, Function.identity()));
        // 7.状态查询
        for (TaskCaseConfigBo taskCaseConfigBo : taskCaseConfigs) {
            if (PartRole.AV.equals(taskCaseConfigBo.getType()) && !ObjectUtils.isEmpty(startMap)) {

                String[] position = startMap.get(taskCaseConfigBo.getParticipatorId()).split(",");
                taskCaseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
                taskCaseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
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
            stateParam.setDeviceId(taskCaseConfigBo.getDeviceId());
            stateParam.setControlChannel(taskCaseConfigBo.getCommandChannel());
            stateParam.setType(1);
            stateParam.setTimestamp(System.currentTimeMillis());
            if (PartRole.AV.equals(taskCaseConfigBo.getType())) {
                // av车需要主车全部轨迹
                stateParam.setParams(new ParamsDto(mainMap.get("mainTrajectories")));
            }
            if (PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getType())) {
                Map<String, Object> tessParams = new HashMap<>();

                List<Map<String, Object>> param1 = new ArrayList<>();
                for (Entry<Integer, TaskCaseInfoBo> caseInfoEntry : caseInfoMap.entrySet()) {
                    TaskCaseInfoBo taskCaseInfoBo = taskCaseInfoMap.get(caseInfoEntry.getKey());
                    Map<String, String> businessIdAndRoleMap = caseBusinessIdAndRoleMap.get(caseInfoEntry.getKey());

                    Map<String, Object> caseParam = new HashMap<>();
                    caseParam.put("caseId", caseInfoEntry.getKey());
                    caseParam.put("avPassTime", caseMainSize.get(caseInfoEntry.getKey()));
                    Label label = new Label();
                    label.setParentId(2L);
                    List<Label> sceneTypeLabelList = labelsService.selectLabelsList(label);
                    List<String> sceneTypes = new ArrayList<>();
                    if (StringUtils.isNotEmpty(taskCaseInfoBo.getAllStageLabel())) {
                        String[] labels = taskCaseInfoBo.getAllStageLabel().split(",");
                        for (String labelId : labels) {
                            for (Label sceneTypeLabel : sceneTypeLabelList) {
                                if (sceneTypeLabel.getId() == Long.parseLong(labelId)) {
                                    sceneTypes.add(sceneTypeLabel.getName());
                                }
                            }
                        }
                    }
                    caseParam.put("type", sceneTypes.stream().collect(Collectors.joining(",")));
                    SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);
                    List<Map<String, Object>> simulationTrajectories = new ArrayList<>();
                    for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
                        if (PartType.MAIN.equals(participantTrajectory.getType())) {
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
                        simulationTrajectories.add(map);
                    }
                    caseParam.put("participantTrajectories", simulationTrajectories);
                    param1.add(caseParam);
                }
                tessParams.put("param1", param1);
                stateParam.setParams(tessParams);
            }
            taskCaseConfigBo.setPositionStatus(deviceDetailService.selectDeviceReadyState(taskCaseConfigBo.getDeviceId(), stateParam, false));
        }
        Map<String, List<TaskCaseConfigBo>> statusMap = taskCaseConfigs.stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getType));
        TaskCaseVerificationPageVo result = new TaskCaseVerificationPageVo();
        result.setTaskId(param.getTaskId());
        result.setTaskCaseId(param.getId());
//        result.setFilePath(taskCaseInfoBo.getFilePath());
//        result.setGeoJsonPath(taskCaseInfoBo.getGeoJsonPath());
        result.setStatusMap(statusMap);
        result.setChannels(taskCaseConfigs.stream().map(TaskCaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
    }

    private List<SimulationTrajectoryDto> queryMainTrajectories(Integer taskId, Integer taskCaseId, String mainId) {
        if (ObjectUtils.isEmpty(taskId) && ObjectUtils.isEmpty(taskCaseId)) {
            return null;
        }
        List<SimulationTrajectoryDto> participantTrajectories = null;
        if (!ObjectUtils.isEmpty(taskCaseId)) {
            TjTaskCase taskCase = getById(taskCaseId);
            TjCase tjCase = caseService.getById(taskCase.getCaseId());
            try {
                participantTrajectories = routeService.readOriTrajectoryFromRouteFile(tjCase.getRouteFile(), mainId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            TjTask tjTask = taskMapper.selectById(taskId);
            try {
                participantTrajectories = routeService.readOriTrajectoryFromRouteFile(tjTask.getRouteFile(), mainId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return participantTrajectories;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(TjTaskCase param) throws BusinessException {
        // 1.用例详情
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        List<Integer> deleteIdList = new ArrayList<>();
        List<TjTaskCaseRecord> addList = new ArrayList<>();
        List<String> channels = new ArrayList<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
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
            TjTaskCaseRecord deleteRecord = new TjTaskCaseRecord();
            deleteRecord.setTaskId(taskCaseInfoBo.getTaskId());
            deleteRecord.setCaseId(taskCaseInfoBo.getId());


            TjTaskCaseRecord caseRecord = taskCaseRecordService.getOne(new QueryWrapper<TjTaskCaseRecord>().eq(ColumnName.TASK_ID, taskCaseInfoBo.getTaskId())
                    .eq(ColumnName.CASE_ID_COLUMN, taskCaseInfoBo.getId()).select("id"));
            if (caseRecord != null) {
                deleteIdList.add(caseRecord.getId());
            }

            TjTaskCaseRecord addRecord = new TjTaskCaseRecord();
            addRecord.setTaskId(taskCaseInfoBo.getTaskId());
            addRecord.setCaseId(taskCaseInfoBo.getId());
            addRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            addRecord.setStatus(TestingStatus.NOT_START);
            addList.add(addRecord);

            channels.addAll(taskCaseInfoBo.getCaseConfigs().stream().map(TaskCaseConfigBo::getDataChannel)
                    .collect(Collectors.toList()));
        }
        Optional.of(deleteIdList).filter(CollectionUtils::isNotEmpty).ifPresent(deleteIds -> taskCaseRecordService.removeByIds(deleteIds));
        taskCaseRecordService.saveBatch(addList);

        // 7.前端结果集
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setTaskId(param.getTaskId());
        caseRealTestVo.setTaskCaseId(param.getId());
        caseRealTestVo.setChannels(new HashSet<>(channels));
        return caseRealTestVo;
    }

    @Override
    public CaseRealTestVo controlTask(Integer taskId, Integer id, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录详情
        TjTaskCase param = new TjTaskCase();
        param.setTaskId(taskId);
        param.setId(id);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("任务用例不存在");
        }

        QueryWrapper<TjTaskCaseRecord> in = new QueryWrapper<TjTaskCaseRecord>().eq(ColumnName.TASK_ID, taskId).in(ColumnName.CASE_ID_COLUMN,
                taskCaseInfos.stream().map(TaskCaseInfoBo::getId).collect(Collectors.toList()));
        List<TjTaskCaseRecord> taskCaseRecords = taskCaseRecordMapper.selectList(in);

        if (CollectionUtils.isEmpty(taskCaseRecords) || taskCaseRecords.stream().anyMatch(t -> t.getStatus() > TestingStatus.NOT_START)) {
            throw new BusinessException("未就绪");
        }

        CaseTrajectoryParam caseTrajectoryParam = new CaseTrajectoryParam();

        for (TaskCaseInfoBo taskCaseInfo : taskCaseInfos) {
            for (TaskCaseConfigBo caseConfig : taskCaseInfo.getCaseConfigs()) {
                if (caseConfig.getType().equals(PartRole.AV)) {
                    caseTrajectoryParam.setDataChannel(caseConfig.getDataChannel());
                    Map<String, String> vehicleTypeMap = new HashMap<>();
                    vehicleTypeMap.put(caseConfig.getParticipatorId(), caseConfig.getType());
                    caseTrajectoryParam.setVehicleTypeMap(vehicleTypeMap);
                    break;
                }
            }
        }
        if (ObjectUtils.isEmpty(caseTrajectoryParam.getDataChannel())) {
            throw new BusinessException("任务数据配置异常");
        }

        caseTrajectoryParam.setTaskId(taskId);

        List<CaseSSInfo> caseSSInfos = new ArrayList<>();
        List<ParticipantTrajectoryBo> mainPoints = new ArrayList<>();
        for (TaskCaseInfoBo taskCase : taskCaseInfos) {
            CaseSSInfo caseSSInfo = new CaseSSInfo();
            if (StringUtils.isEmpty(taskCase.getDetailInfo())) {
                throw new BusinessException(StringUtils.format("用例{}轨迹不存在", taskCase.getCaseNumber()));
            }
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCase.getDetailInfo(), SceneTrajectoryBo.class);
            List<ParticipantTrajectoryBo> avTrajectory = CollectionUtils.emptyIfNull(sceneTrajectoryBo.getParticipantTrajectories()).stream().filter(t -> PartType.MAIN.equals(t.getType())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(avTrajectory)) {
                throw new BusinessException(StringUtils.format("用例{}主车轨迹不存在", taskCase.getCaseNumber()));
            }
            avTrajectory.get(0).getTrajectory().stream().filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst().ifPresent(t -> {
                caseSSInfo.setStartLatitude(Double.parseDouble(t.getLatitude()));
                caseSSInfo.setStartLongitude(Double.parseDouble(t.getLongitude()));
            });
            avTrajectory.get(0).getTrajectory().stream().filter(t -> PointTypeEnum.END.getPointType().equals(t.getType())).findFirst().ifPresent(t -> {
                caseSSInfo.setEndLatitude(Double.parseDouble(t.getLatitude()));
                caseSSInfo.setEndLongitude(Double.parseDouble(t.getLongitude()));
            });
            caseSSInfo.setTaskId(taskCase.getTaskId());
            caseSSInfo.setCaseId(taskCase.getId());
            caseSSInfos.add(caseSSInfo);

            avTrajectory.stream().filter(item -> PartType.MAIN.equals(item.getType())).findFirst().ifPresent(mainPoints::add);

        }
        caseTrajectoryParam.setCaseTrajectorySSList(caseSSInfos);
        restService.sendCaseTrajectoryInfo(caseTrajectoryParam);

        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setTaskId(taskId);
        caseRealTestVo.setTaskCaseId(id);
        caseRealTestVo.setStartTime(DateUtils.getTime());

        List<TrajectoryDetailBo> trajectoryDetailBos = new ArrayList<>();
        for (ParticipantTrajectoryBo mainPoint : mainPoints) {
            trajectoryDetailBos.addAll(mainPoint.getTrajectory());
        }

        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = new HashMap<>();
        mainTrajectoryMap.put("1", trajectoryDetailBos);
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);
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
        taskCaseInfoBo.setTaskCaseRecord(taskCaseRecord);
        taskRedisTrajectoryConsumer.subscribeAndSend(taskCaseInfoBo);
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




