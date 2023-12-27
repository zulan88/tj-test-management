package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseSSInfo;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.param.TessParam;
import net.wanji.business.domain.vo.CaseTestPrepareVo;
import net.wanji.business.domain.vo.CaseTestStartVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.schedule.RealPlaybackSchedule;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TestingService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceDetailService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StopWatch;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 10:00
 * @Descriptoin:
 */
@Slf4j
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
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Autowired
    private KafkaCollector kafkaCollector;

    @Override
    public RealVehicleVerificationPageVo getStatus(Integer caseId, boolean hand) throws BusinessException {
        StopWatch stopWatch = new StopWatch("实车试验 - 轮询状态");
        stopWatch.start("1.校验用例详情并填充数据");
        // 1.查询用例详情并校验
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        validConfig(caseInfoBo);
        stopWatch.stop();

        stopWatch.start("2.查询状态");
        // 2.数据填充
        List<CaseConfigBo> caseConfigs = new ArrayList<>();
        Map<String, String> caseBusinessIdAndRoleMap = new HashMap<>();
        Map<String, String> startMap = new HashMap<>();
        List<SimulationTrajectoryDto> mainTrajectories = new ArrayList<>();
        fillStatusParam(caseInfoBo, caseConfigs, caseBusinessIdAndRoleMap, startMap, mainTrajectories);
        // 3.设备过滤
        caseConfigs = filterConfigs(caseConfigs);
        CaseConfigBo simulationConfig = caseConfigs.stream()
                .filter(t -> PartRole.MV_SIMULATION.equals(t.getParticipantRole()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到仿真设备"));
        if (hand) {
            // 先停止
            stop(caseId);
            // 4.唤醒仿真服务
            if (!restService.startServer(simulationConfig.getIp(), Integer.valueOf(simulationConfig.getServiceAddress()),
                    buildTessServerParam(1, caseInfoBo.getCreatedBy(), caseId))) {
                throw new BusinessException("唤起仿真服务失败");
            }
        }


        // 5.状态查询
        for (CaseConfigBo caseConfigBo : caseConfigs) {
            // 查询设备状态
            Integer status = hand
                    ? deviceDetailService.handDeviceState(caseConfigBo.getDeviceId(), getCommandChannelByRole(caseConfigBo), false)
                    : deviceDetailService.selectDeviceState(caseConfigBo.getDeviceId(), getCommandChannelByRole(caseConfigBo), false);
            caseConfigBo.setStatus(status);
            // 查询设备准备状态
            DeviceReadyStateParam stateParam = new DeviceReadyStateParam(caseConfigBo.getDeviceId(), getCommandChannelByRole(caseConfigBo));
            if (PartRole.AV.equals(caseConfigBo.getParticipantRole())) {
                stateParam.setParams(new ParamsDto("1", mainTrajectories));
            }
            if (PartRole.MV_SIMULATION.equals(caseConfigBo.getParticipantRole())) {
                stateParam.setParams(buildTessStateParam(caseInfoBo, caseBusinessIdAndRoleMap, mainTrajectories.size()));
            }
            Integer positionStatus = hand
                    ? deviceDetailService.handDeviceReadyState(caseConfigBo.getDeviceId(), getReadyStatusChannelByRole(caseConfigBo), stateParam, false)
                    : deviceDetailService.selectDeviceReadyState(caseConfigBo.getDeviceId(), getReadyStatusChannelByRole(caseConfigBo), stateParam, false);
            caseConfigBo.setPositionStatus(positionStatus);
        }
        stopWatch.stop();
//        log.info(stopWatch.prettyPrint());
        // 6.返回结果集
        return buildPageVo(caseInfoBo, startMap, caseConfigs);
    }

    /**
     * 根据角色获取指令通道
     *
     * @param caseConfigBo
     * @return
     */
    private String getCommandChannelByRole(CaseConfigBo caseConfigBo) {
        return PartRole.MV_SIMULATION.equals(caseConfigBo.getSupportRoles())
                ? ChannelBuilder.buildTestingControlChannel(SecurityUtils.getUsername(), caseConfigBo.getCaseId())
                : caseConfigBo.getCommandChannel();
    }

    /**
     * 根据角色获取准备状态通道
     *
     * @param caseConfigBo
     * @return
     */
    private String getReadyStatusChannelByRole(CaseConfigBo caseConfigBo) {
        return PartRole.MV_SIMULATION.equals(caseConfigBo.getSupportRoles())
                ? ChannelBuilder.buildTestingStatusChannel(SecurityUtils.getUsername(), caseConfigBo.getCaseId())
                : ChannelBuilder.DEFAULT_STATUS_CHANNEL;
    }

    /**
     * 构建唤醒tess服务的参数
     *
     * @param roadNum
     * @param caseId
     * @return
     */
    private TessParam buildTessServerParam(Integer roadNum, String username, Integer caseId) {
        return new TessParam().buildRealTestParam(roadNum,
                ChannelBuilder.buildTestingDataChannel(username, caseId),
                ChannelBuilder.buildTestingControlChannel(username, caseId),
                ChannelBuilder.buildTestingEvaluateChannel(username, caseId),
                ChannelBuilder.buildTestingStatusChannel(username, caseId));
    }

    /**
     * 构建页面结果集数据
     *
     * @param caseInfoBo
     * @param startMap
     * @param caseConfigs
     * @return
     */
    private RealVehicleVerificationPageVo buildPageVo(CaseInfoBo caseInfoBo,
                                                      Map<String, String> startMap,
                                                      List<CaseConfigBo> caseConfigs) {
        for (CaseConfigBo caseConfigBo : caseConfigs) {
            String start = startMap.get(caseConfigBo.getBusinessId());
            if (StringUtils.isNotEmpty(start)) {
                String[] position = start.split(",");
                caseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
                caseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
            }
        }
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setCaseId(caseInfoBo.getId());
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(caseConfigs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole)));
        result.setChannels(caseConfigs.stream().map(CaseConfigBo::getDataChannel).collect(Collectors.toSet()));
        result.setMessage(validStatus(caseConfigs));
        return result;
    }


    /**
     * 筛选重复设备
     *
     * @param caseConfigs
     * @return
     */
    private List<CaseConfigBo> filterConfigs(List<CaseConfigBo> caseConfigs) {
        return caseConfigs.stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.collectingAndThen(
                Collectors.toCollection(() ->
                        new TreeSet<>(Comparator.comparing(CaseConfigBo::getDeviceId))), ArrayList::new));
    }

    /**
     * 获取状态接口的数据填充
     *
     * @param caseInfoBo
     * @param caseConfigs
     * @param caseBusinessIdAndRoleMap
     * @param startMap
     * @param mainTrajectories
     * @throws BusinessException
     */
    private void fillStatusParam(CaseInfoBo caseInfoBo,
                                 List<CaseConfigBo> caseConfigs,
                                 Map<String, String> caseBusinessIdAndRoleMap,
                                 Map<String, String> startMap,
                                 List<SimulationTrajectoryDto> mainTrajectories) throws BusinessException {
        // 1..参与者开始点位
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        for (ParticipantTrajectoryBo trajectoryBo : CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories())) {
            startMap.put(trajectoryBo.getId(), CollectionUtils.emptyIfNull(trajectoryBo.getTrajectory()).stream()
                    .filter(t -> PointTypeEnum.START.getPointType().equals(t.getType())).findFirst()
                    .orElse(new TrajectoryDetailBo()).getPosition());
        }
        // 2.用例配置和参与者ID和参与者名称匹配map
        for (CaseConfigBo caseConfig : CollectionUtils.emptyIfNull(caseInfoBo.getCaseConfigs())) {
            caseConfigs.add(caseConfig);
            caseBusinessIdAndRoleMap.put(caseConfig.getBusinessId(), caseConfig.getParticipantRole());
        }
        // 3.主车轨迹 -- av车需要主车全部轨迹
        List<SimulationTrajectoryDto> participantTrajectories = null;
        try {
            participantTrajectories = routeService.readOriRouteFile(caseInfoBo.getRouteFile());
            participantTrajectories = participantTrajectories.stream()
                    .filter(item -> !ObjectUtils.isEmpty(item.getValue())
                            && item.getValue().stream().anyMatch(p -> "1".equals(p.getId())))
                    .peek(s -> s.setValue(s.getValue().stream().filter(p -> "1".equals(p.getId()))
                            .collect(Collectors.toList())))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("查询主车轨迹失败");
        }
        if (CollectionUtils.isEmpty(participantTrajectories)) {
            throw new BusinessException("查询主车轨迹失败");
        }
        mainTrajectories.addAll(participantTrajectories);
    }

    /**
     * 创建tess准备状态参数
     *
     * @param caseInfoBo
     * @param caseBusinessIdAndRoleMap
     * @param mainSize
     * @return
     */
    private Map<String, Object> buildTessStateParam(CaseInfoBo caseInfoBo,
                                                    Map<String, String> caseBusinessIdAndRoleMap,
                                                    Integer mainSize) {
        Map<String, Object> tessParams = new HashMap<>();
        // gdj edit start 2023-11-17
        List<Map<String, Object>> param1 = new ArrayList<>();
        Map<String, Object> mapParam1 = new HashMap<>();
        mapParam1.put("caseId", caseInfoBo.getId());
        mapParam1.put("sort", 1);
        mapParam1.put("avPassTime", mainSize);
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
        SceneTrajectoryBo trajectoryBo = JSONObject.parseObject(caseInfoBo.getDetailInfo(), SceneTrajectoryBo.class);
        for (ParticipantTrajectoryBo participantTrajectory : trajectoryBo.getParticipantTrajectories()) {
            if (PartType.MAIN.equals(participantTrajectory.getType())) {
                tessParams.put("avId", participantTrajectory.getId());
                tessParams.put("avName", participantTrajectory.getName());
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("role", caseBusinessIdAndRoleMap.get(participantTrajectory.getId()));
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
        tessParams.put("taskId", 0);

        return tessParams;
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
        // 6.删除空记录后新增实车测试记录
        caseRealRecordMapper.delete(new LambdaQueryWrapper<TjCaseRealRecord>()
                .eq(TjCaseRealRecord::getCaseId, caseId)
                .isNull(TjCaseRealRecord::getRouteFile));

        TjCaseRealRecord tjCaseRealRecord = new TjCaseRealRecord();
        tjCaseRealRecord.setCaseId(caseId);
        tjCaseRealRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjCaseRealRecord.setStatus(TestingStatusEnum.NO_PASS.getCode());
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
    public CaseTestStartVo start(Integer caseId, Integer action, String username) throws BusinessException, IOException {
        StopWatch stopWatch = new StopWatch(StringUtils.format("开始实车试验 - 用例ID:{}", caseId));
        stopWatch.start("1.查询、校验用例详情");
        log.info("{} 开始实车试验 用例 ：{}", username, caseId);
        // 1.用例详情
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        validConfig(caseInfoBo);
        stopWatch.stop();

        stopWatch.start("2.向主控发送规则");
        // 2.向主控发送规则
        CaseConfigBo mainConfig = caseInfoBo.getCaseConfigs().stream().filter(t ->
                PartRole.AV.equals(t.getParticipantRole())).findFirst().orElseThrow(() ->
                new BusinessException("用例主车配置信息异常"));
        TessParam tessParam = buildTessServerParam(1, username, caseId);
        if (!restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(),
                0, caseId, action,
                generateDeviceConnRules(caseInfoBo, tessParam.getCommandChannel(), tessParam.getDataChannel()),
                mainConfig.getCommandChannel(), false))) {
            throw new BusinessException("主控响应异常");
        }
        stopWatch.stop();

        stopWatch.start("3.更新业务数据，构建结果集");
        // 3.更新业务数据
        TjCaseRealRecord realRecord = caseInfoBo.getCaseRealRecord();
        realRecord.setStartTime(LocalDateTime.now());
        caseRealRecordMapper.updateById(realRecord);
        // 4.前端结果集
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
        stopWatch.stop();
        log.info("耗时：{}", stopWatch.prettyPrint());
        return startVo;
    }

    @Override
    public void end(Integer caseId, int action, String username) throws BusinessException {
        StopWatch stopWatch = new StopWatch(StringUtils.format("结束实车试验 - 用例ID:{}", caseId));
        stopWatch.start("1.向主控发送结束控制请求");
        log.info("{} 结束实车试验 用例 ：{}", username, caseId);
        // 向主控发送控制请求
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        validConfig(caseInfoBo);
        CaseConfigBo mainConfig = caseInfoBo.getCaseConfigs().stream().filter(t ->
                PartRole.AV.equals(t.getParticipantRole())).findFirst().orElseThrow(() ->
                new BusinessException("用例主车配置信息异常"));
        TessParam tessParam = buildTessServerParam(1, username, caseId);
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(), 0, caseId, action,
                        generateDeviceConnRules(caseInfoBo, tessParam.getCommandChannel(), tessParam.getDataChannel()),
                        mainConfig.getCommandChannel(), true))) {
            throw new BusinessException("主控响应异常");
        }
        stopWatch.stop();

        stopWatch.start("2.保存实车测试记录点位详情信息");
        String key = ChannelBuilder.buildTestingDataChannel(caseInfoBo.getCreatedBy(), caseId);
        List<List<SimulationTrajectoryDto>> trajectories = kafkaCollector.take(key, caseId);
        try {
            routeService.saveRealRouteFile2(caseInfoBo.getCaseRealRecord(), action, trajectories);
        } catch (Exception e) {
            log.error("保存实车测试记录点位详情信息异常:{}", e);
        } finally {
            kafkaCollector.remove(key);
        }
        String duration = DateUtils.secondsToDuration((int) Math.floor(
                (double) (CollectionUtils.isEmpty(trajectories) ? 0 : trajectories.size()) / 10));
        RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null, null, duration);
        WebSocketManage.sendInfo(key, JSON.toJSONString(endMsg));

        stopWatch.stop();
        log.info("耗时：{}", stopWatch.prettyPrint());
    }

    @Override
    public CaseTestStartVo controlTask(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        validConfig(caseInfoBo);
        TjCaseRealRecord realRecord = caseInfoBo.getCaseRealRecord();
        if (ObjectUtils.isEmpty(realRecord)) {
            throw new BusinessException("未查询到测试记录");
        }
        CaseTrajectoryParam caseTrajectoryParam = new CaseTrajectoryParam();
        caseTrajectoryParam.setTaskId(0);
        caseTrajectoryParam.setCaseId(caseId);
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(realRecord.getDetailInfo(),
                SceneTrajectoryBo.class);

        sceneTrajectoryBo.getParticipantTrajectories().stream().filter(p -> PartType.MAIN.equals(p.getType())).findFirst().ifPresent(f -> {
            CaseSSInfo caseSSInfo = new CaseSSInfo();
            caseSSInfo.setCaseId(caseId);
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
        });

        caseInfoBo.getCaseConfigs().stream().filter(t -> PartRole.AV.equals(t.getSupportRoles())).findFirst().ifPresent(t -> {
            caseTrajectoryParam.setDataChannel(t.getDataChannel());
        });
        String key = ChannelBuilder.buildTestingDataChannel(SecurityUtils.getUsername(), caseId);
        kafkaCollector.remove(key);

        Map<String, Object> context = new HashMap<>();
        context.put("username", SecurityUtils.getUsername());
        caseTrajectoryParam.setContext(context);
        restService.sendCaseTrajectoryInfo(caseTrajectoryParam);
        CaseTestStartVo startVo = new CaseTestStartVo();
        BeanUtils.copyProperties(realRecord, startVo);
        startVo.setStartTime(DateUtils.getTime());
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        startVo.setMainTrajectories(mainTrajectoryMap);
        startVo.setTestTypeName(caseInfoBo.getTestScene());
        startVo.setCaseId(caseId);
        return startVo;
    }

    @Override
    public CaseTestStartVo hjktest(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        CaseTestStartVo startVo = new CaseTestStartVo();
        startVo.setTestTypeName(caseInfoBo.getTestScene());
        startVo.setCaseId(caseId);
        return startVo;
    }

    @Override
    public void stop(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseId);
        Optional<CaseConfigBo> avConfig = caseInfoBo.getCaseConfigs().stream().filter(t -> PartRole.AV.equals(t.getParticipantRole())).findFirst();
        if (!avConfig.isPresent()) {
            throw new BusinessException("未查询到主车配置信息");
        }
        String commandChannel = avConfig.get().getCommandChannel();
        TessParam tessParam = buildTessServerParam(1, caseInfoBo.getCreatedBy(), caseId);
        if (!restService.sendRuleUrl(
                new CaseRuleControl(System.currentTimeMillis(), 0, caseId, 0,
                        generateDeviceConnRules(caseInfoBo, tessParam.getCommandChannel(), tessParam.getDataChannel()),
                        commandChannel, true))) {
            throw new BusinessException("主控响应异常");
        }
    }

    @Override
    public void playback(Integer recordId, Integer action) throws BusinessException, IOException {
        // 1.实车测试记录
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        // 2.数据校验
        if (ObjectUtils.isEmpty(caseRealRecord)) {
            throw new BusinessException("未查询到试验记录");
        }
        if (StringUtils.isEmpty(caseRealRecord.getRouteFile())) {
            throw new BusinessException("无完整试验记录");
        }
        CaseInfoBo caseInfoBo = caseService.getCaseDetail(caseRealRecord.getCaseId());
        if (ObjectUtils.isEmpty(caseInfoBo) || CollectionUtils.isEmpty(caseInfoBo.getCaseConfigs())
                || caseInfoBo.getCaseConfigs().stream().allMatch(config -> ObjectUtils.isEmpty(config.getDeviceId()))) {
            throw new BusinessException("未进行设备配置");
        }
//        // 点位
//        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
//                CaseTrajectoryDetailBo.class);
//        // 设备配置
//        List<CaseConfigBo> configBos = caseInfoBo.getCaseConfigs();
//        // av类型设备配置
        Optional<CaseConfigBo> caseConfigOptional = caseInfoBo.getCaseConfigs().stream()
                .filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .findFirst();
        if (!caseConfigOptional.isPresent()) {
            throw new BusinessException("未查询到对应记录的主车配置");
        }
//        // 主车配置
        CaseConfigBo mainConfigBo = caseConfigOptional.get();
//        // av类型通道和业务车辆ID映射
//        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
//                CaseConfigBo::getDataChannel, CaseConfigBo::getBusinessId));
//        // av类型通道和业务车辆名称映射
//        Map<String, String> avChannelAndNameMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
//                .collect(Collectors.toMap(CaseConfigBo::getDataChannel, CaseConfigBo::getName));
//        // 主车点位映射
//        Map<String, List<TrajectoryDetailBo>> avBusinessIdPointsMap = originalTrajectory.getParticipantTrajectories()
//                .stream().filter(item ->
//                        avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
//                        ParticipantTrajectoryBo::getId,
//                        ParticipantTrajectoryBo::getTrajectory
//                ));
//        // 主车全部点位
//        List<TrajectoryDetailBo> avPoints = avBusinessIdPointsMap.get(caseConfigBo.getBusinessId());
        // 读取仿真验证主车轨迹
//        TjCase tjCase = caseMapper.selectById(caseRealRecord.getCaseId());
//        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(tjCase.getRouteFile(),
//                caseConfigBo.getBusinessId());
        String key = ChannelBuilder.buildTestingPreviewChannel(SecurityUtils.getUsername(), recordId);
        switch (action) {
            case PlaybackAction.START:
                List<List<SimulationTrajectoryDto>> trajectories = routeService.readRealTrajectoryFromRouteFile2(caseRealRecord.getRouteFile());
                RealPlaybackSchedule.startSendingData(key, mainConfigBo.getDataChannel(), trajectories);
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

    private List<DeviceConnRule> generateDeviceConnRules(CaseInfoBo caseInfoBo, String commandChannel, String dataChannel) {
        List<CaseConfigBo> configs = filterConfigs(caseInfoBo.getCaseConfigs());
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
                rule.setSource(createConnInfo(sourceDevice, commandChannel, dataChannel, sourceParams));
                rule.setTarget(createConnInfo(targetDevice, commandChannel, dataChannel, targetParams));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(CaseConfigBo config, String commandChannel, String dataChannel,
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
}
