package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.wanji.common.annotation.Excel;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author hcy
 * @version 1.0
 * @className TjInfinityTask
 * @description TODO
 * @date 2024/3/11 10:59
 **/
@TableName(value = "tj_infinity_task")
@Data
public class TjInfinityTask implements Serializable {

  private static final long serialVersionUID = 1665544527646456087L;

  /**
   * 主键
   */
  @TableId(value = "ID", type = IdType.AUTO)
  private Integer id;

  /**
   * 流程单号
   */
  @TableField(value = "ORDER_NUMBER")
  private String orderNumber;

  // 排期开始时间

  /**
   * 计划测试时长
   */
  @TableField(value = "PLAN_TEST_TIME")
  private Long planTestTime;
  /**
   * 测试开始时间
   */
  @TableField(value = "TEST_START_TIME")
  private Date testStartTime;

  /**
   * 测试结束时间
   */
  @TableField(value = "TEST_END_TIME")
  private Data testEndTime;

  /**
   * 测试场景名称
   */
  @TableField(value = "CASE_ID")
  private String caseId;

  /**
   * 测试场景名称
   */
  @TableField(value = "CASE_NAME")
  private String caseName;

  /**
   * 被测试对象类型
   */
  @TableField(value = "TESTED_TYPE")
  private String testedType;
  /**
   * 被测试对象名称
   */
  @TableField(value = "TESTED_NAME")
  private String testedName;

  /**
   * 委托单位
   */
  @TableField(value = "ENTRUST_ORG")
  private String entrustOrg;
  /**
   * 委托人
   */
  @TableField(value = "ENTRUSTER")
  private String entruster;
  /**
   * 联系方式
   */
  @TableField(value = "ENTRUSTER_CONTACT")
  private String entrusterContact;
  /**
   * 委托信息
   */
  @TableField(value = "ENTRUST_CONTENT")
  private String entrustContent;

  /**
   * 任务状态（save：待提交；waiting：待测试；prepping: 准备中； running：进行中；finished：已完成；past_due：逾期）
   */
  @TableField(value = "STATUS")
  private String status;

  /**
   * 主车轨迹文件
   */
  @TableField(value = "main_plan_file")
  private String mainPlanFile;
  /**
   * 完整轨迹文件
   */
  @TableField(value = "route_file")
  private String routeFile;

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
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
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
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedDate;
}