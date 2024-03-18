package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants;
import net.wanji.business.common.DeviceStatus;
import net.wanji.business.domain.InfiniteTessParm;
import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.domain.SiteSlice;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.InfinityReadyDto;
import net.wanji.business.domain.dto.device.ParamsDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseSSInfo;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.TessParam;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.domain.vo.task.infinity.DeviceInfo;
import net.wanji.business.domain.vo.task.infinity.InfinityTaskInitVo;
import net.wanji.business.domain.vo.task.infinity.InfinityTaskPreparedVo;
import net.wanji.business.domain.vo.task.infinity.ShardingInfoVo;
import net.wanji.business.entity.TjDeviceDetail;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.infity.TjInfinityTask;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjInfinityMapper;
import net.wanji.business.service.*;
import net.wanji.business.util.DeviceUtils;
import net.wanji.business.util.RedisChannelUtils;
import net.wanji.business.util.RedisLock;
import net.wanji.business.util.TessngUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hcy
 * @version 1.0
 * @className TjInfinityTaskServiceImpl
 * @description TODO
 * @date 2024/3/11 13:12
 **/
@Service
public class TjInfinityTaskServiceImpl extends ServiceImpl<TjInfinityMapper, TjInfinityTask> implements TjInfinityTaskService {

    private final TjTaskDataConfigService tjTaskDataConfigService;
    private final InfinteMileScenceService infinteMileScenceService;
    private final TjDeviceDetailService tjDeviceDetailService;
    private final RouteService routeService;
    private final RestService restService;
    private final RedisLock redisLock;
    private final KafkaCollector kafkaCollector;
    @Resource
    private TjInfinityMapper tjInfinityMapper;

    @Value("${tess.testReportOuterChain}")
    private String testReportOuterChain;

    public TjInfinityTaskServiceImpl(
        TjTaskDataConfigService tjTaskDataConfigService,
        InfinteMileScenceService infinteMileScenceService,
        TjDeviceDetailService tjDeviceDetailService, RouteService routeService,
        RestService restService, RedisLock redisLock,
        KafkaCollector kafkaCollector) {
        this.tjTaskDataConfigService = tjTaskDataConfigService;
        this.infinteMileScenceService = infinteMileScenceService;
        this.tjDeviceDetailService = tjDeviceDetailService;
        this.routeService = routeService;
        this.restService = restService;
        this.redisLock = redisLock;
        this.kafkaCollector = kafkaCollector;
    }

