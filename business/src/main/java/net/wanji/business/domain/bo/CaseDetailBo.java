package net.wanji.business.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:03
 * @Descriptoin:
 */
@Data
public class CaseDetailBo {
    private String sceneDesc;
    private String sceneForm;
    private String evaluationVerify;
    private List<VehicleTrajectoryBo> vehicleTrajectory;
}
