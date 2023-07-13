package net.wanji.dataset.entity;

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
 * inspection_status
 * </p>
 *
 * @author wj
 * @since 2022-11-09
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("inspection_status")
public class InspectionStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水号
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private String recordId;

    /**
     * 用户id
     */
    @TableField("user_id")
    private String userId;

    /**
     * 操作时间
     */
    @TableField("update_time")
    private String updateTime;

    /**
     * 稽核状态
     */
    @TableField("unlawful_mold")
    private String unlawfulMold;

    /**
     * 审核状态 0:未审核,1:已审核
     */
    @TableField("check_condition")
    private String checkCondition;

    /**
     * 是否属实 0:属实,1:不实
     */
    @TableField("be_true")
    private String beTrue;

    /**
     * 置信度
     */
    @TableField("confidence_degree")
    private String confidenceDegree;


}
