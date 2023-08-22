package net.wanji.business.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:03
 * @Descriptoin:
 */
@Data
public class CaseTrajectoryDetailBo extends SceneTrajectoryBo {

    private static final long serialVersionUID = 1L;

    private String sceneDesc;
    private String sceneForm;
    private String evaluationVerify;
}
