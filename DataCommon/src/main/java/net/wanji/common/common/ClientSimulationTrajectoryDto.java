package net.wanji.common.common;

/**
 * @author: guanyuduo
 * @date: 2024/1/2 14:23
 * @descriptoin:
 */

public class ClientSimulationTrajectoryDto extends SimulationTrajectoryDto {

    private boolean isMain;

    private String source;

    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean main) {
        isMain = main;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
