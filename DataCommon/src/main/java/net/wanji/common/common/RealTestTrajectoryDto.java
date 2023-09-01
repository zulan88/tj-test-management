package net.wanji.common.common;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/31 19:25
 * @Descriptoin:
 */
public class RealTestTrajectoryDto {

    private String channel;

    private boolean main;

    private List<SimulationTrajectoryDto> data;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isMain() {
        return main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    public List<SimulationTrajectoryDto> getData() {
        return data;
    }

    public void setData(List<SimulationTrajectoryDto> data) {
        this.data = data;
    }
}
