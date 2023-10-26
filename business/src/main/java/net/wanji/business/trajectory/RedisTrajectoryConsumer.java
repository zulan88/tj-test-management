package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.domain.WebsocketMessage;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.service.RouteService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.SimulationMessage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 9:28
 * @Descriptoin:
 */
@Component
public class RedisTrajectoryConsumer {

    private static final Logger log = LoggerFactory.getLogger("business");

    private final ConcurrentHashMap<String, ChannelListener<SimulationTrajectoryDto>> runningChannel = new ConcurrentHashMap<>();

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public RedisTrajectoryConsumer(RedisMessageListenerContainer redisMessageListenerContainer) {
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
     * @param tjCase        用例
     * @param participantId 指定车辆id（只有在发送ws时筛选）
     * @return
     */
    public void subscribeAndSend(CaseInfoBo caseInfoBo, String participantId) {
        CaseTrajectoryDetailBo originalTrajectory = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        // 添加监听器
        this.addRunningChannel(caseInfoBo.getId(), participantId, originalTrajectory);
    }

    /**
     * 添加监听器
     *
     * @param caseId
     * @param channel
     * @param participantId
     * @param originalTrajectory
     */
    public void addRunningChannel(Integer caseId,
                                  String participantId,
                                  CaseTrajectoryDetailBo originalTrajectory) {
        String channel = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(caseId),
                WebSocketManage.SIMULATION, null);
        if (this.runningChannel.containsKey(channel)) {
            removeListener(channel, false);
        }
        MessageListener listener = createListener(channel, caseId, participantId, originalTrajectory);
        this.runningChannel.put(channel, new ChannelListener(caseId, channel, SecurityUtils.getUsername(),
                System.currentTimeMillis(), listener));
        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));
    }


    /**
     * 创建监听器
     *
     * @param caseId             用例ID
     * @param caseNumber         用例编号
     * @param participantId      参与者ID
     * @param originalTrajectory 原点位结构
     * @return
     */
    public MessageListener createListener(String channel,
                                          Integer caseId,
                                          String participantId,
                                          CaseTrajectoryDetailBo originalTrajectory) {
        ObjectMapper objectMapper = new ObjectMapper();
        String methodLog = StringUtils.format("{}仿真验证 - ", caseId);
        return (message, pattern) -> {
            try {
                // 解析消息
                SimulationMessage simulationMessage = objectMapper.readValue(
                        message.toString(),
                        SimulationMessage.class);
                // 计时
                String duration = DateUtils.secondsToDuration(
                        (int) Math.ceil((double) (getDataSize(channel)) / 10));
                ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
                switch (simulationMessage.getType()) {
                    // 开始消息
                    case RedisMessageType.START:
                        log.info(StringUtils.format("{}开始", methodLog));
                        channelListener.start();
                        break;
                    // 轨迹消息
//                    case RedisMessageType.TRAJECTORY:
//                        if (!channelListener.started) {
//                            break;
//                        }
//                        SimulationTrajectoryDto simulationTrajectory = objectMapper.readValue(String.valueOf(simulationMessage.getValue()),
//                                SimulationTrajectoryDto.class);
//                        log.info(StringUtils.format("{}第{}帧轨迹：{}", methodLog, getDataSize(channel),
//                                JSONObject.toJSONString(simulationTrajectory)));
//                        if (CollectionUtils.isNotEmpty(simulationTrajectory.getValue())) {
//                            // 实际轨迹消息
//                            List<TrajectoryValueDto> data = simulationTrajectory.getValue();
//                            // 检查轨迹
//                            routeService.checkSimulaitonRoute(caseId, originalTrajectory, data);
//                            // 保存轨迹(本地)
//                            receiveData(channel, simulationTrajectory);
//                            // send ws
//                            WebsocketMessage msg = new WebsocketMessage(
//                                    RedisMessageType.TRAJECTORY,
//                                    duration,
//                                    routeService.filterParticipant(data, participantId));
//                            WebSocketManage.sendInfo(channel, JSONObject.toJSONString(msg));
//                        }
//                        break;
//                    // 结束消息
//                    case RedisMessageType.END:
//                        if (!channelListener.started) {
//                            break;
//                        }
//                        // 移除监听器
//                        removeListener(channel, true);
//                        // 解析消息
//                        CaseTrajectoryDetailBo end = objectMapper.readValue(String.valueOf(simulationMessage.getValue()),
//                                CaseTrajectoryDetailBo.class);
//                        log.info(StringUtils.format("{}结束：{}", methodLog, JSONObject.toJSONString(end)));
//                        // 更新数据
//                        Optional.ofNullable(end.getEvaluationVerify()).ifPresent(originalTrajectory::setEvaluationVerify);
//                        originalTrajectory.setDuration(duration);
//                        // 修改用例
//                        TjCase param = new TjCase();
//                        param.setId(caseId);
//                        param.setDetailInfo(JSONObject.toJSONString(originalTrajectory));
//                        int endSuccess = caseMapper.updateById(param);
//                        log.info(StringUtils.format("修改用例场景评价:{}", endSuccess > 0 ? "成功" : "失败"));
//                        // send ws
//                        WebsocketMessage msg = new WebsocketMessage(RedisMessageType.END, null, null);
//                        WebSocketManage.sendInfo(channel, JSONObject.toJSONString(msg));
//                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                removeListener(channel, true);
            }
        };
    }

    /**
     * 接收数据
     *
     * @param channel
     * @param data
     */
    public void receiveData(String key, SimulationTrajectoryDto data) {
        if (!this.runningChannel.containsKey(key)) {
            return;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(key);
        channelListener.refreshData(data);
    }

    /**
     * 获取数据大小
     *
     * @param channel
     * @return
     */
    public synchronized int getDataSize(String key) {
        if (!this.runningChannel.containsKey(key)) {
            return 0;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(key);
        return channelListener.getCurrentSize();
    }

    /**
     * 移除监听器
     *
     * @param channel
     * @param save
     */
    public void removeListener(String channel, boolean save) {
        if (!this.runningChannel.containsKey(channel)) {
            return;
        }
        ChannelListener<SimulationTrajectoryDto> channelListener = this.runningChannel.get(channel);
        redisMessageListenerContainer.removeMessageListener(channelListener.getListener(), new ChannelTopic(channel));
        if (save) {
            try {
                routeService.saveRouteFile(channelListener.getCaseId(), channelListener.getData());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.runningChannel.remove(channel);
    }

    /**
     * 移除过期监听器
     */
    public void removeListeners() {
        try {
            Iterator<Map.Entry<String, ChannelListener<SimulationTrajectoryDto>>> iterator =
                    this.runningChannel.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ChannelListener<SimulationTrajectoryDto>> entry = iterator.next();
                ChannelListener<SimulationTrajectoryDto> channelListener = entry.getValue();
                if (channelListener.isExpire()) {
                    redisMessageListenerContainer.removeMessageListener(channelListener.getListener(),
                            new ChannelTopic(channelListener.getChannel()));
                    iterator.remove();  // Removes the current element from the map
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 监听器实体类
     *
     * @param <T>
     */
    public static class ChannelListener<T> implements MessageListener {
        private final Integer caseId;
        private final String channel;
        private boolean started;
        private final String userName;
        private Long timestamp;
        private final MessageListener listener;
        private final List<T> data;

        public ChannelListener(Integer caseId, String channel, String userName, Long timestamp,
                               MessageListener listener) {
            this.caseId = caseId;
            this.channel = channel;
            this.started = false;
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

        public Integer getCaseId() {
            return caseId;
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

        public boolean isStarted() {
            return started;
        }

        public void start() {
            this.started = true;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {

        }
    }

}
