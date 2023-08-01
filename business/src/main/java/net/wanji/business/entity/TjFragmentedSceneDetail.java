package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 片段式场景定义
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tj_fragmented_scene_detail")
public class TjFragmentedSceneDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 片段式场景id
     */
    @TableField("fragmented_scene_id")
    private Integer fragmentedSceneId;

    /**
     * 道路渠化.车道类型
     * 子场景编号
     */
    @TableField("road_way_type")
    private String roadWayType;

    /**
     * 子场景编号
     */
    @TableField("number")
    private String number;

    /**
     * 道路渠化.车道类型
     * 车道数
     */
    @TableField("lane_num")
    private Integer laneNum;

    /**
     * 地图
     */
    @TableField("resources_detail_id")
    private Integer resourcesDetailId;

    /**
     * 场景描述
     */
    @TableField("scene_desc")
    private String testSceneDesc;

    /**
     * 场景来源
     */
    @TableField("scene_source")
    private String sceneSource;

    /**
     * 场景复杂度（字典：scene_complexity）
     */
    @TableField("scene_complexity")
    private String sceneComplexity;


    /**
     * 交通流状态（字典：traffic_flow_status）
     */
    @TableField("traffic_flow_status")
    private String trafficFlowStatus;

    /**
     * 危险指数
     */
    @TableField("hazardIndex")
    private Integer hazardIndex;

    /**
     * 道路类型(字典：road_type)
     */
    @TableField("road_type")
    private String roadType;

    /**
     * 路面状况（字典：road_condition）
     */
    @TableField("road_condition")
    private String roadCondition;

    /**
     * 天气（字典：weather）
     */
    @TableField("weather")
    private String weather;

    /**
     * 视频数据
     */
    @TableField("video_data_file_path")
    private String videoDataFilePath;

    /**
     * 雷达数据
     */
    @TableField("rada_data_file_path")
    private String radaDataFilePath;

    /**
     * GPS数据
     */
    @TableField("gps_data_file_path")
    private String gpsDataFilePath;

    /**
     * 行驶数据
     */
    @TableField("driver_data_file_path")
    private String driverDataFilePath;


    /**
     * 收藏状态（未收藏：0；已收藏：1；）
     */
    @TableField("collect_status")
    private Integer collectStatus;

    /**
     * 标签
     */
    @TableField("label")
    private String label;

    /**
     * 轨迹信息
     */
    @TableField("trajectory_info")
    private String trajectoryInfo;

    /**
     * 图片路径
     */
    @TableField("img_url")
    private String imgUrl;

    /**
     * 场景类型（字典：scene_type）
     */
    @TableField("scene_type")
    private String sceneType;
}
