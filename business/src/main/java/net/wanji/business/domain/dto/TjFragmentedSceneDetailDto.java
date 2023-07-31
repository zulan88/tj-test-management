package net.wanji.business.domain.dto;

import lombok.Data;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.UpdateGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/28 14:05
 * @Descriptoin:
 */
@Data
public class TjFragmentedSceneDetailDto {

    private Integer id;

    /**
     * 片段式场景id
     */
    @NotNull(message = "请确认场景", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer fragmentedSceneId;

    /**
     * 道路渠化.车道类型（道路走向）
     */
    @NotBlank(message = "请选择道路属性", groups = {InsertGroup.class, UpdateGroup.class})
    private String roadWayType;

    /**
     * 道路渠化.车道类型
     */
    @NotNull(message = "请填写车道数", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer laneNum;

    /**
     * 地图
     */
    @NotNull(message = "请选择地图", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer resourcesDetailId;

    /**
     * 场景描述
     */
    @NotBlank(message = "请填写场景描述", groups = {InsertGroup.class, UpdateGroup.class})
    private String testSceneDesc;

    /**
     * 场景来源
     */
    @NotBlank(message = "请填写场景来源", groups = {InsertGroup.class, UpdateGroup.class})
    private String sceneSource;

    /**
     * 场景复杂度
     */
    @NotBlank(message = "请选择场景复杂度", groups = {InsertGroup.class, UpdateGroup.class})
    private String sceneComplexity;

    /**
     * 交通流状态
     */
    @NotBlank(message = "请选择交通流状态", groups = {InsertGroup.class, UpdateGroup.class})
    private String trafficFlowStatus;

    /**
     * 危险指数
     */
    @NotBlank(message = "请选择危险指数", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer hazardIndex;

    /**
     * 道路类型
     */
    @NotBlank(message = "请选择道路类型", groups = {InsertGroup.class, UpdateGroup.class})
    private String roadType;
    /**
     * 路面状况
     */
    @NotBlank(message = "请选择路面状况", groups = {InsertGroup.class, UpdateGroup.class})
    private String roadCondition;

    /**
     * 天气
     */
    @NotBlank(message = "请选择天气", groups = {InsertGroup.class, UpdateGroup.class})
    private String weather;

    /**
     * 视频数据
     */
    private String videoDataFilePath;

    /**
     * 雷达数据
     */
    private String radaDataFilePath;

    /**
     * GPS数据
     */
    private String gpsDataFilePath;

    /**
     * 行驶数据
     */
    private String driverDataFilePath;

    /**
     * 标签
     */
    @NotEmpty(message = "请选择标签", groups = {InsertGroup.class, UpdateGroup.class})
    private List<String> labelList;

    /**
     * 轨迹信息
     */
    private Map trajectoryJson;

    /**
     * 收藏状态（未收藏：0；已收藏：1；）
     */
    private Integer collectStatus;

}
