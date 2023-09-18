package net.wanji.business.domain.param;

import lombok.Data;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;

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
    private List<ParticipantTrajectoryBo> participantTrajectories;
    private int frequency;

    public TestStartParam(Integer caseId, String channel, int avNum, List<String> avNames,
                          int simulationNum, List<String> simulationNames,
                          int pedestrianNum, List<String> pedestrianNames,
                          List<ParticipantTrajectoryBo> participantTrajectories) {
        this.caseId = caseId;
        this.channel = channel;
        this.avNum = avNum;
        this.avNames = avNames;
        this.simulationNum = simulationNum;
        this.simulationNames = simulationNames;
        this.pedestrianNum = pedestrianNum;
        this.pedestrianNames = pedestrianNames;
        this.participantTrajectories = participantTrajectories;
        this.frequency = 10;
    }

}
