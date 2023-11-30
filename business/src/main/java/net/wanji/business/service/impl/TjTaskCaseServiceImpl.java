package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TaskCaseStatusEnum;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.DeviceStateDto;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseSSInfo;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CaseTreeVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.DeviceStateSendService;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjCaseTreeService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.service.TjTaskCaseRecordService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.TaskRedisTrajectoryConsumer;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

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
    private DeviceStateSendService deviceStateSendService;

    @Autowired
    private TjCaseTreeService caseTreeService;

    @Autowired
    private RedisTemplate<String, Object> noClassRedisTemplate;

    @Autowired
    private TjTaskMapper taskMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @Autowired
    private TaskRedisTrajectoryConsumer taskRedisTrajectoryConsumer;


    @Override
    public void resetStatus(TjTaskCase param) throws BusinessException {
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
        TjTask tjTask = taskMapper.selectById(param.getTaskId());
        if (ObjectUtils.isEmpty(tjTask)) {
            throw new BusinessException("查询失败，请检查任务是否存在");
        }

        // 先停止
        stop(param.getTaskId(), param.getId());

        List<TaskCaseConfigBo> taskCaseConfigs = taskCaseInfos.stream().flatMap(t -> t.getDataConfigs().stream()).filter(distinctByKey(TaskCaseConfigBo::getDeviceId)).collect(Collectors.toList());
        Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap = new HashMap<>();
        Map<Integer, Integer> caseMainSize = new HashMap<>();
        Map<String, List<SimulationTrajectoryDto>> mainTrajectoryMap = new HashMap<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 1.数据校验
            validConfig(taskCaseInfoBo);

            // 2.用例配置
            if (CollectionUtils.isNotEmpty(taskCaseInfoBo.getDataConfigs())) {
                taskCaseConfigs.addAll(taskCaseInfoBo.getDataConfigs());
            }
            // 3.参与者ID和参与者名称匹配map
            Map<String, String> businessIdAndRoleMap = taskCaseInfoBo.getDataConfigs().stream().collect(Collectors.toMap(
                    TaskCaseConfigBo::getParticipatorId,
                    TaskCaseConfigBo::getType));
            caseBusinessIdAndRoleMap.put(taskCaseInfoBo.getCaseId(), businessIdAndRoleMap);
            // 4.用例对应主车轨迹长度(使用实车试验的轨迹)
            List<RealTestTrajectoryDto> realTestTrajectoryDtos = routeService.readRealTrajectoryFromRouteFile(taskCaseInfoBo.getRealRouteFile());
            realTestTrajectoryDtos.stream().filter(RealTestTrajectoryDto::isMain).findFirst().ifPresent(t -> {
                if (!ObjectUtils.isEmpty(param.getId())) {
                    mainTrajectoryMap.put("main", t.getData());
                }
                caseMainSize.put(taskCaseInfoBo.getCaseId(), t.getData().size());
            });
        }
        // 查询主车轨迹
        if (ObjectUtils.isEmpty(param.getId())) {
            try {
                List<SimulationTrajectoryDto> main = routeService.readOriTrajectoryFromRouteFile(tjTask.getMainPlanFile(), "1");
                mainTrajectoryMap.put("main", main);
            } catch (IOException e) {
                log.error("主车规划路径文件读取失败：{}", e);
                throw new BusinessException("读取主车已规划路径文件失败");
            } catch (NullPointerException v2) {
                throw new BusinessException("主车轨迹未规划");
            }
        }
        // 5.重复设备过滤
        taskCaseConfigs = taskCaseConfigs.stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(TaskCaseConfigBo::getDeviceId))), ArrayList::new));

        Map<Integer, TaskCaseInfoBo> taskCaseInfoMap = taskCaseInfos.stream().collect(Collectors.toMap(TaskCaseInfoBo::getCaseId, Function.identity()));
        // 6.状态查询
        for (TaskCaseConfigBo taskCaseConfigBo : taskCaseConfigs) {
            // 查询设备状态
            DeviceStateDto deviceStateDto = new DeviceStateDto();
            deviceStateDto.setDeviceId(taskCaseConfigBo.getDeviceId());
            deviceStateDto.setType(0);
            deviceStateDto.setTimestamp(System.currentTimeMillis());
            deviceStateSendService.sendData(taskCaseConfigBo.getCommandChannel(), deviceStateDto);
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam(taskCaseConfigBo.getDeviceId(), taskCaseConfigBo.getCommandChannel());
            if (PartRole.AV.equals(taskCaseConfigBo.getType())) {
                // av车需要主车全部轨迹
                stateParam.setParams(new ParamsDto(mainTrajectoryMap.get("main")));
            }
            if (PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getType())) {
                stateParam.setParams(buildTessStateParam(param.getTaskId(), taskCaseInfoMap, caseBusinessIdAndRoleMap, caseMainSize));
            }
            restService.selectDeviceReadyState(stateParam);
        }

    }

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
        TjTask tjTask = taskMapper.selectById(param.getTaskId());
        if (ObjectUtils.isEmpty(tjTask)) {
            throw new BusinessException("查询失败，请检查任务是否存在");
        }
        List<TaskCaseConfigBo> taskCaseConfigs = new ArrayList<>();
        Map<Integer, Map<String, String>> caseBusinessIdAndRoleMap = new HashMap<>();
        Map<Integer, Integer> caseMainSize = new HashMap<>();
        Map<String, String> startMap = new HashMap<>();
        Map<String, List<SimulationTrajectoryDto>> mainTrajectoryMap = new HashMap<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 2.数据校验
            validConfig(taskCaseInfoBo);
            // 3.轨迹详情(使用实车试验的点位配置)
            SceneTrajectoryBo trajectoryDetail = JSONObject.parseObject(taskCaseInfoBo.getRealDetailInfo(),
                    SceneTrajectoryBo.class);
            if (taskCaseInfoBo.getSort() == 1) {
                // 4.参与者开始点位
                startMap = CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories()).stream().collect(
                        Collectors.toMap(
                                ParticipantTrajectoryBo::getId,
                                item -> CollectionUtils.emptyIfNull(item.getTrajectory()).stream()
                                        .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                                        .orElse(new TrajectoryDetailBo()).getPosition()));
            }

            // 5.用例配置
            if (CollectionUtils.isNotEmpty(taskCaseInfoBo.getDataConfigs())) {
                taskCaseConfigs.addAll(taskCaseInfoBo.getDataConfigs());
            }
            // 6.参与者ID和参与者名称匹配map
            Map<String, String> businessIdAndRoleMap = taskCaseInfoBo.getDataConfigs().stream().collect(Collectors.toMap(
                    TaskCaseConfigBo::getParticipatorId,
                    TaskCaseConfigBo::getType));
            caseBusinessIdAndRoleMap.put(taskCaseInfoBo.getCaseId(), businessIdAndRoleMap);
            // 7.用例对应主车轨迹长度(使用实车试验的轨迹)
            List<RealTestTrajectoryDto> realTestTrajectoryDtos = routeService.readRealTrajectoryFromRouteFile(taskCaseInfoBo.getRealRouteFile());
            realTestTrajectoryDtos.stream().filter(RealTestTrajectoryDto::isMain).findFirst().ifPresent(t -> {
                if (!ObjectUtils.isEmpty(param.getId())) {
                    mainTrajectoryMap.put("main", t.getData());
                }
                caseMainSize.put(taskCaseInfoBo.getCaseId(), t.getData().size());
            });
        }
        // 5.重复设备过滤
        taskCaseConfigs = taskCaseConfigs.stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(TaskCaseConfigBo::getDeviceId))), ArrayList::new));
        // 6.查询主车轨迹
        if (ObjectUtils.isEmpty(param.getId())) {
            try {
                List<SimulationTrajectoryDto> main = routeService.readOriTrajectoryFromRouteFile(tjTask.getMainPlanFile(), "1");
                mainTrajectoryMap.put("main", main);
            } catch (IOException e) {
                log.error("主车规划路径文件读取失败：{}", e);
                throw new BusinessException("读取主车已规划路径文件失败");
            } catch (NullPointerException v2) {
                throw new BusinessException("主车轨迹未规划");
            }
        }
        Map<Integer, TaskCaseInfoBo> taskCaseInfoMap = taskCaseInfos.stream().collect(Collectors.toMap(TaskCaseInfoBo::getCaseId, Function.identity()));
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
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam(taskCaseConfigBo.getDeviceId(), taskCaseConfigBo.getCommandChannel());
            if (PartRole.AV.equals(taskCaseConfigBo.getType())) {
                // av车需要主车全部轨迹
                stateParam.setParams(new ParamsDto(mainTrajectoryMap.get("main")));
            }
            if (PartRole.MV_SIMULATION.equals(taskCaseConfigBo.getType())) {
                stateParam.setParams(buildTessStateParam(param.getTaskId(), taskCaseInfoMap, caseBusinessIdAndRoleMap, caseMainSize));
            }
            taskCaseConfigBo.setPositionStatus(deviceDetailService.selectDeviceReadyState(taskCaseConfigBo.getDeviceId(), stateParam, false));
        }
        Map<String, List<TaskCaseConfigBo>> statusMap = taskCaseConfigs.stream().collect(
                Collectors.groupingBy(TaskCaseConfigBo::getType));
        TaskCaseVerificationPageVo result = new TaskCaseVerificationPageVo();
        result.setTaskId(param.getTaskId());
        result.setTaskCaseId(param.getId());
        result.setStatusMap(statusMap);
        result.setChannels(taskCaseConfigs.stream().map(TaskCaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        validStatus(result);
        return result;
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
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCaseInfoBo.getRealDetailInfo(), SceneTrajectoryBo.class);
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
        tessParams.put("param1", param1);
        tessParams.put("taskId", taskId);
        return tessParams;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(TjTaskCase param) throws BusinessException {
        // 1.用例详情
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        List<Integer> resetList = new ArrayList<>();
        List<Integer> deleteIdList = new ArrayList<>();
        List<TjTaskCaseRecord> addList = new ArrayList<>();
        List<String> channels = new ArrayList<>();
        for (TaskCaseInfoBo taskCaseInfoBo : taskCaseInfos) {
            // 2.校验数据
            validConfig(taskCaseInfoBo);
            // 3.轨迹详情
            CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                    JSONObject.parseObject(taskCaseInfoBo.getRealDetailInfo(), CaseTrajectoryDetailBo.class);
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
            // 7.删除后新增任务用例测试记录
            TjTaskCaseRecord deleteRecord = new TjTaskCaseRecord();
            deleteRecord.setTaskId(taskCaseInfoBo.getTaskId());
            deleteRecord.setCaseId(taskCaseInfoBo.getCaseId());

            TjTaskCaseRecord caseRecord = taskCaseRecordService.getOne(new QueryWrapper<TjTaskCaseRecord>().eq(ColumnName.TASK_ID, taskCaseInfoBo.getTaskId())
                    .eq(ColumnName.CASE_ID_COLUMN, taskCaseInfoBo.getCaseId()).select("id"));
            if (caseRecord != null) {
                deleteIdList.add(caseRecord.getId());
            }

            TjTaskCaseRecord addRecord = new TjTaskCaseRecord();
            addRecord.setTaskId(taskCaseInfoBo.getTaskId());
            addRecord.setCaseId(taskCaseInfoBo.getCaseId());
            addRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            addRecord.setStatus(TestingStatusEnum.NO_PASS.getCode());
            addList.add(addRecord);

            channels.addAll(taskCaseInfoBo.getDataConfigs().stream().map(TaskCaseConfigBo::getDataChannel)
                    .collect(Collectors.toList()));

            resetList.add(taskCaseInfoBo.getId());
        }
        Optional.of(deleteIdList).filter(CollectionUtils::isNotEmpty).ifPresent(deleteIds -> taskCaseRecordService.removeByIds(deleteIds));
        taskCaseRecordService.saveBatch(addList);
        taskCaseMapper.reset(resetList);

        // 7.前端结果集
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setId(addList.get(0).getId());
        caseRealTestVo.setTaskId(param.getTaskId());
        caseRealTestVo.setTaskCaseId(param.getId());
        caseRealTestVo.setChannels(new HashSet<>(channels));
        return caseRealTestVo;
    }

    @Override
    public CaseRealTestVo controlTask(Integer taskId, Integer taskCaseId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录详情
        TjTaskCase param = new TjTaskCase();
        param.setTaskId(taskId);
        param.setId(taskCaseId);
        List<TaskCaseInfoBo> taskCaseInfos = taskCaseMapper.selectTaskCaseByCondition(param);
        if (CollectionUtils.isEmpty(taskCaseInfos)) {
            throw new BusinessException("任务用例不存在");
        }
        CaseTrajectoryParam caseTrajectoryParam = new CaseTrajectoryParam();
        Map<String, Object> context = new HashMap<>();

        for (TaskCaseInfoBo taskCaseInfo : taskCaseInfos) {
            for (TaskCaseConfigBo caseConfig : taskCaseInfo.getDataConfigs()) {
                if (caseConfig.getType().equals(PartRole.AV)) {
                    context.put("dataChannel", caseConfig.getDataChannel());
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
            if (StringUtils.isEmpty(taskCase.getRealDetailInfo())) {
                throw new BusinessException(StringUtils.format("用例{}轨迹不存在", taskCase.getCaseNumber()));
            }
            SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(taskCase.getRealDetailInfo(), SceneTrajectoryBo.class);
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
        // 开始监听所有数据通道
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(taskId),
                WebSocketManage.TASK, null);
        context.put("key", key);
        taskRedisTrajectoryConsumer.subscribeAndSend(key, taskId, taskCaseId, taskCaseInfos);
        caseTrajectoryParam.setContext(context);
        // 向主控发送主车信息
        restService.sendCaseTrajectoryInfo(caseTrajectoryParam);

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
    public CaseRealTestVo gettaskInfo(Integer taskId) throws BusinessException {
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(taskId),
                WebSocketManage.TASK, null);
        Integer caseId = taskRedisTrajectoryConsumer.getRunningCase(key);
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        caseRealTestVo.setTaskId(taskId);
        caseRealTestVo.setTaskCaseId(caseId);
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        caseRealTestVo.setTestTypeName(caseInfoBo.getTestScene());
        return caseRealTestVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo caseStartEnd(Integer taskId, Integer caseId, Integer action, boolean taskEnd, Map<String, Object> context) throws BusinessException, IOException {
        log.info("任务{} 用例{} ,action:{}, 上下文：{}, 任务终止:{}", taskId, caseId, action, JSONObject.toJSONString(context), taskEnd);
        // 1.任务用例测试记录详情
        TjTaskCaseRecord taskCaseRecord = ssGetTjTaskCaseRecord(taskId, caseId, action);
        // 2.任务用例详情
        TjTaskCase taskCase = getTjTaskCaseByTaskCaseRecord(taskId, caseId);
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseByCondition(taskCase).get(0);
        // 3.校验数据
        validConfig(taskCaseInfoBo);
        // 4.更新业务数据
        ssCaseResultUpdate(action, taskCaseRecord, taskCase);
        // 5.记录redis中正在运行的用例
        if (1 == action) {
            taskRedisTrajectoryConsumer.updateRunningCase(String.valueOf(context.get("key")), caseId);
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("taskEnd", taskEnd);
            jsonObject.put("action", action);
            SimulationMessage endMsg = new SimulationMessage(RedisMessageType.END, jsonObject);
            noClassRedisTemplate.convertAndSend(String.valueOf(context.get("dataChannel")), endMsg);
        }
        // 6.向主控发送控制请求
        Optional<TaskCaseConfigBo> first = taskCaseInfoBo.getDataConfigs()
                .stream().filter(e -> PartRole.AV.equals(e.getType())).findFirst();
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(),
                        String.valueOf(taskCaseInfoBo.getTaskId()), action > 0 ? action : 0,
                        generateDeviceConnRules(taskCaseInfoBo),
                        first.get().getCommandChannel(), taskEnd))) {
            throw new BusinessException("主控响应异常");
        }

        // 7.前端结果集
        return ssGetCaseRealTestVo(taskCaseRecord);
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        // 1.任务用例测试记录
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        // 2.数据校验
        if (ObjectUtils.isEmpty(taskCaseRecord)) {
            throw new BusinessException("未查询到任务用例测试记录");
        }
        if (StringUtils.isEmpty(taskCaseRecord.getRouteFile())) {
            throw new BusinessException("未查询到可使用轨迹文件，请先进行试验");
        }
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
        TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseInfo(taskCase.getId());
        if (ObjectUtils.isEmpty(taskCaseInfoBo) || CollectionUtils.isEmpty(taskCaseInfoBo.getDataConfigs())
                || taskCaseInfoBo.getDataConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("未进行设备配置");
        }
        // 点位
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // av类型设备配置
        List<TaskCaseConfigBo> avConfigs = taskCaseInfoBo.getDataConfigs().stream().filter(item -> PartRole.AV.equals(item.getSupportRoles())).collect(Collectors.toList());
        // 主车配置
        TaskCaseConfigBo caseConfigBo = avConfigs.get(0);
        // av类型通道和业务车辆ID映射
        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
                TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorId));
        // av类型通道和业务车辆名称映射
        Map<String, String> avChannelAndNameMap = taskCaseInfoBo.getDataConfigs().stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
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
        // todo 读取仿真验证主车轨迹
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
                        realTestTrajectoryDto.setId(avChannelAndBusinessIdMap.get(realTestTrajectoryDto.getChannel()));
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
    public RealTestResultVo getResult(Integer taskId, Integer id) throws BusinessException {
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(id);
        if (ObjectUtils.isEmpty(taskCaseRecord) || ObjectUtils.isEmpty(taskCaseRecord.getDetailInfo())) {
            throw new BusinessException("待开始测试");
        }
        TjTask tjTask = taskMapper.selectById(taskCaseRecord.getTaskId());
        if (ObjectUtils.isEmpty(tjTask)) {
            throw new BusinessException("未查询到任务信息");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
                .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
        caseTrajectoryDetailBo.setParticipantTrajectories(trajectoryBos);
        RealTestResultVo realTestResultVo = new RealTestResultVo();
        BeanUtils.copyProperties(caseTrajectoryDetailBo, realTestResultVo);
        realTestResultVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, tjTask.getTestType()));
        realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
        realTestResultVo.setId(taskCaseRecord.getId());
        realTestResultVo.setStartTime(taskCaseRecord.getStartTime());
        realTestResultVo.setEndTime(taskCaseRecord.getEndTime());
        return realTestResultVo;
    }

