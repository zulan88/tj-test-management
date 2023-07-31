package net.wanji.business.domain.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:04
 * @Descriptoin:
 */
@Data
public class VehicleTrajectoryBo {
    /**
     * 车辆ID
     */
    private String id;
    /**
     * 类型(主车:main; 从车:slave)
     */
    private String type;
    /**
     * 角色（AV：AV；MV-实车：MV_REAL；MV-仿真车：MV_SIMULATION；SP：SP）
     */
    private String role;
    /**
     * 点位详情
     */
    private List<TrajectoryDetailBo> trajectory;
    /**
     * 路线
     */
    private List<Map<String, Double>> route;
    /**
     * 时长
     */
    private String duration;
}
