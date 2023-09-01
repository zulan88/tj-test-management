package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PointType;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.service.RouteService;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.socket.realTest.RealWebSocketManage;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private final ConcurrentHashMap<Integer, List<ChannelListener<SimulationTrajectoryDto>>> runningChannel = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public ImitateRedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    private RouteService routeService;

    @PostConstruct
    public void validChannel() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new DefaultThreadFactory("ImitateRedisTrajectoryConsumer-removeListeners"));
        scheduledExecutorService.scheduleAtFixedRate(
                this::removeListeners, 0, 20, TimeUnit.SECONDS);
    }


    /**
     * 订阅测试用例轨迹
     *
     * @param tjCase          用例
     * @param participantId   指定车辆id（只有在发送ws时筛选）
     * @param participantName 指定车辆名称（筛选时使用）
     * @return
     */
    public void subscribeAndSend(TjCaseRealRecord caseRealRecord, List<CaseConfigBo> configBos) {
        ObjectMapper objectMapper = new ObjectMapper();
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        Map<String, String> avChannelAndBusinessIdMap = configBos.stream().filter(item -> PartRole.AV.equals(item.getSupportRoles()))
                .collect(Collectors.toMap(CaseConfigBo::getDataChannel, CaseConfigBo::getBusinessId));
        Map<String, String> avBusinessIdPosMap = originalTrajectory.getParticipantTrajectories().stream().filter(item ->
                avChannelAndBusinessIdMap.containsValue(item.getId())).collect(Collectors.toMap(
                ParticipantTrajectoryBo::getId,
                value -> {
                    TrajectoryDetailBo trajectoryDetailBo = value.getTrajectory().stream().filter(point ->
                            PointType.END.equals(point.getType())).findFirst().orElse(null);
                    return ObjectUtils.isEmpty(trajectoryDetailBo) ? "" : trajectoryDetailBo.getPosition();
                }));
        MessageListener listener = (message, pattern) -> {
            try {
                String channel = new String(message.getChannel());
                SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(message.toString(),
                        SimulationTrajectoryDto.class);
                receiveData(caseRealRecord.getId(), channel, simulationTrajectory);
                if (CollectionUtils.isNotEmpty(simulationTrajectory.getValue())) {
                    // 实际轨迹消息
                    List<TrajectoryValueDto> data = simulationTrajectory.getValue();
                    // send ws
                    if (!ObjectUtils.isEmpty(avChannelAndBusinessIdMap)
                            && avChannelAndBusinessIdMap.containsKey(channel)) {
                        Double longitude = simulationTrajectory.getValue().get(0).getLongitude();
                        Double latitude = simulationTrajectory.getValue().get(0).getLatitude();
                        String pos = avBusinessIdPosMap.get(avChannelAndBusinessIdMap.get(channel));
                        double endLongitude = StringUtils.isEmpty(pos) ? 0 : Double.parseDouble(pos.split(",")[0]);
                        double endLatitude = StringUtils.isEmpty(pos) ? 0 : Double.parseDouble(pos.split(",")[1]);
                        double instance = GeoUtil.calculateDistance(latitude, longitude, endLatitude, endLongitude);
                        if (instance <= 4) {
                            removeListenerThenSave(caseRealRecord.getId(), originalTrajectory);
                            RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null, null);
                            RealWebSocketManage.sendInfo(channel, JSONObject.toJSONString(endMsg));
                        } else {
                            Map<String, Object> map = new HashMap<>();
//                            map.put("")

                            RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, null, data);
                            RealWebSocketManage.sendInfo(channel, JSONObject.toJSONString(endMsg));
                        }
                    } else {
                        RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, null, data);
                        RealWebSocketManage.sendInfo(channel, JSONObject.toJSONString(msg));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeListenerThenSave(caseRealRecord.getId(), originalTrajectory);
            }
        };
        this.addRunningChannel(caseRealRecord.getId(), configBos, listener);
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

    public void addRunningChannel(Integer recordId, List<CaseConfigBo> configBos, MessageListener listener) {
        if (this.runningChannel.containsKey(recordId)) {
            return;
        }
        List<ChannelTopic> topics = configBos.stream().map(CaseConfigBo::getDataChannel).map(ChannelTopic::new).collect(Collectors.toList());
        List<ChannelListener<SimulationTrajectoryDto>> listeners = new ArrayList<>();
        for (CaseConfigBo configBo : configBos) {
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

        public int getCurrentSize() {
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
