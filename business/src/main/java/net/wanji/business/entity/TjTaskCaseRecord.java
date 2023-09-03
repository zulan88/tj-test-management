package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 
 * @TableName tj_task_case_record
 */
@TableName(value ="tj_task_case_record")
@Data
public class TjTaskCaseRecord implements Serializable {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务ID
     */
    private Integer taskId;

    /**
     * 用例ID
     */
    private Integer caseId;

    /**
     * 点位详情
     */
    private String detailInfo;

    /**
     * 轨迹文件
     */
    private String routeFile;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 开始时间
     */
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}