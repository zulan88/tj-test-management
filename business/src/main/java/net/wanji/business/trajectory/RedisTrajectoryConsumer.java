package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.domain.WebsocketMessage;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.common.common.SimulationInfoDto;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.business.service.RouteService;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.socket.websocket.WebSocketManage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 9:28
 * @Descriptoin:
 */
@Component
public class RedisTrajectoryConsumer {

    private static final Logger log = LoggerFactory.getLogger("business");

    private ConcurrentHashMap<String, ChannelListener<SimulationTrajectoryDto>> runningChannel = new ConcurrentHashMap<>();

    private RedisMessageListenerContainer redisMessageListenerContainer;

    public RedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    private RouteService routeService;

    @Autowired
    private TjCaseMapper caseMapper;

    @PostConstruct
    public void validChannel() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(
                this::removeListeners, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * 订阅测试用例轨迹
     * @param channel 用例编号
     * @param participantId 指定车辆id（只有在发送ws时筛选）
     * @return
     */
    public void subscribeAndSend(Integer caseId, String channel, String participantId) {
        ObjectMapper objectMapper = new ObjectMapper();

        MessageListener listener = (message, pattern) -> {
            try {
                SimulationMessage simulationMessage = objectMapper.readValue(message.toString(),
                        SimulationMessage.class);
                log.info(JSONObject.toJSONString(simulationMessage));
                switch (simulationMessage.getType()) {
                    case RedisMessageType.START:
                        TjCase startCase = caseMapper.selectById(caseId);
                        CaseTrajectoryDetailBo startCaseDetail = JSONObject.parseObject(startCase.getDetailInfo(),
                                CaseTrajectoryDetailBo.class);
                        SimulationInfoDto start = objectMapper.readValue(simulationMessage.getValue(),
                                SimulationInfoDto.class);
                        log.info(StringUtils.format("开始接收{}数据：{}", channel, JSONObject.toJSONString(start)));
                        Optional.ofNullable(start.getSceneDesc()).ifPresent(startCaseDetail::setSceneDesc);
                        Optional.ofNullable(start.getEvaluationVerify()).ifPresent(startCaseDetail::setEvaluationVerify);
                        Optional.ofNullable(start.getSceneForm()).ifPresent(startCaseDetail::setSceneForm);
                        startCase.setDetailInfo(JSONObject.toJSONString(startCaseDetail));
                        caseMapper.updateById(startCase);
                        break;
                    case RedisMessageType.TRAJECTORY:
                        SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(simulationMessage.getValue(),
                                SimulationTrajectoryDto.class);
                        log.info(StringUtils.format("第{}帧轨迹：{}", getDataSize(channel),
                                JSONObject.toJSONString(simulationTrajectory)));
                        if (StringUtils.isNotEmpty(simulationTrajectory.getValue())) {
                            // 实际轨迹消息
                            List<TrajectoryValueDto> data = JSONObject.parseArray(simulationTrajectory.getValue(),
                                    TrajectoryValueDto.class);
                            routeService.checkRoute(caseId, data);
                            receiveData(channel, simulationTrajectory);
                            // 计时
                            String countDown = DateUtils.secondsToDuration(
                                    (int) Math.floor((double) (getDataSize(channel)) / 10));

                            data = routeService.filterParticipant(data, participantId);
                            // send ws
                            WebsocketMessage msg = new WebsocketMessage(countDown, data);
                            WebSocketManage.sendInfo(StringUtils.isEmpty(participantId) ? "ALL_VEHICLE" : participantId,
                                    JSONObject.toJSONString(msg));
                        }
                        break;
                    case RedisMessageType.END:
                        removeListenerThenSave(channel);
                        SimulationInfoDto end = objectMapper.readValue(simulationMessage.getValue(),
                                SimulationInfoDto.class);
                        log.info(StringUtils.format("结束接收{}数据：{}",  channel, JSONObject.toJSONString(end)));
                        TjCase endCase = caseMapper.selectById(caseId);
                        CaseTrajectoryDetailBo endCaseDetail = JSONObject.parseObject(endCase.getDetailInfo(),
                                CaseTrajectoryDetailBo.class);
                        Optional.ofNullable(end.getSceneDesc()).ifPresent(endCaseDetail::setSceneDesc);
                        Optional.ofNullable(end.getEvaluationVerify()).ifPresent(endCaseDetail::setEvaluationVerify);
                        Optional.ofNullable(end.getSceneForm()).ifPresent(endCaseDetail::setSceneForm);
                        endCase.setDetailInfo(JSONObject.toJSONString(endCaseDetail));
                        caseMapper.updateById(endCase);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeListenerThenSave(channel);
            }
        };
        this.addRunningChannel(channel, listener);
    }

    public void removeMessageListener(@Nullable MessageListener listener, String channel) {
        redisMessageListenerContainer.removeMessageListener(listener, new ChannelTopic(channel));
    }

    public void addRunningChannel(String channel, MessageListener listener) {
        if (this.runningChannel.containsKey(channel)) {
            return;
        }
        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));
        this.runningChannel.put(channel, new ChannelListener(channel, SecurityUtils.getUsername(),
                System.currentTimeMillis(), listener));
    }

    public void receiveData(String channel, SimulationTrajectoryDto data) {
        if (!this.runningChannel.containsKey(channel)) {
            return;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
        channelListener.refreshData(data);
    }

    public int getDataSize(String channel) {
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
            routeService.saveRouteFile(channelListener.getChannel(),
                    channelListener.getData());
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
                    try {
                        routeService.saveRouteFile(channelListener.getChannel(),
                                channelListener.getData());
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    iterator.remove();  // Removes the current element from the map
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static class ChannelListener<T> {
        private String channel;
        private String userName;
        private Long timestamp;
        private MessageListener listener;
        private List<T> data;

        public ChannelListener(String channel, String userName, Long timestamp, MessageListener listener) {
            this.channel = channel;
            this.userName = userName;
            this.timestamp = timestamp;
            this.listener = listener;
            this.data = new ArrayList<>();
        }

        public void refreshData(T data) {
            this.data.add(data);
            this.timestamp = System.currentTimeMillis();
            System.out.println(StringUtils.format("refresh {} {}", this.channel, this.timestamp));
        }

        public boolean isExpire() {
            return System.currentTimeMillis() - timestamp > 2000;
        }

        public int getCurrentSize() {
            return this.data.size();
        }

        public String getChannel() {
            return channel;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public MessageListener getListener() {
            return listener;
        }

        public void setListener(MessageListener listener) {
            this.listener = listener;
        }

        public List<T> getData() {
            return data;
        }
    }
}
