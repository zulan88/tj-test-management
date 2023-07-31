package net.wanji.business.domain.dto;

import lombok.Data;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.QueryGroup;
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
    @NotNull(message = "请确认场景", groups = {QueryGroup.class, InsertGroup.class, UpdateGroup.class})
    private Integer fragmentedSceneId;

    /**
     * 子场景编号
     */
    private String number;

    /**
     * 道路走向
     */
    @NotBlank(message = "请选择道路属性", groups = {InsertGroup.class, UpdateGroup.class})
    private String roadWay;

    /**
     * 车道数
     */
    @NotNull(message = "请选择车道数", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer laneNum;

    /**
     * 地图
     */
    @NotNull(message = "请选择地图", groups = {InsertGroup.class, UpdateGroup.class})
    private Integer resourcesDetailId;

    /**
     * 测试场景说明
     */
    @NotBlank(message = "请填写测试场景说明", groups = {InsertGroup.class, UpdateGroup.class})
    private String testSceneDesc;

    /**
     * 测试方法说明
     */
    @NotBlank(message = "请填写测试方法说明", groups = {InsertGroup.class, UpdateGroup.class})
    private String testMethodDesc;

    /**
     * 测试要求说明
     */
    @NotBlank(message = "请填写测试要求说明", groups = {InsertGroup.class, UpdateGroup.class})
    private String testRequirementDesc;

    /**
     * 场景来源
     */
    @NotBlank(message = "请填写场景来源", groups = {InsertGroup.class, UpdateGroup.class})
    private String sceneSource;

    /**
     * 图片路径
     */
    @NotBlank(message = "请上传图片", groups = {InsertGroup.class, UpdateGroup.class})
    private String imgUrl;

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
     * 交通流状态
     */
    @NotBlank(message = "请选择交通流状态", groups = {InsertGroup.class, UpdateGroup.class})
    private String trafficFlowStatus;

    /**
     * 场景复杂度
     */
    @NotBlank(message = "请选择场景复杂度", groups = {InsertGroup.class, UpdateGroup.class})
    private String sceneComplexity;

    /**
     * 场景类型（字典：scene_type）
     */
    @NotBlank(message = "请选择场景类型", groups = {InsertGroup.class, UpdateGroup.class})
    private String sceneType;

    /**
     * 收藏状态（未收藏：0；已收藏：1；）
     */
    private Integer collectStatus;

    /**
     * 标签
     */
    @NotEmpty(message = "请选择标签", groups = {InsertGroup.class, UpdateGroup.class})
    private List<String> labelList;

    /**
     * 轨迹信息
     */
    @NotNull(message = "请填写轨迹信息", groups = {InsertGroup.class, UpdateGroup.class})
    private Map trajectoryJson;
}
