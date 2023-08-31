package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.domain.WebsocketMessage;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.service.RouteService;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.socket.realTest.RealWebSocketManage;
import net.wanji.socket.simulation.WebSocketManage;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private final ConcurrentHashMap<String, ChannelListener<SimulationTrajectoryDto>> runningChannel = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public ImitateRedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private RouteService routeService;

    @PostConstruct
    public void validChannel() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new DefaultThreadFactory("RedisTrajectoryConsumer-removeListeners"));
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
    public void subscribeAndSend(TjCase tjCase, List<String> channels) {
        ObjectMapper objectMapper = new ObjectMapper();
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(tjCase.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        MessageListener listener = (message, pattern) -> {
            try {
                SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(message.toString(),
                        SimulationTrajectoryDto.class);
                log.info(StringUtils.format("{}第{}帧轨迹：{}", pattern.toString(), getDataSize(pattern.toString()),
                        JSONObject.toJSONString(simulationTrajectory)));
                if (CollectionUtils.isNotEmpty(simulationTrajectory.getValue())) {
                    // 实际轨迹消息
                    List<TrajectoryValueDto> data = simulationTrajectory.getValue();
                    routeService.checkRoute(tjCase.getId(), originalTrajectory, data);
                    receiveData(tjCase.getCaseNumber(), simulationTrajectory);
                    // 计时
                    String countDown = DateUtils.secondsToDuration(
                            (int) Math.floor((double) (getDataSize(tjCase.getCaseNumber())) / 10));
                    // send ws
                    WebsocketMessage msg = new WebsocketMessage(countDown, data);
                    RealWebSocketManage.sendInfo(pattern.toString(), JSONObject.toJSONString(msg));
                }
                if (getDataSize(pattern.toString()) == 500) {
                    removeListenerThenSave(pattern.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeListenerThenSave(tjCase.getCaseNumber());
            }
        };
        this.addRunningChannel(tjCase.getId(), channels, listener);
    }

    public void removeMessageListener(@Nullable MessageListener listener, String channel) {
        redisMessageListenerContainer.removeMessageListener(listener, new ChannelTopic(channel));
    }

    public void addRunningChannel(Integer recordId, List<String> channels, MessageListener listener) {
        if (this.runningChannel.containsKey(recordId)) {
            return;
        }
        List<ChannelTopic> topics = channels.stream().map(ChannelTopic::new).collect(Collectors.toList());
        redisMessageListenerContainer.addMessageListener(listener, topics);
        for (ChannelTopic topic : topics) {
            this.runningChannel.put(topic.getTopic(), new ChannelListener(recordId, topic.getTopic(), SecurityUtils.getUsername(),
                    System.currentTimeMillis(), listener));
        }

    }

    public void receiveData(String channel, SimulationTrajectoryDto data) {
        if (!this.runningChannel.containsKey(channel)) {
            return;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
        channelListener.refreshData(data);
    }

    public synchronized int getDataSize(String channel) {
        if (!this.runningChannel.containsKey(channel)) {
            return 0;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
        return channelListener.getCurrentSize();
    }

    public void removeListenerThenSave(String channel) {
        if (!this.runningChannel.containsKey(channel)) {
            return;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
        removeMessageListener(channelListener.getListener(), channelListener.getChannel());
        try {
            routeService.saveRouteFile(channelListener.getRecordId(), channelListener.getData());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        this.runningChannel.remove(channel);
    }

    public void removeListeners() {
        try {
            Iterator<Map.Entry<String, ChannelListener<SimulationTrajectoryDto>>> iterator =
                    this.runningChannel.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ChannelListener<SimulationTrajectoryDto>> entry = iterator.next();
                ChannelListener<SimulationTrajectoryDto> channelListener = entry.getValue();
                if (channelListener.isExpire()) {
                    removeMessageListener(channelListener.getListener(), channelListener.getChannel());
//                    try {
//                        routeService.saveRouteFile(channelListener.getChannel(),
//                                channelListener.getData());
//                    } catch (ExecutionException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
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
        private Long timestamp;
        private MessageListener listener;
        private List<T> data;

        public ChannelListener(Integer recordId, String channel, String userName, Long timestamp,
                               MessageListener listener) {
            this.recordId = recordId;
            this.channel = channel;
            this.userName = userName;
            this.timestamp = timestamp;
            this.listener = listener;
            this.data = new ArrayList<>();
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


        public Long getTimestamp() {
            return timestamp;
        }


        public MessageListener getListener() {
            return listener;
        }

        public List<T> getData() {
            return data;
        }
    }

}
