package net.wanji.business.domain.bo;

import lombok.Data;
import net.wanji.common.utils.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:04
 * @Descriptoin:
 */
@Data
public class ParticipantTrajectoryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参与者ID
     */
    private String id;
    /**
     * 参与者类型(主车:main; 从车:slave; 行人：pedestrian； 障碍物：obstacle)
     */
    private String type;
    /**
     * 模型（1-小客车；2-大货车；3-大巴车；4-行人；5-自行车；）
     */
    private Integer model;
    /**
     * 参与者名称
     */
    private String name;
    /**
     * 持续时长
     */
    private String duration;
    /**
     * 点位详情
     */
    private List<TrajectoryDetailBo> trajectory;
    /**
     * 路线
     */
    List<Map<String, Double>> route;

    Boolean isHide;

    public String getDuration() {
        return StringUtils.isEmpty(duration) ? "00:00" : duration;
    }
}
