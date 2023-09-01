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
 * 测试任务-用例详情表
 * @TableName tj_task_case
 */
@TableName(value ="tj_task_case")
@Data
public class TjTaskCase implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 测试任务id
     */
    @TableField(value = "task_id")
    private Integer taskId;

    /**
     * 用例id
     */
    @TableField(value = "case_id")
    private Integer caseId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 测试状态
     */
    @TableField(value = "status")
    private String status;

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