    @Override
    public Map<String, Long> selectCount(TaskDto taskDto) {
        List<Map<String, String>> statusMaps = tjInfinityMapper.selectCountByStatus(taskDto);
        Map<String, Long> statusCountMap = CollectionUtils.emptyIfNull(statusMaps).stream().map(t -> t.get("status")).collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        Map<String, Long> result = new HashMap<>();
        for (String status : Constants.TaskStatusEnum.getPageCountList()) {
            result.put(status, statusCountMap.getOrDefault(status, 0L));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> pageList(TaskDto in) {

        List<Map<String, Object>> pageList = tjInfinityMapper.getPageList(in);
        for (Map<String, Object> task : pageList) {
            String id = task.get("id").toString();
            List<TjTaskDataConfig> list = tjTaskDataConfigService.list(new QueryWrapper<TjTaskDataConfig>().eq("task_id", id));
            task.put("avDeviceIds", list);

            // TODO 任务历史记录
            List<Map<String, Object>> historyRecords = new ArrayList<>();
            historyRecords.add(new HashMap<String, Object>() {{
                put("taskStartTime", "2024-03-15 00:00:00");
                put("taskRunningTime", "00:15:24");
                put("record", "1");
            }});

            historyRecords.add(new HashMap<String, Object>() {{
                put("taskStartTime", "2024-03-15 00:00:00");
                put("taskRunningTime", "00:15:24");
                put("record", "2");
            }});

            task.put("historyRecords", historyRecords);
        }


        return pageList;
    }

    @Override
    public List<CasePageVo> getTaskCaseList(Integer taskId) {
        return null;
    }

    @Override
    public String getTestReportOuterChain(HttpServletRequest request) {
        return StringUtils.isEmpty(testReportOuterChain) ? "" : testReportOuterChain;
    }

    @Override
    public int saveTask(Map<String, Object> task) throws BusinessException {
        String caseId = task.get("caseId").toString();
        tjInfinityMapper.saveTask(task);
        int id = Integer.parseInt(task.get("id").toString());

        // 保存参与者数据
        List<Map<String, Object>> configList = (List<Map<String, Object>>) task.get("avDeviceIds");
        for (Map<String, Object> configMap : configList) {
            TjTaskDataConfig newAvConfig = new TjTaskDataConfig();
            newAvConfig.setDeviceId(Integer.parseInt(configMap.get("deviceId").toString()));
            newAvConfig.setType(configMap.get("type").toString());
            newAvConfig.setParticipatorId(configMap.get("participatorId").toString());
            newAvConfig.setParticipatorName(configMap.get("participatorName").toString());
            newAvConfig.setTaskId(id);
            newAvConfig.setCaseId(Integer.parseInt(caseId));
            tjTaskDataConfigService.save(newAvConfig);
        }
        // 默认添加仿真车（tessng）
        addDefaultTessng(id, 0);

        return id;
    }

    private void addDefaultTessng(Integer taskId, Integer caseId)
        throws BusinessException {
        QueryWrapper<TjDeviceDetail> tdqw = new QueryWrapper<>();
        tdqw.eq("support_roles", Constants.PartRole.MV_SIMULATION);
        List<TjDeviceDetail> deviceDetails = tjDeviceDetailService.list(tdqw);
        if (null != deviceDetails && !deviceDetails.isEmpty()) {
            TjDeviceDetail deviceDetail = deviceDetails.get(0);
            TjTaskDataConfig dataConfig = new TjTaskDataConfig();
            dataConfig.setType(Constants.PartRole.MV_SIMULATION);
            dataConfig.setDeviceId(deviceDetail.getDeviceId());
            dataConfig.setTaskId(taskId);
            dataConfig.setCaseId(caseId);
        } else {
            throw new BusinessException("无限里程-仿真车添加失败！");
        }
    }

    @Override
    public void saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) {
        String weights = JSON.toJSONString(saveCustomScenarioWeightBo.getWeights());
        tjInfinityMapper.saveCustomScenarioWeight(saveCustomScenarioWeightBo.getTask_id(), weights, "0");
    }

    @Override
    public void saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo) {
        String weights = JSON.toJSONString(saveCustomIndexWeightBo.getList());
        tjInfinityMapper.saveCustomScenarioWeight(saveCustomIndexWeightBo.getTask_id(), weights, "1");
    }

    @Override
    public int updateTaskStatus(String status, int id) {
        TjInfinityTask tjInfinityTask = new TjInfinityTask();
        tjInfinityTask.setId(id);
        tjInfinityTask.setStatus(status);
        return tjInfinityMapper.updateById(tjInfinityTask);
    }

