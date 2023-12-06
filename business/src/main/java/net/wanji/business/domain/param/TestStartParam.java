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

    private String channel;
    private int avNum;
    private int simulationNum;
    private int pedestrianNum;
    private List<ParticipantTrajectoryBo> participantTrajectories;
    private int frequency;

    public TestStartParam(String channel, int avNum, int simulationNum, int pedestrianNum,
                          List<ParticipantTrajectoryBo> participantTrajectories) {
        this.channel = channel;
        this.avNum = avNum;
        this.simulationNum = simulationNum;
        this.pedestrianNum = pedestrianNum;
        this.participantTrajectories = participantTrajectories;
        this.frequency = 10;
    }

}
