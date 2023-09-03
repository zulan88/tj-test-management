package net.wanji.common.common;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/14 17:16
 * @Descriptoin:
 */

public class SimulationMessage {
    /**
     * 类型（1：详情SimulationInfoDto.class；2：轨迹SimulationTrajectoryDto.class; 3.评分）
     */
    private String type;
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
