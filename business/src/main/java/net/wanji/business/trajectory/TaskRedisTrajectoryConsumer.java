package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.component.CountDown;
import net.wanji.business.component.PathwayPoints;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.CountDownDto;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.service.RouteService;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.socket.simulation.WebSocketManage;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 9:28
 * @Descriptoin:
 */
@Component
public class TaskRedisTrajectoryConsumer {

    private static final Logger log = LoggerFactory.getLogger("business");

    private final ConcurrentHashMap<Integer, List<ChannelListener<SimulationTrajectoryDto>>> runningChannel = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public TaskRedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @PostConstruct
    public void validChannel() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new DefaultThreadFactory("TaskRedisTrajectoryConsumer-removeListeners"));
        scheduledExecutorService.scheduleAtFixedRate(
                this::removeListeners, 0, 20, TimeUnit.SECONDS);
    }


    /**
     * 订阅测试用例轨迹
     * @param taskCaseRecord 实车验证记录
     * @param configBos 用例配置信息
     */
    public void subscribeAndSend(TjTaskCaseRecord taskCaseRecord, List<TaskCaseConfigBo> configBos) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(taskCaseRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 所有通道和业务车辆ID映射
        Map<String, String> allChannelAndBusinessIdMap = configBos.stream().collect(Collectors.toMap(TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorId));
        // av配置
        List<TaskCaseConfigBo> avConfigs = configBos.stream().filter(item -> PartRole.AV.equals(item.getType())).collect(Collectors.toList());
        // av类型通道和业务车辆ID映射
        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
                TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorId));
        // av类型通道和业务车辆名称映射
        Map<String, String> avChannelAndNameMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .collect(Collectors.toMap(TaskCaseConfigBo::getDataChannel, TaskCaseConfigBo::getParticipatorName));
        TaskCaseConfigBo caseConfigBo = avConfigs.get(0);
        Map<String, List<TrajectoryDetailBo>> avBusinessIdPointsMap = originalTrajectory.getParticipantTrajectories()
                .stream().filter(item ->
                        avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
                                ParticipantTrajectoryBo::getId,
                                ParticipantTrajectoryBo::getTrajectory
                ));
        // 主车全部点位
        List<TrajectoryDetailBo> avPoints = avBusinessIdPointsMap.get(caseConfigBo.getParticipatorId());
        // 主车全程、剩余时间、到达时间
        ArrayList<Point2D.Double> doubles = new ArrayList<>();
        for (TrajectoryDetailBo trajectoryDetailBo : avPoints) {
            String[] pos = trajectoryDetailBo.getPosition().split(",");
            doubles.add(new Point2D.Double(Double.parseDouble(pos[0]), Double.parseDouble(pos[1])));
        }
        CountDown countDown = new CountDown(doubles);
        // 主车途径点提示
        PathwayPoints pathwayPoints = new PathwayPoints(avPoints);
        // 读取仿真验证主车轨迹
        TjCase tjCase = caseMapper.selectById(taskCaseRecord.getCaseId());
        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(tjCase.getRouteFile(),
                caseConfigBo.getParticipatorName());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        Map<String, Object> realMap = new HashMap<>();
        Map<String, Object> tipsMap = new HashMap<>();
        MessageListener listener = (message, pattern) -> {
            try {
                String channel = new String(message.getChannel());
                SimulationMessage simulationMessage = objectMapper.readValue(message.toString(),
                        SimulationMessage.class);
                switch (simulationMessage.getType()) {
                    case RedisMessageType.TRAJECTORY:
                        SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(simulationMessage.getValue(),
                                SimulationTrajectoryDto.class);
                        // 实际轨迹消息
                        List<TrajectoryValueDto> data = simulationTrajectory.getValue();
                        for (TrajectoryValueDto trajectoryValueDto : CollectionUtils.emptyIfNull(data)) {
                            // 填充业务ID
                            trajectoryValueDto.setId(allChannelAndBusinessIdMap.get(channel));
                        }
                        // 无论是否有轨迹都保存
                        receiveData(taskCaseRecord.getId(), channel, simulationTrajectory);
                        if (CollectionUtils.isNotEmpty(data)) {
                            // send ws
                            String duration = DateUtils.secondsToDuration(
                                    (int) Math.floor((double) (getDataSize(taskCaseRecord.getId(), channel)) / 10));
                            if (!ObjectUtils.isEmpty(avChannelAndBusinessIdMap)
                                    && avChannelAndBusinessIdMap.containsKey(channel)) {
                                CountDownDto countDownDto = countDown.countDown(data.get(0).getSpeed(),
                                        new Point2D.Double(data.get(0).getLongitude(), data.get(0).getLatitude()));
                                if (!ObjectUtils.isEmpty(countDownDto)) {
                                    double mileage = countDownDto.getFullLength();
                                    double remainLength = countDownDto.getRemainLength();
                                    realMap.put("mileage", String.format("%.2f", mileage));
                                    realMap.put("duration", countDownDto.getTimeRemaining());
                                    realMap.put("arriveTime", DateUtils.dateToString(countDownDto.getArrivalTime(), DateUtils.HH_MM_SS));
                                    double percent = 1 - (mileage / remainLength);
                                    realMap.put("percent", percent < 0 ? 100 : percent);
                                }
                                PathwayPoints nearestPoint = pathwayPoints.findNearestPoint(data.get(0).getLongitude(), data.get(0).getLatitude());
                                if (nearestPoint.hasTips()) {
                                    tipsMap.put("name", avChannelAndNameMap.get(channel));
                                    tipsMap.put("pointName", nearestPoint.getPointName());
                                    tipsMap.put("pointDistance", nearestPoint.getDistance());
                                    tipsMap.put("pointSpeed", nearestPoint.getPointSpeed());
                                }
                                realMap.put("tips", tipsMap);

                                // 仿真车未来轨迹
                                List<Map<String, Double>> futureList = new ArrayList<>();
                                if (!CollectionUtils.isEmpty(mainSimuTrajectories)) {
                                    // 仿真验证中当前主车位置
                                    data.add(mainSimuTrajectories.remove(0));
                                    // 仿真验证中主车剩余轨迹
                                    futureList = mainSimuTrajectories.stream().map(item -> {
                                        Map<String, Double> posMap = new HashMap<>();
                                        posMap.put("longitude", item.getLongitude());
                                        posMap.put("latitude", item.getLatitude());
                                        return posMap;
                                    }).collect(Collectors.toList());
                                }
                                realMap.put("simuFuture", futureList);
                                realMap.put("speed", data.get(0).getSpeed());
                                RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY,
                                        realMap, data, duration);
                                WebSocketManage.sendInfo(channel, JSONObject.toJSONString(msg));
                            } else {
                                RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY,
                                        null, data, duration);
                                WebSocketManage.sendInfo(channel, JSONObject.toJSONString(msg));
                            }
                        }
                        break;
                    case RedisMessageType.SCORE:
                        log.info(StringUtils.format("{}评分：{}", channel, JSONObject.toJSONString(simulationMessage)));
                        break;
                    case RedisMessageType.END:
                        if ("TESSResult".equals(channel)) {
                            CaseTrajectoryDetailBo end = objectMapper.readValue(simulationMessage.getValue(),
                                    CaseTrajectoryDetailBo.class);
                            log.info(StringUtils.format("结束接收{}数据：{}", tjCase.getCaseNumber(),
                                    JSONObject.toJSONString(end)));
                            Optional.ofNullable(end.getEvaluationVerify()).ifPresent(originalTrajectory::setEvaluationVerify);
                            TjTaskCaseRecord param = new TjTaskCaseRecord();
                            param.setId(taskCaseRecord.getId());
                            String duration = DateUtils.secondsToDuration(
                                    (int) Math.floor((double) (getDataSize(taskCaseRecord.getId(),
                                            caseConfigBo.getDataChannel())) / 10));
                            originalTrajectory.setDuration(duration);
                            param.setDetailInfo(JSONObject.toJSONString(originalTrajectory));
                            int endSuccess = taskCaseRecordMapper.updateById(param);

                            QueryWrapper<TjTaskCase> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq(ColumnName.CASE_ID_COLUMN, taskCaseRecord.getCaseId()).eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
                            TjTaskCase taskCase = taskCaseMapper.selectOne(queryWrapper);
                            taskCase.setStatus("已完成");
                            taskCase.setEndTime(new Date());
                            // todo 通过率计算
                            taskCase.setPassingRate("100%");
                            taskCase.setTestTotalTime(duration);
                            taskCaseMapper.updateById(taskCase);
                            log.info(StringUtils.format("修改用例场景评价:{}", endSuccess));
                            removeListenerThenSave(taskCaseRecord.getId(), originalTrajectory);

                            RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null, null,
                                    duration);
                            WebSocketManage.sendInfo(channel, JSONObject.toJSONString(endMsg));
                        }
                        break;
                    default:
                        break;
                }


            } catch (IOException e) {
                e.printStackTrace();
                removeListenerThenSave(taskCaseRecord.getId(), originalTrajectory);
            }
        };
        this.addRunningChannel(taskCaseRecord.getId(), configBos, listener);
    }

    public void removeMessageListeners(Integer recordId) {
        if (!this.runningChannel.containsKey(recordId)) {
            return;
        }
        List<ChannelListener<SimulationTrajectoryDto>> channelListeners = runningChannel.get(recordId);
        List<ChannelTopic> topics = channelListeners.stream().map(ChannelListener::getChannel).map(ChannelTopic::new)
                .collect(Collectors.toList());
        log.info("removeMessageListeners:{}", JSONObject.toJSONString(topics));
        redisMessageListenerContainer.removeMessageListener(channelListeners.get(0).getListener(), topics);
    }

    public void addRunningChannel(Integer recordId, List<TaskCaseConfigBo> configBos, MessageListener listener) {
        if (this.runningChannel.containsKey(recordId)) {
            return;
        }
        List<ChannelTopic> topics = configBos.stream().map(TaskCaseConfigBo::getDataChannel).map(ChannelTopic::new).collect(Collectors.toList());
        List<ChannelListener<SimulationTrajectoryDto>> listeners = new ArrayList<>();
        for (TaskCaseConfigBo configBo : configBos) {
            ChannelListener<SimulationTrajectoryDto> channelListener =
                    new ChannelListener<>(recordId, configBo.getDataChannel(), SecurityUtils.getUsername(),
                            configBo.getSupportRoles(),
                            System.currentTimeMillis(), listener);
            listeners.add(channelListener);
        }
        log.info("addRunningChannel:{}", JSONObject.toJSONString(topics));
        redisMessageListenerContainer.addMessageListener(listener, topics);
        this.runningChannel.put(recordId, listeners);
    }

    public void receiveData(Integer recordId, String channel, SimulationTrajectoryDto data) {
        if (!this.runningChannel.containsKey(recordId)) {
            return;
        }
        for (ChannelListener<SimulationTrajectoryDto> channelListener : this.runningChannel.get(recordId)) {
            if (channelListener.getChannel().equals(channel)) {
                channelListener.refreshData(data);
            }
        }
    }

    public synchronized int getDataSize(Integer recordId, String channel) {
        if (!this.runningChannel.containsKey(recordId)) {
            return 0;
        }
        for (ChannelListener<SimulationTrajectoryDto> channelListener : this.runningChannel.get(recordId)) {
            if (channelListener.getChannel().equals(channel)) {
                return channelListener.getCurrentSize();
            }
        }
        return 0;
    }

    public void removeListenerThenSave(Integer recordId, CaseTrajectoryDetailBo originalTrajectory) {
        if (!this.runningChannel.containsKey(recordId)) {
            return;
        }
        removeMessageListeners(recordId);
        List<RealTestTrajectoryDto> data = new ArrayList<>();
        for (ChannelListener<SimulationTrajectoryDto> channelListener : this.runningChannel.get(recordId)) {
            RealTestTrajectoryDto realTestTrajectoryDto = new RealTestTrajectoryDto();
            realTestTrajectoryDto.setChannel(channelListener.getChannel());
            realTestTrajectoryDto.setData(channelListener.getData());
            for (SimulationTrajectoryDto trajectoryDto : channelListener.getData()) {
                routeService.checkRealRoute(recordId, originalTrajectory, trajectoryDto.getValue());
            }
            data.add(realTestTrajectoryDto);
        }
        try {
            routeService.saveRealRouteFile(recordId, data);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        this.runningChannel.remove(recordId);
    }

    public void removeListeners() {
        try {
            Iterator<Entry<Integer, List<ChannelListener<SimulationTrajectoryDto>>>> iterator =
                    this.runningChannel.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Integer, List<ChannelListener<SimulationTrajectoryDto>>> next = iterator.next();
                List<ChannelListener<SimulationTrajectoryDto>> value = next.getValue();
                int expireCount = 0;
                for (ChannelListener<SimulationTrajectoryDto> channelListener : value) {
                    if (channelListener.isExpire()) {
                        expireCount++;

                    }
                }
                if (expireCount == value.size()) {
                    removeMessageListeners(next.getKey());
                    iterator.remove();  // Removes the current element from the map
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static class ChannelListener<T> {
        private Integer recordId;
        private String channel;
        private String userName;
        private String role;
        private Long timestamp;
        private MessageListener listener;
        private List<T> data;
        private boolean finished;

        public ChannelListener(Integer recordId, String channel, String userName, String role, Long timestamp,
                               MessageListener listener) {
            this.recordId = recordId;
            this.channel = channel;
            this.userName = userName;
            this.role = role;
            this.timestamp = timestamp;
            this.listener = listener;
            this.data = new ArrayList<>();
            this.finished = false;
        }

        public void refreshData(T data) {
            this.data.add(data);
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpire() {
            return System.currentTimeMillis() - timestamp > 10000;
        }

        public synchronized int getCurrentSize() {
            return this.data.size();
        }

        public Integer getRecordId() {
            return recordId;
        }

        public String getChannel() {
            return channel;
        }


        public String getUserName() {
            return userName;
        }

        public String getRole() {
            return role;
        }

        public Long getTimestamp() {
            return timestamp;
        }


        public MessageListener getListener() {
            return listener;
        }

        public List<T> getData() {
            return data;
        }

        public boolean isFinished() {
            return finished;
        }
    }

}
