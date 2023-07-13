package net.wanji.dataset.vo;

import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : yangliang
 * @create 2022/12/6 23:17
 */
@Data
public class MonitorTrafficFlowStatistics {
    private String startTime;//开始时间
    private String endTime;//结束时间
    private String provincialBoundaries;//省界
    private Long dailyTrafficFlowExport;//日车流出口
    private Long dailyTrafficFlowEntrance;//日车流入口
    private Long dailyAbnormalTrafficFlowExp;//日车流异常出口
    private Long dailyAbnormalTrafficFlowEnt;//日车流异常入口
    private String ringRatioExp;//环比出口
    private String ringRatioEnt;//环比入口
    private boolean statusExp;//出口 true为下降，false上升
    private boolean statusEnt;//入口 true为下降，false上升
}
