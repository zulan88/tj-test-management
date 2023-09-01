package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 测试任务-数据配置表
 * @TableName tj_task_data_config
 */
@TableName(value ="tj_task_data_config")
@Data
public class TjTaskDataConfig implements Serializable {
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
     * 类型：av,mvReal,mvSimulation,sp
     */
    @TableField(value = "type")
    private String type;

    /**
     * 参与者ID
     */
    @TableField(value = "participator_id")
    private String participatorId;

    /**
     * 参与者名称
     */
    @TableField(value = "participator_name")
    private String participatorName;

    /**
     * 绑定设备id
     */
    @TableField(value = "device_id")
    private String deviceId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}