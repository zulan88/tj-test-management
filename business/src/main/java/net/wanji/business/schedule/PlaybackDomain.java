package net.wanji.business.schedule;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.domain.WebsocketMessage;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.socket.simulation.WebSocketManage;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 18:58
 * @Descriptoin:
 */
@Data
public class PlaybackDomain {

    private ScheduledFuture<?> future;
    private String id;
    private List<List<TrajectoryValueDto>> data;
    private int index;
    private boolean running;

    public PlaybackDomain(ScheduledExecutorService executorService, String id, List<List<TrajectoryValueDto>> data) {
        this.id = id;
        this.data = data;
        this.index = 0;
        this.running = true;
        this.future = executorService.scheduleAtFixedRate(() -> {
            // send data
            try {
                if (!running) {
                    return;
                }
                if (index >= data.size()) {
                    WebsocketMessage message = new WebsocketMessage(RedisMessageType.END, null, null);
                    WebSocketManage.sendInfo(id, JSONObject.toJSONString(message));
                    PlaybackSchedule.stopSendingData(id);
                    return;
                }
                String countDown = DateUtils.secondsToDuration(
                        (int) Math.floor((double) (data.size() - index) / 10));
                WebsocketMessage message = new WebsocketMessage(RedisMessageType.TRAJECTORY, countDown, data.get(index));
                WebSocketManage.sendInfo(id, JSONObject.toJSONString(message));
                index++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
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
        this.future.cancel(true);
        WebSocketManage.close(this.id);
    }

    private void validFuture() throws BusinessException {
        if (ObjectUtils.isEmpty(this.future)) {
            throw new BusinessException("任务不存在");
        }
    }

}