//    @Override
//    public RealTestResultVo getResult(Integer taskId, Integer id) throws BusinessException {
//        RealTestResultVo realTestResultVo = new RealTestResultVo();
//        if (ObjectUtils.isEmpty(id)) {
//            TjTask tjTask = taskMapper.selectById(taskId);
////            TjTaskCase taskCase = taskCaseMapper.selectOne(new QueryWrapper<TjTaskCase>().eq(ColumnName.TASK_ID, taskId));
//
//            List<TjTaskCaseRecord> taskCaseRecords = taskCaseRecordMapper.selectList(
//                    new QueryWrapper<TjTaskCaseRecord>().eq(ColumnName.TASK_ID, taskId));
//            if (ObjectUtils.isEmpty(taskCaseRecords)) {
//                throw new BusinessException("未查询到任务测试记录");
//            }
//            List<ParticipantTrajectoryBo> participantTrajectoryBos = new ArrayList<>();
//            for (TjTaskCaseRecord taskCaseRecord : taskCaseRecords) {
//                CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
//                        CaseTrajectoryDetailBo.class);
//                if (StringUtils.isEmpty(realTestResultVo.getSceneDesc())) {
//                    realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
//                }
//                if (StringUtils.isEmpty(realTestResultVo.getSceneDesc())) {
//                    realTestResultVo.setSceneDesc(caseTrajectoryDetailBo.getSceneDesc());
//                }
//                List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
//                        .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
//                participantTrajectoryBos.addAll(trajectoryBos);
//            }
//            realTestResultVo.setParticipantTrajectories(participantTrajectoryBos);
//            realTestResultVo.setStartTime(DateUtils.dateToLDT(tjTask.getStartTime()));
//            realTestResultVo.setEndTime(DateUtils.dateToLDT(tjTask.getEndTime()));
//
//
//        } else {
//
//
//            TjTaskCase taskCase = getById(id);
//
//            TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectOne(
//                    new QueryWrapper<TjTaskCaseRecord>()
//                            .eq(ColumnName.TASK_ID, taskId)
//                            .eq(ColumnName.CASE_ID_COLUMN, taskCase.getCaseId()));
//            if (ObjectUtils.isEmpty(taskCaseRecord) || ObjectUtils.isEmpty(taskCaseRecord.getDetailInfo())) {
//                throw new BusinessException("待开始测试");
//            }
//            if (TestingStatus.FINISHED > taskCaseRecord.getStatus()) {
//                return null;
//            }
//            CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
//                    CaseTrajectoryDetailBo.class);
//            List<ParticipantTrajectoryBo> trajectoryBos = caseTrajectoryDetailBo.getParticipantTrajectories().stream()
//                    .filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toList());
//            caseTrajectoryDetailBo.setParticipantTrajectories(trajectoryBos);
//
//            BeanUtils.copyProperties(caseTrajectoryDetailBo, realTestResultVo);
//            realTestResultVo.setSceneName(caseTrajectoryDetailBo.getSceneDesc());
//            realTestResultVo.setId(id);
//            realTestResultVo.setStartTime(taskCaseRecord.getStartTime());
//            realTestResultVo.setEndTime(taskCaseRecord.getEndTime());
//        }
//        realTestResultVo.setTaskId(taskId);
//        return realTestResultVo;
//    }

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
        TaskCaseInfoBo taskCaseInfoBo = taskCaseMapper.selectTaskCaseByCondition(taskCase).get(0);

        Optional<TaskCaseConfigBo> first = taskCaseInfoBo.getDataConfigs()
                .stream().filter(e -> PartRole.AV.equals(e.getType())).findFirst();
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(),
                        String.valueOf(taskId), 0,
                        generateDeviceConnRules(taskCaseInfoBo),
                        first.get().getCommandChannel(), true))) {
            throw new BusinessException("主控响应异常");
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
            treeVo.setNumber(caseCountMap.get(treeVo.getId()).intValue());
        }
        return caseTree;
    }

    @Override
    public boolean addTaskCase(@NotNull Integer taskId, @NotNull Integer caseId) {
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        taskCase.setCaseId(caseId);
        taskCase.setSort(0);
        taskCase.setCreateTime(new Date());
        taskCase.setStatus(TaskCaseStatusEnum.WAITING.getCode());
        return save(taskCase);
    }

    @Override
    public boolean deleteTaskCase(@NotNull Integer taskId, @NotNull Integer caseId) {
        return remove(new QueryWrapper<TjTaskCase>().eq(ColumnName.TASK_ID, taskId)
                .eq(ColumnName.CASE_ID_COLUMN, caseId));
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
        if (StringUtils.isEmpty(taskCaseInfoBo.getRealDetailInfo())) {
            throw new BusinessException("用例异常：无路径配置信息");
        }
        if (StringUtils.isEmpty(taskCaseInfoBo.getRealRouteFile())) {
            throw new BusinessException("用例异常：未进行实车试验");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(TaskCaseInfoBo caseInfoBo) {
        List<TaskCaseConfigBo> caseConfigs = caseInfoBo.getDataConfigs().stream().filter(config ->
                !ObjectUtils.isEmpty(config.getDeviceId())).collect(Collectors.toList());

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

    private TjTaskCaseRecord ssGetTjTaskCaseRecord(Integer taskId, Integer caseId, Integer action)
            throws BusinessException {
        QueryWrapper<TjTaskCaseRecord> recordQueryWrapper = new QueryWrapper<>();
        recordQueryWrapper.eq(ColumnName.TASK_ID, taskId).eq(ColumnName.CASE_ID_COLUMN, caseId);
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectOne(recordQueryWrapper);
        if (ObjectUtils.isEmpty(taskCaseRecord)) {
            throw new BusinessException("未找到用例测试记录");
        }
        return taskCaseRecord;
    }

    private TjTaskCase getTjTaskCaseByTaskCaseRecord(Integer taskId, Integer caseId) {
        QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId).eq(ColumnName.TASK_ID, taskId);
        return taskCaseMapper.selectOne(queryWrapper);
    }

    private void ssCaseResultUpdate(Integer action, TjTaskCaseRecord taskCaseRecord,
                                    TjTaskCase taskCase) {
        if (1 == action) {
            taskCaseRecord.setStartTime(LocalDateTime.now());

            Date date = new Date();
            taskCase.setStartTime(date);
            taskCase.setStatus(TaskCaseStatusEnum.RUNNING.getCode());

            TjTask tjTask = taskMapper.selectById(taskCaseRecord.getTaskId());
            if (ObjectUtils.isEmpty(tjTask.getStartTime())) {
                tjTask.setStartTime(date);
                taskMapper.updateById(tjTask);
            }
            taskCaseRecordMapper.updateById(taskCaseRecord);
            taskCaseMapper.updateById(taskCase);
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
}




