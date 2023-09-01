package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 测试任务-指标配置表
 * @TableName tj_task_dc
 */
@TableName(value ="tj_task_dc")
@Data
public class TjTaskDc implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 测试任务id
     */
    @TableField(value = "task_id")
    private Integer taskId;

    /**
     * 指标id
     */
    @TableField(value = "dc_id")
    private Integer dcId;

    /**
     * 权重
     */
    @TableField(value = "weight")
    private String weight;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}