    @Override
    public InfinityTaskInitVo init(Integer taskId) throws BusinessException {
        InfinityTaskInitVo infinityTaskInitVo = new InfinityTaskInitVo();
        TjInfinityTask byId = this.getById(taskId);

        InfinteMileScenceExo infinteMileScenceExo = infinteMileScenceService.selectInfinteMileScenceById(
            byId.getCaseId());
        List<SiteSlice> siteSlices = infinteMileScenceExo.getSiteSlices();
        List<ShardingInfoVo> sis = siteSlices.stream().map(e -> {
            List<Point2D.Double> lls = e.getRoute().stream().map(
                    ee -> new Point2D.Double(Double.parseDouble(ee.getLongitude()),
                        Double.parseDouble(ee.getLatitude())))
                .collect(Collectors.toList());
            return new ShardingInfoVo(e.getSliceId(), lls);
        }).collect(Collectors.toList());

        boolean running = checkTaskStatus(byId.getStatus());
        if (!running) {
            List<TjTaskDataConfig> tjTaskDataConfigs = taskDevices(taskId);
            List<TjTaskDataConfig> exceptSVDevices = tjTaskDataConfigs.stream()
                .filter(
                    t -> !Constants.PartRole.MV_SIMULATION.equals(t.getType()))
                .collect(Collectors.toList());
            TjTaskDataConfig simulationConfig = tjTaskDataConfigs.stream()
                .filter(
                    t -> Constants.PartRole.MV_SIMULATION.equals(t.getType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到仿真设备"));
            // 重置
            devicesReset(0, taskId, infinteMileScenceExo,
                simulationConfig, exceptSVDevices, byId.getCreatedBy());
        }

        infinityTaskInitVo.setShardingInfos(sis);
        infinityTaskInitVo.setRunning(running);
        return infinityTaskInitVo;
    }

    @Override
    public InfinityTaskPreparedVo prepare(Integer taskId)
        throws BusinessException {
        InfinityTaskPreparedVo infinityTaskPreparedVo = new InfinityTaskPreparedVo();
        TjInfinityTask byId = this.getById(taskId);
        checkDevicesStatus(taskId, infinityTaskPreparedVo, byId.getRouteFile());
        return infinityTaskPreparedVo;
    }

    @Override
    public boolean preStart(Integer taskId) {
        TjInfinityTask byId = this.getById(taskId);
        InfinteMileScenceExo infinteMileScenceExo = infinteMileScenceService.selectInfinteMileScenceById(
            byId.getCaseId());
        List<SiteSlice> siteSlices = infinteMileScenceExo.getSiteSlices();

        CaseTrajectoryParam caseTrajectoryParam = new CaseTrajectoryParam();
        caseTrajectoryParam.setTaskId(0);
        caseTrajectoryParam.setCaseId(taskId);
        caseTrajectoryParam.setTestMode(Constants.TestMode.INFINITY_TEST);
        List<TjTaskDataConfig> tjTaskDataConfigs = taskDevices(taskId);
        caseTrajectoryParam.setVehicleIdTypeMap(tjTaskDataConfigs.stream()
            .collect(HashMap::new,
                (m, v) -> m.put(v.getType(), String.valueOf(v.getDeviceId())),
                HashMap::putAll));
        TjTaskDataConfig avConfig = tjTaskDataConfigs.stream()
            .filter(e -> Constants.PartRole.AV.equals(e.getType())).findFirst()
            .get();
        TjTaskDataConfig simulationConfig = tjTaskDataConfigs.stream()
            .filter(e -> Constants.PartRole.MV_SIMULATION.equals(e.getType()))
            .findFirst().get();
        caseTrajectoryParam.setDataChannel(
            tjDeviceDetailService.getById(avConfig.getDeviceId())
                .getDataChannel());
        List<CaseSSInfo> trajectorySS = siteSlices.stream().map(e -> {
            CaseSSInfo caseSSInfo = new CaseSSInfo();
            caseSSInfo.setCaseId(taskId);
            caseSSInfo.setShardingId(e.getSliceId());
            caseSSInfo.setTrajectoryPoints(e.getRoute().stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("latitude", r.getLatitude());
                map.put("longitude", r.getLongitude());
                return map;
            }).collect(Collectors.toList()));
            return caseSSInfo;
        }).collect(Collectors.toList());
        caseTrajectoryParam.setCaseTrajectorySSVoList(trajectorySS);
        caseTrajectoryParam.setTaskDuration(byId.getPlanTestTime());
        caseTrajectoryParam.setControlChannel(
            tjDeviceDetailService.getById(simulationConfig.getDeviceId())
                .getCommandChannel());

        String key = Constants.ChannelBuilder.buildTestingDataChannel(SecurityUtils.getUsername(), taskId);
        kafkaCollector.remove(key, taskId);

        Map<String, Object> context = new HashMap<>();
        context.put("user", SecurityUtils.getUsername());
        caseTrajectoryParam.setContext(context);
        restService.sendCaseTrajectoryInfo(caseTrajectoryParam);
        return false;
    }

