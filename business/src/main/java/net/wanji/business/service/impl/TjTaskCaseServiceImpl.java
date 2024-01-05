package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TaskCaseStatusEnum;
import net.wanji.business.common.Constants.TaskStatusEnum;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseSSInfo;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.param.TessParam;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CaseTreeVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.HistogramVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.mapper.TjTaskDataConfigMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjCaseTreeService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjTaskCaseRecordService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.util.RedisLock;
import net.wanji.common.common.ClientSimulationTrajectoryDto;
import net.wanji.common.common.SimulationTrajectoryDto;
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
import org.springframework.util.StopWatch;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author guowenhao
 * @description 针对表【tj_task_case(测试任务-用例详情表)】的数据库操作Service实现
 * @createDate 2023-08-31 17:39:16
 */
@Service
@Slf4j
public class TjTaskCaseServiceImpl extends ServiceImpl<TjTaskCaseMapper, TjTaskCase>
        implements TjTaskCaseService {

    @Autowired
    private TjDeviceDetailService deviceDetailService;

    @Autowired
    private RestService restService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjTaskCaseRecordService taskCaseRecordService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjCaseTreeService caseTreeService;

    @Autowired
    private TjTaskMapper taskMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @Autowired
    private TjTaskDataConfigMapper taskDataConfigMapper;

    @Autowired
    private KafkaCollector kafkaCollector;

    @Autowired
    private RedisLock redisLock;

    @Override
    public TaskCaseVerificationPageVo getStatus(TjTaskCase param, boolean hand) throws BusinessException {
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
        Integer taskId = taskCaseInfos.get(0).getTaskId();
        TjTask tjTask = taskMapper.selectById(taskId);
        if (ObjectUtils.isEmpty(tjTask)) {
            throw new BusinessException("查询失败，请检查任务是否存在");
        }
        // 2.数据填充
        List<TaskCaseConfigBo> allTaskCaseConfigs = new ArrayList<>();
        Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap = new HashMap<>();
        Map<Integer, Integer> caseMainSize = new HashMap<>();
        Map<String, String> startMap = new HashMap<>();
        Map<String, List<SimulationTrajectoryDto>> mainTrajectoryMap = new HashMap<>();
        Map<Integer, TaskCaseInfoBo> taskCaseInfoMap = new HashMap<>();
        fillStatusParam(param.getId(), taskCaseInfos, allTaskCaseConfigs, caseBusinessIdAndRoleMap, caseMainSize, startMap,
                tjTask.getMainPlanFile(), mainTrajectoryMap, taskCaseInfoMap);
        // 3.重复设备过滤
        List<TaskCaseConfigBo> distTaskCaseConfigs = filterConfigs(allTaskCaseConfigs);
        List<TaskCaseConfigBo> filteredTaskCaseConfigs = distTaskCaseConfigs.stream()
                .filter(t -> !PartRole.MV_SIMULATION.equals(t.getType())).collect(Collectors.toList());
        TaskCaseConfigBo simulationConfig = distTaskCaseConfigs.stream()
                .filter(t -> PartRole.MV_SIMULATION.equals(t.getType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到仿真设备"));
        if (hand) {
            // 先停止
            stop(taskId, param.getId());
            // 4.唤醒仿真服务
            if (!restService.startServer(simulationConfig.getIp(), Integer.valueOf(simulationConfig.getServiceAddress()),
                    buildTessServerParam(1, tjTask.getCreatedBy(), taskId))) {
                throw new BusinessException("唤醒仿真服务失败");
            }
            for(TaskCaseConfigBo taskCaseConfigBo : filteredTaskCaseConfigs){
                if(!redisLock.tryLock("task_"+taskCaseConfigBo.getDataChannel(),SecurityUtils.getUsername())){
                    throw new BusinessException(taskCaseConfigBo.getDeviceName()+"设备正在使用中，请稍后再试");
                }
            }
        }
        // 5.状态查询
        for (TaskCaseConfigBo taskCaseConfigBo : distTaskCaseConfigs) {
            // 查询设备状态
            Integer status = hand
                    ? deviceDetailService.handDeviceState(taskCaseConfigBo.getDeviceId(), getCommandChannelByRole(taskCaseConfigBo), false)
                    : deviceDetailService.selectDeviceState(taskCaseConfigBo.getDeviceId(), getCommandChannelByRole(taskCaseConfigBo), false);
            taskCaseConfigBo.setStatus(status);
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam(taskCaseConfigBo.getDeviceId(), getCommandChannelByRole(taskCaseConfigBo));
            if (PartRole.AV.equals(taskCaseConfigBo.getType())) {
                // av车需要主车全部轨迹
                stateParam.setParams(new ParamsDto("1", mainTrajectoryMap.get("main")));
            }
            if (PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getType())) {
                stateParam.setParams(buildTessStateParam(taskId, taskCaseInfoMap, caseBusinessIdAndRoleMap, caseMainSize));
            }
            Integer readyStatus = hand
                    ? deviceDetailService.handDeviceReadyState(taskCaseConfigBo.getDeviceId(),
                    getReadyStatusChannelByType(taskCaseConfigBo), stateParam, false)
                    : deviceDetailService.selectDeviceReadyState(taskCaseConfigBo.getDeviceId(),
                    getReadyStatusChannelByType(taskCaseConfigBo), stateParam, false);
            taskCaseConfigBo.setPositionStatus(readyStatus);
        }
        // 6.构建页面结果集
        return buildPageVo(param, startMap, allTaskCaseConfigs, distTaskCaseConfigs);
    }

    private String getCommandChannelByRole(TaskCaseConfigBo taskCaseConfigBo) {
        return PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getSupportRoles())
                ? ChannelBuilder.buildTaskControlChannel(SecurityUtils.getUsername(), taskCaseConfigBo.getTaskId())
                : taskCaseConfigBo.getCommandChannel();
    }

    private String getReadyStatusChannelByType(TaskCaseConfigBo taskCaseConfigBo) {
        return PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getSupportRoles())
                ? ChannelBuilder.buildTaskStatusChannel(SecurityUtils.getUsername(), taskCaseConfigBo.getTaskId())
                : ChannelBuilder.DEFAULT_STATUS_CHANNEL;
    }

    /**
     * 构建唤醒tess服务的参数
     *
     * @param roadNum
     * @param taskId
     * @return
     */
    private TessParam buildTessServerParam(Integer roadNum, String username, Integer taskId) {
        return new TessParam().buildTaskParam(roadNum,
                ChannelBuilder.buildTaskDataChannel(username, taskId),
                ChannelBuilder.buildTaskControlChannel(username, taskId),
                ChannelBuilder.buildTaskEvaluateChannel(username, taskId),
                ChannelBuilder.buildTaskStatusChannel(username, taskId));
    }

    /**
     * 构建页面结果集数据
     *
     * @param taskCase
     * @param startMap
     * @param allTaskCaseConfigs
     * @param distTaskCaseConfigs
     * @return
     */
    private TaskCaseVerificationPageVo buildPageVo(TjTaskCase taskCase, Map<String, String> startMap,
                                                   List<TaskCaseConfigBo> allTaskCaseConfigs,
                                                   List<TaskCaseConfigBo> distTaskCaseConfigs) {
        for (TaskCaseConfigBo taskCaseConfigBo : allTaskCaseConfigs) {
            if (PartRole.AV.equals(taskCaseConfigBo.getType()) && !ObjectUtils.isEmpty(startMap)) {
                String[] position = startMap.get(taskCaseConfigBo.getParticipatorId()).split(",");
                taskCaseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
                taskCaseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
            }
        }
        TaskCaseVerificationPageVo result = new TaskCaseVerificationPageVo();
        result.setTaskId(taskCase.getTaskId());
        result.setTaskCaseId(taskCase.getId());
        result.setStatusMap(distTaskCaseConfigs.stream().collect(Collectors.groupingBy(TaskCaseConfigBo::getType)));
        result.setViewMap(allTaskCaseConfigs.stream().collect(Collectors.groupingBy(TaskCaseConfigBo::getType)));
        validStatus(result);
        return result;
    }

    /**
     * 筛选重复设备
     *
     * @param taskCaseConfigs
     * @return
     */
    private List<TaskCaseConfigBo> filterConfigs(List<TaskCaseConfigBo> taskCaseConfigs) {
        return taskCaseConfigs.stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(TaskCaseConfigBo::getDeviceId))), ArrayList::new));
    }

    /**
     * 获取状态接口的数据填充
     *
     * @param taskCaseId
     * @param taskCaseInfos
     * @param taskCaseConfigs
     * @param caseBusinessIdAndRoleMap
     * @param caseMainSize
     * @param startMap
     * @param mainPlanFile
     * @param mainTrajectoryMap
     * @param taskCaseInfoMap
     * @throws BusinessException
     */
    private void fillStatusParam(Integer taskCaseId,
                                 List<TaskCaseInfoBo> taskCaseInfos,
                                 List<TaskCaseConfigBo> taskCaseConfigs,
                                 Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap,
                                 Map<Integer, Integer> caseMainSize,
                                 Map<String, String> startMap,
                                 String mainPlanFile,
                                 Map<String, List<SimulationTrajectoryDto>> mainTrajectoryMap,
                                 Map<Integer, TaskCaseInfoBo> taskCaseInfoMap) throws BusinessException {
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 2.数据校验
            validConfig(taskCaseInfoBo);
            // 3.轨迹详情(使用实车试验的点位配置)
            SceneTrajectoryBo trajectoryDetail = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(),
                    SceneTrajectoryBo.class);
            if (taskCaseInfoBo.getSort() == 1) {
                // 4.参与者开始点位
                for (ParticipantTrajectoryBo trajectoryBo : CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories())) {
                    startMap.put(trajectoryBo.getId(), CollectionUtils.emptyIfNull(trajectoryBo.getTrajectory()).stream()
                            .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                            .orElse(new TrajectoryDetailBo()).getPosition());
                }
            }
            if (CollectionUtils.isEmpty(taskCaseInfoBo.getDataConfigs())) {
                log.error("任务 {} 用例 {} 配置信息异常", taskCaseInfoBo.getTaskId(), taskCaseInfoBo.getCaseId());
                throw new BusinessException("任务用例配置信息异常");
            }
            // 5.用例配置
            taskCaseConfigs.addAll(taskCaseInfoBo.getDataConfigs());
            // 6.参与者ID和参与者名称匹配map
            Map<String, String> businessIdAndRoleMap = taskCaseInfoBo.getDataConfigs().stream().collect(Collectors.toMap(
                    TaskCaseConfigBo::getParticipatorId,
                    TaskCaseConfigBo::getType));
            caseBusinessIdAndRoleMap.put(taskCaseInfoBo.getCaseId(), businessIdAndRoleMap);

            // 7.用例对应主车轨迹长度(使用仿真验证的轨迹)
            try {
                List<SimulationTrajectoryDto> trajectories = routeService.readOriRouteFile(taskCaseInfoBo.getRouteFile());
                trajectories = trajectories.stream()
                        .filter(item -> !ObjectUtils.isEmpty(item.getValue())
                                && item.getValue().stream()
                                .anyMatch(p -> PartRole.AV.equals(businessIdAndRoleMap.get(p.getId()))))
                        .peek(s -> s.setValue(s.getValue().stream()
                                .filter(p -> PartRole.AV.equals(businessIdAndRoleMap.get(p.getId())))
                                .collect(Collectors.toList())))
                        .collect(Collectors.toList());
                // 若taskCaseId不为空，则按用例进行，使用用例仿真验证的轨迹
                if (!ObjectUtils.isEmpty(taskCaseId) && taskCaseId.equals(taskCaseInfoBo.getId())) {
                    mainTrajectoryMap.put("main", trajectories);
                }
                caseMainSize.put(taskCaseInfoBo.getCaseId(), trajectories.size());
            } catch (IOException e) {
                log.error("用例轨迹文件读取失败：{}", e.getMessage());
            }
        }
        // 8.taskCaseId为空时，则按整体任务进行，使用规划后的轨迹
        if (ObjectUtils.isEmpty(taskCaseId)) {
            try {
                List<SimulationTrajectoryDto> main = routeService.readOriRouteFile(mainPlanFile);
                mainTrajectoryMap.put("main", main);
            } catch (IOException e) {
                log.error("主车规划路径文件读取失败：{}", e.getMessage());
                throw new BusinessException("读取主车已规划路径文件失败");
            } catch (NullPointerException v2) {
                throw new BusinessException("主车轨迹未规划");
            }
        }
        for (TaskCaseInfoBo caseInfoBo : taskCaseInfos) {
            taskCaseInfoMap.put(caseInfoBo.getCaseId(), caseInfoBo);
        }
    }

    /**
     * 创建tess准备状态参数
     *
     * @param taskCaseInfoMap
     * @param caseBusinessIdAndRoleMap
     * @param caseMainSize
     * @return
     */
    private Map<String, Object> buildTessStateParam(Integer taskId, Map<Integer, TaskCaseInfoBo> taskCaseInfoMap,
                                                    Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap,
                                                    Map<Integer, Integer> caseMainSize) {
        Map<String, Object> tessParams = new HashMap<>();

        List<Map<String, Object>> param1 = new ArrayList<>();
        for (Entry<Integer, TaskCaseInfoBo> caseInfoEntry : taskCaseInfoMap.entrySet()) {
            Integer caseId = caseInfoEntry.getKey();
            TaskCaseInfoBo taskCaseInfoBo = caseInfoEntry.getValue();
            Map<String, String> businessIdAndRoleMap = caseBusinessIdAndRoleMap.get(caseId);

            Map<String, Object> caseParam = new HashMap<>();
            caseParam.put("caseId", caseId);
            caseParam.put("sort", taskCaseInfoBo.getSort());
            caseParam.put("avPassTime", caseMainSize.get(caseId));
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
            caseParam.put("type", String.join(",", sceneTypes));
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);
            List<Map<String, Object>> simulationTrajectories = new ArrayList<>();
            for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
                if (PartType.MAIN.equals(participantTrajectory.getType())) {
                    tessParams.put("avId", participantTrajectory.getId());
                    tessParams.put("avName", participantTrajectory.getName());
                    continue;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("id", participantTrajectory.getId());
                map.put("model", participantTrajectory.getModel());
                map.put("name", caseId + "_" + participantTrajectory.getName());
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
        param1.sort(Comparator.comparingInt(t -> (int) t.get("sort")));
        tessParams.put("param1", param1);
        tessParams.put("taskId", taskId);
        return tessParams;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(TjTaskCase param) throws BusinessException {
        // 1.用例详情
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("测试任务用例不存在");
        }
        Integer taskId = taskCaseInfos.get(0).getTaskId();
        List<Integer> resetList = new ArrayList<>();
        List<Integer> deleteIdList = new ArrayList<>();
        List<TjTaskCaseRecord> addList = new ArrayList<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 2.校验数据
            validConfig(taskCaseInfoBo);
            // 3.轨迹详情
            CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                    JSONObject.parseObject(taskCaseInfoBo.getDetailInfo(), CaseTrajectoryDetailBo.class);
            // 4.角色配置信息
            Map<String, List<TaskCaseConfigBo>> partMap = taskCaseInfoBo.getDataConfigs().stream().collect(
                    Collectors.groupingBy(TaskCaseConfigBo::getSupportRoles));
            // 5.各角色数量
            int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
            int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
            int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
            caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                    simulationNum, pedestrianNum));
            caseTrajectoryDetailBo.setSceneDesc(taskCaseInfoBo.getSceneName());
            // 6.删除空记录
            List<TjTaskCaseRecord> caseRecords = taskCaseRecordService.list(new LambdaQueryWrapper<TjTaskCaseRecord>()
                    .eq(TjTaskCaseRecord::getTaskId, taskCaseInfoBo.getTaskId())
                    .eq(TjTaskCaseRecord::getCaseId, taskCaseInfoBo.getCaseId())
                    .isNull(TjTaskCaseRecord::getRouteFile)
                    .select(TjTaskCaseRecord::getId));
            Optional.of(caseRecords).filter(CollectionUtils::isNotEmpty).ifPresent(records -> records.forEach(record -> {
                deleteIdList.add(record.getId());
            }));
            // 7.新增记录
            TjTaskCaseRecord addRecord = new TjTaskCaseRecord();
            addRecord.setTaskId(taskCaseInfoBo.getTaskId());
            addRecord.setCaseId(taskCaseInfoBo.getCaseId());
            addRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            addRecord.setStatus(TestingStatusEnum.NO_PASS.getCode());
            addList.add(addRecord);
            // 8.重置测试用例
            resetList.add(taskCaseInfoBo.getId());
        }
        Optional.of(deleteIdList).filter(CollectionUtils::isNotEmpty).ifPresent(deleteIds -> taskCaseRecordService.removeByIds(deleteIds));
        Optional.of(addList).filter(CollectionUtils::isNotEmpty).ifPresent(records -> taskCaseRecordService.saveBatch(records));
        Optional.of(resetList).filter(CollectionUtils::isNotEmpty).ifPresent(resets -> taskCaseMapper.reset(resets));

        // 9.前端结果集
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setId(addList.get(0).getId());
        caseRealTestVo.setTaskId(taskId);
        caseRealTestVo.setTaskCaseId(param.getId());
        return caseRealTestVo;
    }

    @Override
    public CaseRealTestVo controlTask(Integer taskId, Integer taskCaseId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录详情
        TjTask tjTask = taskMapper.selectById(taskId);
        TjTaskCase param = new TjTaskCase();
        param.setTaskId(taskId);
        param.setId(taskCaseId);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (ObjectUtils.isEmpty(tjTask) || CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("任务用例不存在");
        }
        CaseTrajectoryParam caseTrajectoryParam = new CaseTrajectoryParam();
        for (TaskCaseInfoBo taskCaseInfo : taskCaseInfos) {
            for (TaskCaseConfigBo caseConfig : taskCaseInfo.getDataConfigs()) {
                if (caseConfig.getType().equals(PartRole.AV)) {
                    caseTrajectoryParam.setDataChannel(caseConfig.getDataChannel());
                    Map<String, String> vehicleTypeMap = new HashMap<>();
                    vehicleTypeMap.put(caseConfig.getType(), caseConfig.getParticipatorId());
                    caseTrajectoryParam.setVehicleIdTypeMap(vehicleTypeMap);
                    break;
                }
            }
        }
        if (ObjectUtils.isEmpty(caseTrajectoryParam.getDataChannel())) {
            throw new BusinessException("任务数据配置异常");
        }
        caseTrajectoryParam.setTaskId(taskId);
        caseTrajectoryParam.setCaseId(taskCaseId);

        List<CaseSSInfo> caseSSInfos = new ArrayList<>();
        List<ParticipantTrajectoryBo> mainPoints = new ArrayList<>();
        for (TaskCaseInfoBo taskCase : taskCaseInfos) {
            CaseSSInfo caseSSInfo = new CaseSSInfo();
            caseSSInfo.setCaseId(taskCase.getCaseId());
            if (StringUtils.isEmpty(taskCase.getDetailInfo())) {
                throw new BusinessException(StringUtils.format("用例{}轨迹不存在", taskCase.getCaseNumber()));
            }
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCase.getDetailInfo(), SceneTrajectoryBo.class);
            CollectionUtils.emptyIfNull(sceneTrajectoryBo.getParticipantTrajectories()).stream().filter(p ->
                    PartType.MAIN.equals(p.getType())).findFirst().ifPresent(f -> {

                caseSSInfo.setTrajectoryPoints(f.getTrajectory().stream().map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("latitude", t.getLatitude());
                    map.put("longitude", t.getLongitude());
                    return map;
                }).collect(Collectors.toList()));
                caseTrajectoryParam.setCaseTrajectorySSVoList(Collections.singletonList(caseSSInfo));

                Map<String, String> vehicleTypeMap = new HashMap<>();
                vehicleTypeMap.put(PartRole.AV, f.getId());
                caseTrajectoryParam.setVehicleIdTypeMap(vehicleTypeMap);

                mainPoints.add(f);
            });
            caseSSInfos.add(caseSSInfo);
        }
        caseTrajectoryParam.setCaseTrajectorySSVoList(caseSSInfos);

        // 更新kafka收集器
        String key = ChannelBuilder.buildTaskDataChannel(SecurityUtils.getUsername(), taskId);
        kafkaCollector.remove(key, null);


        Map<String, Object> context = new HashMap<>();
        context.put("username", SecurityUtils.getUsername());
        caseTrajectoryParam.setContext(context);
        // 向主控发送主车信息
        restService.sendCaseTrajectoryInfo(caseTrajectoryParam);

        // 前端结果集
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setTaskId(taskId);
        caseRealTestVo.setTaskCaseId(taskCaseId);
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

    @Override
    public CaseRealTestVo getTaskInfo(Integer taskId) throws BusinessException {
        TjTaskCase taskCase = taskCaseMapper.selectOne(new LambdaQueryWrapper<TjTaskCase>().eq(TjTaskCase::getTaskId, taskId).eq(TjTaskCase::getStatus, TaskCaseStatusEnum.RUNNING.getCode()));
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setTaskId(taskId);
        caseRealTestVo.setTaskCaseId(taskCase.getCaseId());
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(taskCase.getCaseId());
        caseRealTestVo.setTestTypeName(caseInfoBo.getTestScene());
        return caseRealTestVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo caseStartEnd(Integer taskId, Integer caseId, Integer action, boolean taskEnd, Map<String, Object> context) throws BusinessException {
        StopWatch stopWatch = new StopWatch(StringUtils.format("任务控制 - 任务ID:{} 用例ID:{}", taskId, caseId));
        stopWatch.start("1.查询校验任务用例详情");
        log.info("任务{} 用例{} ,action:{}, 上下文：{}, 任务终止:{}", taskId, caseId, action, JSONObject.toJSONString(context), taskEnd);
        // 1.任务用例测试记录详情
        TjTaskCaseRecord taskCaseRecord = ssGetTjTaskCaseRecord(taskId, caseId, action);
        // 2.任务用例详情
        TjTaskCase taskCase = getTjTaskCaseByTaskCaseRecord(taskId, caseId);
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseByCondition(taskCase).get(0);
        // 3.校验数据
        validConfig(taskCaseInfoBo);
        Optional<TaskCaseConfigBo> first = taskCaseInfoBo.getDataConfigs()
                .stream().filter(e -> PartRole.AV.equals(e.getType())).findFirst();
        if (!first.isPresent()) {
            throw new BusinessException("未查询到主车配置信息");
        }
        TaskCaseConfigBo mainConfig = first.get();
        stopWatch.stop();

        stopWatch.start("2.启动tessng服务");
        // 4.启动tessng服务
        TessParam tessParam = buildTessServerParam(1, (String) context.get("username"), taskId);
        stopWatch.stop();

        stopWatch.start("3.向主控发送规则向主控发送规则");
        // 5.向主控发送控制请求
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(), taskId, caseId, action > 0 ? action : 0,
                        generateDeviceConnRules(taskCaseInfoBo, tessParam.getCommandChannel(), tessParam.getDataChannel()),
                        mainConfig.getCommandChannel(), taskEnd))) {
            throw new BusinessException("主控响应异常");
        }
        stopWatch.stop();

        stopWatch.start("4.业务处理，构建结果集");
        // 6.业务处理
        if (1 == action) {
            ssCaseResultUpdate(action, taskCaseRecord, mainConfig, taskCase,null, null);
        } else {
            TjTask tjTask = taskMapper.selectById(taskId);
            String key = ChannelBuilder.buildTaskDataChannel(tjTask.getCreatedBy(), taskId);
            try {
                List<List<ClientSimulationTrajectoryDto>> trajectories = kafkaCollector.take(key, caseId);
                String duration = DateUtils.secondsToDuration((int) Math.floor(
                        (double) (CollectionUtils.isEmpty(trajectories) ? 0 : trajectories.size()) / 10));
                ssCaseResultUpdate(action, taskCaseRecord, mainConfig, taskCase, duration, trajectories);
                RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null, null, duration);
                WebSocketManage.sendInfo(key, JSON.toJSONString(endMsg));
            } finally {
                if (taskEnd) {
                    kafkaCollector.remove(key, null);
                }
            }
        }
        // 7.前端结果集
        CaseRealTestVo caseRealTestVo = ssGetCaseRealTestVo(taskCaseRecord);
        stopWatch.stop();
        log.info("耗时：{}", stopWatch.prettyPrint());
        return caseRealTestVo;
    }


    @Override
    public void playback(Integer taskId, Integer caseId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录
        TjTaskCase param = new TjTaskCase();
        param.setTaskId(taskId);
        param.setCaseId(caseId);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);

        List<List<ClientSimulationTrajectoryDto>> trajectories = new ArrayList<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            Optional<TjTaskCaseRecord> optional = CollectionUtils.emptyIfNull(taskCaseInfoBo.getRecords()).stream()
                    .filter(r -> TestingStatusEnum.PASS.getCode().equals(r.getStatus()))
                    .findFirst();
            if (!optional.isPresent()) {
                continue;
            }
            TjTaskCaseRecord tjTaskCaseRecord = optional.get();
            trajectories.addAll(routeService.readRealTrajectoryFromRouteFile2(tjTaskCaseRecord.getRouteFile()));
        }
        // 2.数据校验
        if (CollectionUtils.isEmpty(trajectories)) {
            throw new BusinessException("未查询到任何可用轨迹文件，请先进行试验");
        }
        TjTaskDataConfig dataConfig = taskDataConfigMapper.selectOne(new LambdaQueryWrapper<TjTaskDataConfig>().eq(TjTaskDataConfig::getTaskId, taskId)
                .eq(TjTaskDataConfig::getType, PartRole.AV));
        TjDeviceDetail avDevice = deviceDetailService.getById(dataConfig.getDeviceId());
        String key = ChannelBuilder.buildTaskPreviewChannel(SecurityUtils.getUsername(), taskId, caseId);
        switch (action) {
            case PlaybackAction.START:
                RealPlaybackSchedule.startSendingData(key, avDevice.getDataChannel(), trajectories);
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
    public Object getEvaluation(Integer taskId, Integer id) throws BusinessException {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("taskId", taskId);
            result.put("id", id);
            result.put("score", "90.00");
            result.put("time", 60);
            result.put("startTime", "2024-01-05 14:00:00");
            result.put("endTime", "2024-01-05 14:01:00");
            result.put("hazardRatio", "52");

            HistogramVo histogramVo = new HistogramVo();
            histogramVo.setType(Arrays.asList("优秀", "良好", "一般", "较差", "很差"));
            List<Object> data = new ArrayList<>();
            data.add(Arrays.asList(1, 0, 1));
            data.add(Arrays.asList(0, 1, 1));
            data.add(Arrays.asList(1, 1, 0));
            data.add(Arrays.asList(0, 0, 1));
            data.add(Arrays.asList(0, 1, 0));
            histogramVo.setData(data);
            histogramVo.setXAxis(Arrays.asList("安全性", "舒适性", "效率性"));
            result.put("chart", histogramVo);
        } catch (Exception e) {
            throw new BusinessException("获取评价信息失败");
        } finally {
            unLock(taskId);
        }
        return result;
    }

    @Override
    public List<RealTestResultVo> getResult(Integer taskId, Integer id) throws BusinessException {
        TjTask task = taskMapper.selectById(taskId);
        if (ObjectUtils.isEmpty(task)) {
            throw new BusinessException("未查询到任务信息");
        }
        TjTaskCase param = new TjTaskCase();
        param.setTaskId(taskId);
        param.setId(id);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("未查询到任务用例信息");
        }
        List<RealTestResultVo> result = new ArrayList<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            Optional<TjTaskCaseRecord> optional = CollectionUtils.emptyIfNull(taskCaseInfoBo.getRecords()).stream()
                    .filter(r -> TestingStatusEnum.PASS.getCode().equals(r.getStatus()))
                    .findFirst();
            if (!optional.isPresent()) {
                continue;
            }
            TjTaskCaseRecord taskCaseRecord = optional.get();
            CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                    CaseTrajectoryDetailBo.class);
            List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
                    .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
            caseTrajectoryDetailBo.setParticipantTrajectories(trajectoryBos);
            RealTestResultVo realTestResultVo = new RealTestResultVo();
            BeanUtils.copyProperties(caseTrajectoryDetailBo, realTestResultVo);
            realTestResultVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, task.getTestType()));
            realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
            realTestResultVo.setId(id);
            realTestResultVo.setTaskId(taskId);
            realTestResultVo.setRecordId(taskCaseRecord.getId());
            realTestResultVo.setStartTime(taskCaseRecord.getStartTime());
            realTestResultVo.setEndTime(taskCaseRecord.getEndTime());
            result.add(realTestResultVo);
        }
        unLock(taskId);
        return result;
    }

    @Override
    public CommunicationDelayVo communicationDelayVo(Integer taskId, Integer id) throws BusinessException {
        List<Map<String, Object>> infos = taskCaseRecordMapper.recordPartInfo(id);
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

    @Override
    public void stop(Integer taskId, Integer taskCaseId) throws BusinessException {
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        taskCase.setId(taskCaseId);
        List<TaskCaseInfoBo> taskCaseInfoBos = taskCaseMapper.selectTaskCaseByCondition(taskCase);
        TjTask tjTask = taskMapper.selectById(taskId);
        if (ObjectUtils.isEmpty(tjTask) || CollectionUtils.isEmpty(taskCaseInfoBos)) {
            throw new BusinessException("未查询到任务信息");
        }
        TaskCaseInfoBo taskCaseInfoBo = taskCaseInfoBos.get(taskCaseInfoBos.size() - 1);
        if (CollectionUtils.isEmpty(taskCaseInfoBo.getDataConfigs())) {
            throw new BusinessException("未查询到配置信息");
        }
        Optional<TaskCaseConfigBo> first = taskCaseInfoBo.getDataConfigs()
                .stream().filter(e -> PartRole.AV.equals(e.getType())).findFirst();
        if (!first.isPresent()) {
            throw new BusinessException("未查询到主车配置信息");
        }
        TessParam tessParam = buildTessServerParam(1, tjTask.getCreatedBy(), taskId);
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(),
                        taskId, taskCaseInfoBo.getCaseId(), 0,
                        generateDeviceConnRules(taskCaseInfoBo, tessParam.getCommandChannel(), tessParam.getDataChannel()),
                        first.get().getCommandChannel(), true))) {
            throw new BusinessException("主控响应异常");
        }

    }

    @Override
    public void manualTermination(Integer taskId, Integer taskCaseId) throws BusinessException {
        Integer caseId = 0;
        if (!ObjectUtils.isEmpty(taskCaseId)) {
            TjTaskCase taskCase = this.getById(taskCaseId);
            caseId = taskCase.getCaseId();
        }
        if (!restService.sendManualTermination(taskId, caseId)) {
            throw new BusinessException("任务终止失败");
        }
    }

    @Override
    public List<CaseTreeVo> selectTree(String type, Integer taskId) {
        List<CaseTreeVo> caseTree = caseTreeService.selectTree(type, null);
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        List<TaskCaseVo> taskCaseList = taskCaseMapper.selectByCondition(taskCase);
        Map<Integer, Long> caseCountMap = CollectionUtils.emptyIfNull(taskCaseList).stream()
                .collect(Collectors.groupingBy(TaskCaseVo::getTreeId, Collectors.counting()));
        for (CaseTreeVo treeVo : CollectionUtils.emptyIfNull(caseTree)) {
            treeVo.setNumber(caseCountMap.containsKey(treeVo.getId()) ? caseCountMap.get(treeVo.getId()).intValue() : 0);
        }
        return caseTree;
    }

    @Override
    public boolean addTaskCase(@NotNull Integer taskId, @NotNull List<Integer> caseIds) throws BusinessException {
        List<TjTaskCase> addedList = list(new QueryWrapper<TjTaskCase>().eq(ColumnName.TASK_ID, taskId)
                .in(ColumnName.CASE_ID_COLUMN, caseIds));
        List<Integer> addedIdList = CollectionUtils.emptyIfNull(addedList).stream()
                .map(TjTaskCase::getCaseId).collect(Collectors.toList());
        caseIds.removeAll(addedIdList);

        List<TjCase> cases = caseService.listByIds(caseIds);
        Map<Integer, TjCase> caseMap = CollectionUtils.emptyIfNull(cases).stream()
                .collect(Collectors.toMap(TjCase::getId, Function.identity()));

        List<TjTaskCase> saveList = new ArrayList<>();
        for (Integer caseId : caseIds) {
            if (!caseMap.containsKey(caseId) || StringUtils.isEmpty(caseMap.get(caseId).getRouteFile())) {
                throw new BusinessException(StringUtils.format("未查询到用例{}可用轨迹文件", caseId));
            }
            TjTaskCase taskCase = new TjTaskCase();
            taskCase.setTaskId(taskId);
            taskCase.setCaseId(caseId);
            taskCase.setSort(0);
            taskCase.setCreateTime(new Date());
            taskCase.setStatus(TaskCaseStatusEnum.WAITING.getCode());
            taskCase.setRouteFile(caseMap.get(caseId).getRouteFile());
            taskCase.setDetailInfo(caseMap.get(caseId).getDetailInfo());
            saveList.add(taskCase);
        }
        return saveBatch(saveList);
    }

    @Override
    public boolean deleteTaskCase(@NotNull Integer taskId, @NotNull List<Integer> caseIds) {
        return remove(new QueryWrapper<TjTaskCase>().eq(ColumnName.TASK_ID, taskId)
                .in(ColumnName.CASE_ID_COLUMN, caseIds));
    }

    private void validStatus(TaskCaseVerificationPageVo pageVo) {
        Map<String, List<TaskCaseConfigBo>> statusMap = pageVo.getStatusMap();
        List<TaskCaseConfigBo> configs = new ArrayList<>();
        statusMap.values().stream().flatMap(List::stream).forEach(configs::add);
        StringBuilder messageBuilder = new StringBuilder();
        for (TaskCaseConfigBo config : configs) {
            if (ObjectUtils.isEmpty(config.getStatus()) || YN.Y_INT != config.getStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_OFFLINE_TEMPLATE,
                        config.getDeviceName()));
            }
            if (ObjectUtils.isEmpty(config.getPositionStatus()) || YN.Y_INT != config.getPositionStatus()) {
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
        if (CollectionUtils.isEmpty(taskCaseInfoBo.getDataConfigs())) {
            throw new BusinessException("任务异常：任务未进行设备配置");
        }
        if (taskCaseInfoBo.getDataConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("用例异常：用例未进行设备配置");
        }
        if (StringUtils.isEmpty(taskCaseInfoBo.getDetailInfo())) {
            throw new BusinessException("用例异常：无路径配置信息");
        }
        if (StringUtils.isEmpty(taskCaseInfoBo.getRouteFile())) {
            throw new BusinessException("用例异常：未进行仿真验证");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(TaskCaseInfoBo caseInfoBo, String commandChannel, String dataChannel) {
        List<TaskCaseConfigBo> caseConfigs = caseInfoBo.getDataConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());

        List<Integer> ids = new ArrayList<>();
        List<TaskCaseConfigBo> configs = new ArrayList<>();
        for (TaskCaseConfigBo caseConfig : caseConfigs) {
            if (!ids.contains(caseConfig.getDeviceId())) {
                ids.add(caseConfig.getDeviceId());
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

                rule.setSource(createConnInfo(sourceDevice, commandChannel, dataChannel, sourceParams));
                rule.setTarget(createConnInfo(targetDevice, commandChannel, dataChannel, targetParams));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(TaskCaseConfigBo config, String commandChannel, String dataChannel,
                                                 Map<String, Object> params) {
        return PartRole.MV_SIMULATION.equals(config.getSupportRoles())
                ? createSimulationConnInfo(String.valueOf(config.getDeviceId()), commandChannel, dataChannel, params)
                : new DeviceConnInfo(String.valueOf(config.getDeviceId()), config.getCommandChannel(),
                config.getDataChannel(), params);
    }

    private static DeviceConnInfo createSimulationConnInfo(String deviceId, String commandChannel, String dataChannel,
                                                           Map<String, Object> params) {
        return new DeviceConnInfo(deviceId, commandChannel, dataChannel, params);
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

    private TjTaskCaseRecord ssGetTjTaskCaseRecord(Integer taskId, Integer caseId, Integer action)
            throws BusinessException {
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectOne(new LambdaQueryWrapper<TjTaskCaseRecord>()
                .eq(TjTaskCaseRecord::getTaskId, taskId)
                .eq(TjTaskCaseRecord::getCaseId, caseId)
                .isNull(TjTaskCaseRecord::getRouteFile));
        if (ObjectUtils.isEmpty(taskCaseRecord)) {
            throw new BusinessException("未找到用例测试记录");
        }
        return taskCaseRecord;
    }

    private TjTaskCase getTjTaskCaseByTaskCaseRecord(Integer taskId, Integer caseId) {
        return taskCaseMapper.selectOne(new LambdaQueryWrapper<TjTaskCase>()
                .eq(TjTaskCase::getCaseId, caseId).eq(TjTaskCase::getTaskId, taskId));
    }

    private void ssCaseResultUpdate(Integer action, TjTaskCaseRecord taskCaseRecord,
                                    TaskCaseConfigBo mainConfig,
                                    TjTaskCase taskCase, String duration,
                                    List<List<ClientSimulationTrajectoryDto>> trajectories) throws BusinessException {
        if (1 == action) {
            Date date = new Date();
            LocalDateTime now = DateUtils.dateToLDT(date);
            taskCaseRecord.setStartTime(now);
            taskCaseRecordMapper.updateById(taskCaseRecord);

            taskCase.setStartTime(date);
            taskCase.setStatus(TaskCaseStatusEnum.RUNNING.getCode());
            this.updateById(taskCase);

            TjTask tjTask = taskMapper.selectById(taskCaseRecord.getTaskId());
            if (ObjectUtils.isEmpty(tjTask.getStartTime())) {
                tjTask.setStartTime(date);
                taskMapper.updateById(tjTask);
            }
        } else {

            try {
                routeService.checkMain(trajectories, mainConfig.getDataChannel());
                routeService.saveTaskRouteFile2(taskCaseRecord, trajectories, action);
            } catch (Exception e) {
                log.error("保存轨迹文件异常:{}", e);
                throw new BusinessException("保存轨迹文件失败");
            }

            log.info("save task case info");
            taskCase.setTestTotalTime(String.valueOf(DateUtils.durationToSeconds(duration)));
            taskCase.setEndTime(new Date());
            taskCase.setStatus(TaskCaseStatusEnum.FINISHED.getCode());
            taskCase.setPassingRate(0 == action ? "100%" : "0%");
            QueryWrapper<TjTaskCase> updateMapper = new QueryWrapper<>();
            updateMapper.eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId()).eq(ColumnName.CASE_ID_COLUMN,
                    taskCaseRecord.getCaseId());
            taskCaseMapper.update(taskCase, updateMapper);

            QueryWrapper<TjTaskCase> queryMapper = new QueryWrapper<>();
            queryMapper.eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
            List<TjTaskCase> tjTaskCases = taskCaseMapper.selectList(queryMapper);
            if (CollectionUtils.emptyIfNull(tjTaskCases).stream().allMatch(item ->
                    TaskCaseStatusEnum.FINISHED.getCode().equals(item.getStatus()))) {
                log.info("任务{}下所有用例已完成", taskCaseRecord.getTaskId());
                TjTask tjTask = new TjTask();
                tjTask.setId(tjTaskCases.get(0).getTaskId());
                tjTask.setEndTime(new Date());
                tjTask.setTestTotalTime(DateUtils.secondsToDuration(tjTaskCases.stream().mapToInt(caseObj ->
                        Integer.parseInt(caseObj.getTestTotalTime())).sum()));
                tjTask.setStatus(TaskStatusEnum.FINISHED.getCode());
                taskMapper.updateById(tjTask);
            }
        }

    }

    private static CaseRealTestVo ssGetCaseRealTestVo(
            TjTaskCaseRecord taskCaseRecord) {
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(taskCaseRecord, caseRealTestVo);
        caseRealTestVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(
                taskCaseRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);
        return caseRealTestVo;
    }

    /**
     * 辅助方法：根据指定的属性值去重
     *
     * @param keyExtractor
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private void unLock(Integer taskId){
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(taskCase);
        List<TaskCaseConfigBo> taskCaseConfigs = new ArrayList<>();
        for(TaskCaseInfoBo taskCaseInfo : taskCaseInfos){
            taskCaseConfigs.addAll(taskCaseInfo.getDataConfigs());
        }
        taskCaseConfigs = filterConfigs(taskCaseConfigs);
        List<TaskCaseConfigBo> filteredTaskCaseConfigs = taskCaseConfigs.stream()
                .filter(t -> !PartRole.MV_SIMULATION.equals(t.getType())).collect(Collectors.toList());
        for(TaskCaseConfigBo taskCaseConfigBo : filteredTaskCaseConfigs){
            redisLock.releaseLock("task_"+taskCaseConfigBo.getDataChannel(),SecurityUtils.getUsername());
        }
    }
}




