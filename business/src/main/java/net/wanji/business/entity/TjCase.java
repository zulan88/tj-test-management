package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.wanji.common.annotation.Excel;

/**
 * <p>
 * 
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tj_case")
public class TjCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;


    @TableField("test_type")
    private String testType;

    /**
     * 用例编号
     */
    @TableField("case_number")
    @Excel(name = "用例编号")
    private String caseNumber;

    /**
     * 标签
     */
    @TableField("label")
    @Excel(name = "标签")
    private String label;

    /**
     * 所属场景详情id
     */
    @TableField("scene_detail_id")
    private Integer sceneDetailId;

    /**
     * 所属资源详情id
     */
    @TableField("resources_detail_id")
    private Integer resourcesDetailId;

    /**
     * 测试目标
     */
    @TableField("test_target")
    @Excel(name = "测试目标")
    private String testTarget;

    /**
     * 评价对象
     */
    @TableField("eva_object")
    @Excel(name = "评价对象")
    private String evaObject;

    /**
     * 测试场景
     */
    @TableField("test_scene")
    @Excel(name = "测试场景")
    private String testScene;

    /**
     * 仿真点位详情
     */
    @TableField("detail_info")
    private String detailInfo;

    /**
     * 点位文件路径
     */
    @TableField("local_file")
    private String localFile;

    /**
     * topic
     */
    @TableField("topic")
    private String topic;

    /**
     * 轨迹文件
     */
    @TableField("route_file")
    private String routeFile;

    /**
     * 状态：待配置：0；待仿真1；仿真验证2；实车验证3；待入库4
     */
    @TableField("status")
    private String status;

    /**
     * 创建人
     */
    @TableField("created_by")
    @Excel(name = "创建人")
    private String createdBy;

    /**
     * 创建日期
     */
    @TableField("created_date")
    @Excel(name = "创建日期")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    /**
     * 修改人
     */
    @TableField("updated_by")
    @Excel(name = "修改人")
    private String updatedBy;

    /**
     * 修改日期
     */
    @TableField("updated_date")
    @Excel(name = "修改日期")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedDate;


}