    @Override
    public boolean startStop(Integer taskId, Integer caseId, Integer action, String username)
        throws BusinessException {
        // 1.用例详情
        TjInfinityTask tjInfinityTask = this.getById(caseId);
        // 2.向主控发送规则
        List<TjTaskDataConfig> tjTaskDataConfigs = taskDevices(tjInfinityTask.getCaseId());
        List<TjDeviceDetail> tjDeviceDetails = tjDeviceDetailService.listByIds(
            tjTaskDataConfigs.stream().map(TjTaskDataConfig::getDeviceId)
                .collect(Collectors.toList()));
        TjDeviceDetail avDetail = tjDeviceDetails.stream()
            .filter(e -> Constants.PartRole.AV.equals(e.getSupportRoles()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("用例主车配置信息异常"));

        control(0, caseId, avDetail.getCommandChannel(), username,
            tjDeviceDetails, action);

        // 3.更新业务数据
        tjInfinityTask.setStatus(1 == action ?
            Constants.TaskStatusEnum.RUNNING.getCode() :
            Constants.TaskStatusEnum.FINISHED.getCode());
        this.updateById(tjInfinityTask);
        return true;
    }

    private void checkDevicesStatus(Integer taskId,
        InfinityTaskPreparedVo infinityTaskPreparedVo, String routeFile)
        throws BusinessException {
        List<TjTaskDataConfig> configList = taskDevices(taskId);

        List<DeviceInfo> deviceInfos = new ArrayList<>();
        for (TjTaskDataConfig dataConfig : configList) {
            DeviceInfo deviceInfo = initDeviceBaseInfo(dataConfig);
            checkDeviceBusyStatus(infinityTaskPreparedVo, dataConfig,
                deviceInfo);
            if(!infinityTaskPreparedVo.isCanStart()){
                continue;
            }
            checkDeviceOnlineStatus(infinityTaskPreparedVo, taskId, dataConfig, deviceInfo);
            if(!infinityTaskPreparedVo.isCanStart()){
                continue;
            }
            checkDevicePosition(infinityTaskPreparedVo, taskId, dataConfig, deviceInfo, routeFile);

            deviceInfos.add(deviceInfo);
        }

        infinityTaskPreparedVo.setDevicesInfo(deviceInfos);
    }

    private void checkDevicePosition(InfinityTaskPreparedVo infinityTaskPreparedVo, Integer taskId,
        TjTaskDataConfig dataConfig, DeviceInfo deviceInfo, String routeFile)
        throws BusinessException {
        DeviceReadyStateParam stateParam = new DeviceReadyStateParam(
            deviceInfo.getId(),
            RedisChannelUtils.getCommandChannelByRole(0, taskId,
                dataConfig.getType(), deviceInfo.getCommandChannel()));
        if (Constants.PartRole.AV.equals(deviceInfo.getType())) {
            stateParam.setParams(
                new ParamsDto("1", routeService.mainTrajectory(routeFile)));
        }
        if (Constants.PartRole.MV_SIMULATION.equals(deviceInfo.getType())) {
           // 无限里程不发送其它自定参数
        }
        Integer i = tjDeviceDetailService.selectDeviceReadyState(
            deviceInfo.getId(), RedisChannelUtils.getReadyStatusChannelByRole(
                dataConfig.getCaseId(), dataConfig.getType()), stateParam,
            false);
        if(1== i){
            deviceInfo.setDeviceStatus(DeviceStatus.ARRIVED);
        }else {
            deviceInfo.setDeviceStatus(DeviceStatus.NOT_ARRIVED);
            infinityTaskPreparedVo.setCanStart(false);
            infinityTaskPreparedVo.setMessage(
                String.format("设备[%d]未到达！", dataConfig.getDeviceId()));
        }
    }

    private void checkDeviceOnlineStatus(
        InfinityTaskPreparedVo infinityTaskPreparedVo, Integer taskId,
        TjTaskDataConfig dataConfig, DeviceInfo deviceInfo) {
        Integer i = tjDeviceDetailService.selectDeviceState(
            dataConfig.getDeviceId(),
            RedisChannelUtils.getCommandChannelByRole(0, taskId,
                dataConfig.getType(), deviceInfo.getCommandChannel()), false);
        if (1 == i) {
            deviceInfo.setDeviceStatus(DeviceStatus.ONLINE);
        } else {
            deviceInfo.setDeviceStatus(DeviceStatus.OFFLINE);
            infinityTaskPreparedVo.setCanStart(false);
            infinityTaskPreparedVo.setMessage(
                String.format("设备[%s]离线！", dataConfig.getParticipatorName()));
        }
    }

    private void checkDeviceBusyStatus(
        InfinityTaskPreparedVo infinityTaskPreparedVo,
        TjTaskDataConfig dataConfig, DeviceInfo deviceInfo) {
        Integer running = tjDeviceDetailService.selectDeviceBusyStatus(
            DeviceUtils.getVisualDeviceId(0, dataConfig.getDeviceId(),
                dataConfig.getType()));
        if (1 == running) {
            deviceInfo.setDeviceStatus(DeviceStatus.BUSY);
            infinityTaskPreparedVo.setCanStart(false);
            infinityTaskPreparedVo.setMessage(String.format("设备[%s]使用中",
                dataConfig.getParticipatorName()));
        } else {
            deviceInfo.setDeviceStatus(DeviceStatus.IDLE);
        }
    }

    private DeviceInfo initDeviceBaseInfo(TjTaskDataConfig dataConfig) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(dataConfig.getDeviceId());
        deviceInfo.setType(dataConfig.getType());
        deviceInfo.setName(dataConfig.getParticipatorName());
        TjDeviceDetail byId = tjDeviceDetailService.getById(
            dataConfig.getDeviceId());
        deviceInfo.setCommandChannel(byId.getCommandChannel());
        return deviceInfo;
    }

