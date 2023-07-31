package net.wanji.business.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/28 9:05
 * @Descriptoin:
 */
@Data
public class SceneTrajectoryBo {
    private Integer fragmentedSceneId;
    private List<VehicleTrajectoryBo> vehicle;
    private List<VehicleTrajectoryBo> pedestrian;
    private List<Object> obstacle;
}
