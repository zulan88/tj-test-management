package net.wanji.business.schedule;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.component.CountDown;
import net.wanji.business.component.PathwayPoints;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.CountDownDto;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.framework.manager.AsyncManager;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 18:58
 * @Descriptoin:
 */
@Data
public class RealPlaybackDomain {

    private List<ScheduledFuture<?>> future;
    private String key;
    private List<RealTestTrajectoryDto> realTestTrajectories;
    private boolean running;
    private int index;
    private int length;

    public RealPlaybackDomain(String key, List<RealTestTrajectoryDto> realTestTrajectories) {
        this.key = key;
        this.realTestTrajectories = realTestTrajectories;
        this.running = true;
        this.index = 0;
        this.length = realTestTrajectories.stream().map(RealTestTrajectoryDto::getData)
                .mapToInt(List::size)
                .max()
                .orElse(0);
        this.future = new ArrayList<>(realTestTrajectories.size());
        for (RealTestTrajectoryDto realTestTrajectory : realTestTrajectories) {
            // 实际轨迹
            List<SimulationTrajectoryDto> trajectories = realTestTrajectory.getData();
            List<List<TrajectoryValueDto>> actualTrajectories = trajectories.stream().map(SimulationTrajectoryDto::getValue)
                    .collect(Collectors.toList());
            // 所有点位
            List<TrajectoryDetailBo> points = StringUtils.isEmpty(realTestTrajectory.getPoints()) ? new ArrayList<>()
                    : JSONObject.parseArray(realTestTrajectory.getPoints(), TrajectoryDetailBo.class);
            // 主车全程、剩余时间、到达时间
            ArrayList<Point2D.Double> doubles = new ArrayList<>();
            for (TrajectoryDetailBo trajectoryDetailBo : points) {
                String[] pos = trajectoryDetailBo.getPosition().split(",");
                doubles.add(new Point2D.Double(Double.parseDouble(pos[0]), Double.parseDouble(pos[1])));
            }
            CountDown countDown = new CountDown(doubles);
            // 主车途径点提示
            PathwayPoints pathwayPoints = new PathwayPoints(points);
            // 仿真车轨迹
            List<TrajectoryValueDto> mainSimuTrajectories = realTestTrajectory.getMainSimuTrajectories();
            // wskey
            String wsKey = key.concat("_").concat(realTestTrajectory.getChannel());
            Map<String, Object> realMap = new HashMap<>();
            ScheduledFuture<?> scheduledFuture = AsyncManager.me().execute(() -> {
                // send data
                try {
                    if (!running) {
                        return;
                    }
                    if (index >= length) {
                        if (realTestTrajectory.isMain()) {
                            RealWebsocketMessage endMsg = new RealWebsocketMessage(RedisMessageType.END, null,
                                    null, "00:00");
                            WebSocketManage.sendInfo(wsKey, JSONObject.toJSONString(endMsg));
                            RealPlaybackSchedule.stopSendingData(key);
                            return;
                        }
                    }
                    String duration = DateUtils.secondsToDuration(
                            (int) Math.floor((double) (length - index) / 10));
                    List<TrajectoryValueDto> data = actualTrajectories.get(index >= actualTrajectories.size() ? actualTrajectories.size() - 1 : index);
                    if (realTestTrajectory.isMain()) {
                        if (CollectionUtils.isNotEmpty(data)) {
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
                            PathwayPoints nearestPoint = pathwayPoints.findNearestPoint(data.get(0).getLongitude(),
                                    data.get(0).getLatitude());
                            Map<String, Object> tipsMap = new HashMap<>();
                            if (nearestPoint.hasTips()) {
                                tipsMap.put("name", realTestTrajectory.getName());
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
                        }
                        RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, realMap, data, duration);
                        WebSocketManage.sendInfo(wsKey, JSONObject.toJSONString(msg));
                    } else {
                        index ++;
                        RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, null, data, duration);
                        WebSocketManage.sendInfo(wsKey, JSONObject.toJSONString(msg));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 100);
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

    public synchronized void stopSendingData() throws BusinessException, IOException {
        this.validFuture();
        this.running = false;
        for (ScheduledFuture<?> scheduledFuture : this.future) {
            scheduledFuture.cancel(true);
        }
        WebSocketManage.remove(this.key);
    }

    private void validFuture() throws BusinessException {
        if (CollectionUtils.isEmpty(this.future)) {
            throw new BusinessException("任务不存在");
        }
    }

}