    private void devicesReset(Integer taskId, Integer caseId,
        InfinteMileScenceExo infinteMileScenceExo,
        TjTaskDataConfig simulationConfig,
        List<TjTaskDataConfig> exceptSVDevicesConf, String createBy)
        throws BusinessException {
        TjDeviceDetail svDetail = tjDeviceDetailService.getById(
            simulationConfig.getDeviceId());
        List<TjDeviceDetail> exceptSVDevices = tjDeviceDetailService.listByIds(
            exceptSVDevicesConf.stream().map(TjTaskDataConfig::getDeviceId)
                .collect(Collectors.toList()));
        TjDeviceDetail avDeviceDetail = exceptSVDevices.stream()
            .filter(e -> Constants.PartRole.AV.equals(e.getSupportRoles()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("未查询到主车配置信息"));
        // 先停止
        List<TjDeviceDetail> tjDeviceDetails = new ArrayList<>(exceptSVDevices);
        tjDeviceDetails.add(svDetail);
        control(taskId, caseId, avDeviceDetail.getCommandChannel(), createBy,
            tjDeviceDetails, 0);
        List<String> mapList = new ArrayList<>();
        if (ObjectUtils.isEmpty(infinteMileScenceExo.getMapId())) {
            mapList.add("10");
        } else {
            mapList.add(String.valueOf(infinteMileScenceExo.getMapId()));
        }
        // 4.唤醒仿真服务
        if (!restService.startServer(svDetail.getIp(),
            Integer.valueOf(svDetail.getServiceAddress()),
            TessngUtils.buildInfinityTaskRunParam(caseId,
                SecurityUtils.getUsername(), mapList,
                verifiedInfiniteTessParm(infinteMileScenceExo)))) {
            throw new BusinessException("唤起仿真服务失败");
        }
        if (!redisLock.tryLock("case_" + taskId, SecurityUtils.getUsername())) {
            throw new BusinessException("当前用例正在测试中，请稍后再试");
        }
        for (TjDeviceDetail deviceDetail : exceptSVDevices) {
            if (!redisLock.tryLock("task_" + deviceDetail.getDataChannel(),
                SecurityUtils.getUsername())) {
                throw new BusinessException(
                    deviceDetail.getDeviceName() + "设备正在使用中，请稍后再试");
            }
        }
    }

    private static InfiniteTessParm verifiedInfiniteTessParm(
        InfinteMileScenceExo infinteMileScenceExo) {
        InfiniteTessParm infiniteTessParm = new InfiniteTessParm();
        infiniteTessParm.setSiteSlices(infinteMileScenceExo.getSiteSlices());
        infiniteTessParm.setTrafficFlowConfigs(infinteMileScenceExo.getTrafficFlowConfigs());
        infiniteTessParm.setTrafficFlows(infinteMileScenceExo.getTrafficFlows());
        return infiniteTessParm;
    }

    public void control(Integer taskId, Integer caseId, String avCommandChannel,
        String createBy, List<TjDeviceDetail> deviceDetails, int taskType)
        throws BusinessException {

        TessParam tessParam = TessngUtils.buildTessServerParam(1, createBy,
            taskId, null);
        if (!restService.sendRuleUrl(
            new CaseRuleControl(System.currentTimeMillis(), taskId, caseId, taskType,
                DeviceUtils.generateDeviceConnRules(deviceDetails,
                    tessParam.getCommandChannel(), tessParam.getDataChannel()),
                avCommandChannel, true))) {
            throw new BusinessException("主控响应异常");
        }
    }

    private List<TjTaskDataConfig> taskDevices(Integer taskId) {
        QueryWrapper<TjTaskDataConfig> tcqw = new QueryWrapper<>();
        tcqw.eq("task_id", taskId);
        return tjTaskDataConfigService.list(tcqw);
    }

    private boolean checkTaskStatus(String status) {
        return Constants.TaskStatusEnum.RUNNING.getCode().equals(status);
    }
}
