package net.wanji.business.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:05
 * @Descriptoin:
 */
@Data
public class TrajectoryDetailBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String time;
    private Long frameId;
    private String position;
    private String lane;
    private Double speed;
    private String speedUnit;
    private boolean pass;
    private String reason = "等待校验";
}
