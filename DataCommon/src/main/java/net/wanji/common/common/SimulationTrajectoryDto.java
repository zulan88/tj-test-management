package net.wanji.common.common;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 9:40
 * @Descriptoin:
 */
public class SimulationTrajectoryDto {

    /**
     * 时间戳类型（创建时间：CREATE_TIME）
     */
    private String timestampType;

    /**
     * 实际值（TrajectoryValueDto.class）
     */
    private String value;

    /**
     * 时间戳
     */
    private Long timestamp;

    public String getTimestampType() {
        return timestampType;
    }

    public void setTimestampType(String timestampType) {
        this.timestampType = timestampType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
