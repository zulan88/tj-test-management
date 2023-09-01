package net.wanji.business.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 测试任务表
 * @TableName tj_task
 */
@TableName(value ="tj_task")
@Data
public class TjTask implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 测试任务名称
     */
    @TableField(value = "task_name")
    private String taskName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 测试用例数
     */
    @TableField(value = "case_count")
    private Integer caseCount;

    /**
     * 通过率
     */
    @TableField(value = "passing_rate")
    private String passingRate;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "start_time")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "end_time")
    private Date endTime;

    /**
     * 测试总时长
     */
    @TableField(value = "test_total_time")
    private String testTotalTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}