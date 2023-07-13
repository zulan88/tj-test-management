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
     * 测试道路说明
     */
    @TableField("test_road_desc")
    private String testRoadDesc;

    /**
     * 测试场景说明
     */
    @TableField("test_scene_desc")
    private String testSceneDesc;

    /**
     * 测试方法说明
     */
    @TableField("test_method_desc")
    private String testMethodDesc;

    /**
     * 测试要求说明
     */
    @TableField("test_requirement_desc")
    private String testRequirementDesc;

    /**
     * 场景来源
     */
    @TableField("scene_source")
    private String sceneSource;

    /**
     * 图片路径
     */
    @TableField("img_url")
    private String imgUrl;

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


}
