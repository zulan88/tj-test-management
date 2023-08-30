package net.wanji.business.domain.param;

import lombok.Data;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/25 17:28
 * @Descriptoin:
 */
@Data
public class TestStartParam {

    private Integer caseId;

    private String channel;

    private int avNum;
    private List<String> avNames;
    private int simulationNum;
    private List<String> simulationNames;
    private int pedestrianNum;
    private List<String> pedestrianNames;
    private int frequency;

    public TestStartParam(int avNum, List<String> avNames,
                          int simulationNum, List<String> simulationNames,
                          int pedestrianNum, List<String> pedestrianNames) {
        this.avNum = avNum;
        this.avNames = avNames;
        this.simulationNum = simulationNum;
        this.simulationNames = simulationNames;
        this.pedestrianNum = pedestrianNum;
        this.pedestrianNames = pedestrianNames;
        this.frequency = 10;
    }

}
