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
     * 委托单位
     */
    @TableField(value = "client")
    private String client;

    /**
     * 委托人
     */
    @TableField(value = "consigner")
    private String consigner;

    /**
     * 测试类型（字典：test_type）
     */
    @TableField(value = "test_type")
    private String testType;

    /**
     * 任务编号
     */
    @TableField(value = "task_code")
    private String taskCode;

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

    /**
     * 流程节点（1：创建任务信息；2：选择测试用例；3：测试连续性配置；4：查看评价方案；5.待试验；6.已完成）
     */
    private Integer processNode;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}