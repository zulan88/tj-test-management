package net.wanji.business.schedule;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.WebsocketMessage;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.socket.realTest.RealWebSocketManage;
import net.wanji.socket.simulation.WebSocketManage;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 18:58
 * @Descriptoin:
 */
@Data
public class RealPlaybackDomain {

    private List<ScheduledFuture<?>> future;
    private String key;
    private Map<String, List<List<TrajectoryValueDto>>> data;
    private boolean running;

    public RealPlaybackDomain(ScheduledExecutorService executorService,
                              String key,
                              Map<String, List<List<TrajectoryValueDto>>> data) {
        this.key = key;
        this.data = data;
        this.running = true;
        this.future = new ArrayList<>(data.size());
        for (Entry<String, List<List<TrajectoryValueDto>>> trajectoryEntry : data.entrySet()) {
            ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() -> {
                // send data
                int index = 0;
                try {
                    if (!running) {
                        return;
                    }
                    if (index >= data.size()) {
                        PlaybackSchedule.stopSendingData(key);
                        return;
                    }
                    index++;
                    String countDown = DateUtils.secondsToDuration(
                            (int) Math.floor((double) (data.size() - index) / 10));
//                    RealWebsocketMessage message = new RealWebsocketMessage(countDown, data.get(index));
//                    RealWebSocketManage.sendInfo(trajectoryEntry.getKey(), JSONObject.toJSONString(message));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
            future.add(scheduledFuture);
        }


    }

    public void suspend() throws BusinessException {
        this.validFuture();
        if (!this.running) {
            throw new BusinessException("当前任务未处于运行状态");
        }
        this.running = false;
    }

    public void goOn() throws BusinessException {
        this.validFuture();
        this.running = true;
    }

    public synchronized void stopSendingData() throws BusinessException {
        this.validFuture();
        this.running = false;
        for (ScheduledFuture<?> scheduledFuture : this.future) {
            scheduledFuture.cancel(true);
        }
        RealWebSocketManage.close(this.key);
    }

    private void validFuture() throws BusinessException {
        if (ObjectUtils.isEmpty(this.future)) {
            throw new BusinessException("任务不存在");
        }
    }

}
