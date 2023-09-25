package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.component.CountDown;
import net.wanji.business.component.PathwayPoints;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.CountDownDto;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.service.RouteService;
import net.wanji.business.trajectory.RedisTrajectoryConsumer.ChannelListener;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DataUtils;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.business.socket.WebSocketManage;
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
public class ImitateRedisTrajectoryConsumer {

    private static final Logger log = LoggerFactory.getLogger("business");

    private final ConcurrentHashMap<String, List<ChannelListener<SimulationTrajectoryDto>>> runningChannel = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public ImitateRedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @PostConstruct
    public void validChannel() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new DefaultThreadFactory("ImitateRedisTrajectoryConsumer-removeListeners"));
        scheduledExecutorService.scheduleAtFixedRate(
                this::removeListeners, 0, 20, TimeUnit.SECONDS);
    }


    /**
     * 订阅轨迹
     *
     * @param caseInfoBo 用例信息
     * @return
     */
    public void subscribeAndSend(CaseInfoBo caseInfoBo) throws IOException {
        // 添加监听器
        this.addRunningChannel(caseInfoBo);
    }


    public void addRunningChannel(CaseInfoBo caseInfoBo) throws IOException {
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(caseInfoBo.getCaseRealRecord().getId()),
                WebSocketManage.REAL, null);
        if (this.runningChannel.containsKey(key)) {
            removeListener(key, false, null);
        }
        MessageListener listener = createListener(key, caseInfoBo);
        List<ChannelTopic> topics = caseInfoBo.getCaseConfigs().stream().map(CaseConfigBo::getDataChannel).map(ChannelTopic::new).collect(Collectors.toList());
        List<ChannelListener<SimulationTrajectoryDto>> listeners = new ArrayList<>();
        for (CaseConfigBo configBo : caseInfoBo.getCaseConfigs()) {
            ChannelListener<SimulationTrajectoryDto> channelListener =
                    new ChannelListener<>(caseInfoBo.getId(), caseInfoBo.getCaseRealRecord().getId(),
                            configBo.getDataChannel(), SecurityUtils.getUsername(),
                            configBo.getSupportRoles(), System.currentTimeMillis(), listener);
            listeners.add(channelListener);
        }
        log.info("addRunningChannel:{}", JSONObject.toJSONString(topics));
        this.runningChannel.put(key, listeners);
        redisMessageListenerContainer.addMessageListener(listener, topics);
    }


    /**
     * 创建监听器
     *
     * @param caseInfoBo 用例信息
     * @return
     */
    public MessageListener createListener(String key, CaseInfoBo caseInfoBo) throws IOException {
        TjCaseRealRecord caseRealRecord = caseInfoBo.getCaseRealRecord();
        List<CaseConfigBo> configBos = caseInfoBo.getCaseConfigs();
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 所有通道和业务车辆ID映射
        Map<String, List<CaseConfigBo>> allChannelAndBusinessIdMap = configBos.stream().collect(Collectors.groupingBy(CaseConfigBo::getDataChannel));
        // av配置
        List<CaseConfigBo> avConfigs = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles())).collect(Collectors.toList());
        // av类型通道和业务车辆ID映射
        Map<String, String> avChannelAndBusinessIdMap = avConfigs.stream().collect(Collectors.toMap(
                CaseConfigBo::getDataChannel, CaseConfigBo::getBusinessId));
        // av类型通道和业务车辆名称映射
        Map<String, String> avChannelAndNameMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .collect(Collectors.toMap(CaseConfigBo::getDataChannel, CaseConfigBo::getName));
        CaseConfigBo caseConfigBo = avConfigs.get(0);
        Map<String, List<TrajectoryDetailBo>> avBusinessIdPointsMap = originalTrajectory.getParticipantTrajectories()
                .stream().filter(item ->
                        avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        // 主车全部点位
        List<TrajectoryDetailBo> avPoints = avBusinessIdPointsMap.get(caseConfigBo.getBusinessId());
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
        TjCase tjCase = caseMapper.selectById(caseRealRecord.getCaseId());
        List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(tjCase.getRouteFile(),
                caseConfigBo.getBusinessId());
        List<TrajectoryValueDto> mainSimuTrajectories = mainSimulations.stream()
                .map(item -> item.get(0)).collect(Collectors.toList());
        Map<String, Object> realMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String methodLog = StringUtils.format("{}实车验证 - ", caseRealRecord.getCaseId());
        return (message, pattern) -> {
            try {
                String channel = new String(message.getChannel());
                SimulationMessage simulationMessage = objectMapper.readValue(message.toString(),
                        SimulationMessage.class);
                log.info("{}{}:{}", methodLog, channel, JSONObject.toJSONString(simulationMessage));
                switch (simulationMessage.getType()) {
                    case RedisMessageType.TRAJECTORY:
                        SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(simulationMessage.getValue(),
                                SimulationTrajectoryDto.class);
                        // 实际轨迹消息
                        List<TrajectoryValueDto> data = simulationTrajectory.getValue();
                        for (TrajectoryValueDto trajectoryValueDto : CollectionUtils.emptyIfNull(data)) {
                            trajectoryValueDto.setName(DataUtils.convertUnicodeToChinese(trajectoryValueDto.getName()));
                            allChannelAndBusinessIdMap.get(channel).stream().filter(item -> item.getName().equals(trajectoryValueDto.getName())).findFirst().ifPresent(item -> trajectoryValueDto.setId(item.getBusinessId()));
                        }
                        if ("TESSResult".equals(channel)) {
                            List<TrajectoryValueDto> mv = CollectionUtils.emptyIfNull(data).stream().filter(item ->
                                    !item.getName().contains("主车")).collect(Collectors.toList());
                            simulationTrajectory.setValue(mv);
                            data = mv;
                        }
                        // 无论是否有轨迹都保存
                        receiveData(key, channel, simulationTrajectory);
                        if (CollectionUtils.isNotEmpty(data)) {
                            // send ws
                            String duration = DateUtils.secondsToDuration(
                                    (int) Math.floor((double) (getDataSize(key, channel)) / 10));
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
                                    double percent = mileage > 0 ? 1 - (remainLength / mileage) : 1;
                                    realMap.put("percent", percent * 100);
                                }
                                PathwayPoints nearestPoint = pathwayPoints.findNearestPoint(data.get(0).getLongitude(), data.get(0).getLatitude());
                                Map<String, Object> tipsMap = new HashMap<>();
                                if (nearestPoint.hasTips()) {
                                    tipsMap.put("name", avChannelAndNameMap.get(channel));
                                    tipsMap.put("pointName", nearestPoint.getPointName());
                                    tipsMap.put("pointDistance", nearestPoint.getDistance());
                                    tipsMap.put("pointSpeed", nearestPoint.getPointSpeed());
                                }
                                realMap.put("tips", tipsMap);
                                // 仿真车未来轨迹
                                List<Map<String, Double>> futureList = new ArrayList<>();
//                                if (!CollectionUtils.isEmpty(mainSimuTrajectories)) {
//                                    // 仿真验证中当前主车位置
//                                    data.add(mainSimuTrajectories.remove(0));
//                                    // 仿真验证中主车剩余轨迹
//                                    futureList = mainSimuTrajectories.stream().map(item -> {
//                                        Map<String, Double> posMap = new HashMap<>();
//                                        posMap.put("longitude", item.getLongitude());
//                                        posMap.put("latitude", item.getLatitude());
//                                        return posMap;
//                                    }).collect(Collectors.toList());
//                                }
                                realMap.put("simuFuture", futureList);
                                realMap.put("speed", data.get(0).getSpeed());
                                RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, realMap, data,
                                        duration);
                                WebSocketManage.sendInfo(key.concat("_").concat(channel), JSONObject.toJSONString(msg));
                            } else {
                                RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, null, data,
                                        duration);
                                WebSocketManage.sendInfo(key.concat("_").concat(channel), JSONObject.toJSONString(msg));
                            }
                        }
                        break;
                    case RedisMessageType.END:
                        if ("TESSResult".equals(channel)) {
                            CaseTrajectoryDetailBo end = objectMapper.readValue(simulationMessage.getValue(),
                                    CaseTrajectoryDetailBo.class);
                            log.info(StringUtils.format("结束接收{}数据：{}", tjCase.getCaseNumber(),
                                    JSONObject.toJSONString(end)));
                            try {
                                Optional.ofNullable(end.getEvaluationVerify()).ifPresent(originalTrajectory::setEvaluationVerify);
                            } catch (Exception e) {
                                originalTrajectory.setEvaluationVerify("True");
                            }
                            TjCaseRealRecord param = new TjCaseRealRecord();
                            param.setId(caseRealRecord.getId());
                            String duration = DateUtils.secondsToDuration(
                                    (int) Math.floor((double) (getDataSize(key,
                                            caseConfigBo.getDataChannel())) / 10));
                            originalTrajectory.setDuration(duration);
                            param.setDetailInfo(JSONObject.toJSONString(originalTrajectory));
                            int endSuccess = caseRealRecordMapper.updateById(param);
                            log.info(StringUtils.format("修改用例场景评价:{}", endSuccess));
                            removeListener(key, true, originalTrajectory);

                            RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null, null,
                                    duration);
                            WebSocketManage.sendInfo(key.concat("_").concat(channel), JSONObject.toJSONString(endMsg));
                        }
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeListener(key, true, originalTrajectory);
            }
        };
    }

    public void receiveData(String key, String channel, SimulationTrajectoryDto data) {
        if (!this.runningChannel.containsKey(key)) {
            return;
        }
        for (ChannelListener<SimulationTrajectoryDto> channelListener : this.runningChannel.get(key)) {
            if (channelListener.getChannel().equals(channel)) {
                channelListener.refreshData(data);
            }
        }
    }

    public synchronized int getDataSize(String key, String channel) {
        if (!this.runningChannel.containsKey(key)) {
            return 0;
        }
        for (ChannelListener<SimulationTrajectoryDto> channelListener : this.runningChannel.get(key)) {
            if (channelListener.getChannel().equals(channel)) {
                return channelListener.getCurrentSize();
            }
        }
        return 0;
    }

    /**
     * 移除监听器
     * @param key
     * @param save
     * @param originalTrajectory
     */
    public void removeListener(String key, boolean save, CaseTrajectoryDetailBo originalTrajectory) {
        if (!this.runningChannel.containsKey(key)) {
            return;
        }
        removeMessageListeners(key);
        List<RealTestTrajectoryDto> data = new ArrayList<>();
        List<ChannelListener<SimulationTrajectoryDto>> channelListeners = this.runningChannel.get(key);
        Integer recordId = channelListeners.get(0).getRecordId();
        for (ChannelListener<SimulationTrajectoryDto> channelListener : channelListeners) {
            RealTestTrajectoryDto realTestTrajectoryDto = new RealTestTrajectoryDto();
            realTestTrajectoryDto.setChannel(channelListener.getChannel());
            realTestTrajectoryDto.setData(channelListener.getData());
            for (SimulationTrajectoryDto trajectoryDto : channelListener.getData()) {
                routeService.checkRealRoute(recordId, originalTrajectory, trajectoryDto.getValue());
            }
            data.add(realTestTrajectoryDto);
        }
        if (save) {
            try {
                routeService.saveRealRouteFile(recordId, data);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.runningChannel.remove(key);
    }

    /**
     * 移除所有过期监听器
     */
    public void removeListeners() {
        try {
            Iterator<Entry<String, List<ChannelListener<SimulationTrajectoryDto>>>> iterator =
                    this.runningChannel.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, List<ChannelListener<SimulationTrajectoryDto>>> next = iterator.next();
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

    public void removeMessageListeners(String key) {
        if (!this.runningChannel.containsKey(key)) {
            return;
        }
        List<ChannelListener<SimulationTrajectoryDto>> channelListeners = runningChannel.get(key);
        List<ChannelTopic> topics = channelListeners.stream().map(ChannelListener::getChannel).map(ChannelTopic::new)
                .collect(Collectors.toList());
        log.info("removeMessageListeners:{}", JSONObject.toJSONString(topics));
        redisMessageListenerContainer.removeMessageListener(channelListeners.get(0).getListener(), topics);
    }

    public static class ChannelListener<T> {
        private Integer caseId;
        private Integer recordId;
        private String channel;
        private String userName;
        private String role;
        private Long timestamp;
        private MessageListener listener;
        private List<T> data;
        private boolean finished;

        public ChannelListener(Integer caseId, Integer recordId, String channel, String userName, String role, Long timestamp,
                               MessageListener listener) {
            this.caseId = caseId;
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

        public Integer getCaseId() {
            return caseId;
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
