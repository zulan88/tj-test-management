package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author hcy
 * @version 1.0
 * @className TjSceneScore
 * @description TODO
 * @date 2024/3/26 13:50
 **/
@TableName(value = "tj_scene_score")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TjSceneScore {
  /**
   * 主键
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  /**
   * 记录id
   */
  @TableField(value = "record_id")
  private String recordId;
  /**
   * 场景id
   */
  @TableField(value = "scene_id")
  private int senceID;
  /**
   * 1:已完成 0:测试中
   */
  @TableField(value = "scene_status")
  private int senceStatus;
  /**
   * 当senceStatus为1时发送得分情况
   * 例:50/100
   */
  @TableField(value = "scene_score")
  private String senceScore;
  /**
   * 安全性总分
   */
  @TableField(value = "safe_score")
  private int safeScore;
  /**
   * 安全性扣分项，没有扣分发送0
   */
  @TableField(value = "safe_minus_score")
  private int safeMinusScore;
  /**
   * 前向碰撞时间(TTC)
   */
  @TableField(value = "tcc")
  private int TTC;
  /**
   * 当前全程的平均速度
   */
  @TableField(value = "avg_speed_all")
  private Float avgSpeedAll;
  /**
   * 从当前场景开始的平均速度
   */
  @TableField(value = "avg_speed")
  private Float avgSpeed;
  /**
   * 效率性总分
   */
  @TableField(value = "efficiency_score")
  private int efficiencyScore;
  /**
   * 效率性扣分项，没有扣分发送0
   */
  @TableField(value = "efficiency_minus_score")
  private int efficiencyMinusScore;
  /**
   * 任务耗时
   */
  @TableField(value = "task_already_time")
  private int taskAlreadyTime;
  /**
   * 期望耗时
   */
  @TableField(value = "task_time")
  private int taskTime;
  /**
   * 舒适性总分
   */
  @TableField(value = "comfortable_score")
  private int comfortableScore;
  /**
   * 舒适性扣分项，没有扣分发送0
   */
  @TableField(value = "comfortable_minus_score")
  private int comfortableMinusScore;
  /**
   * 纵向加速度
   */
  @TableField(value = "vertical_a")
  private float verticalA;
  /**
   * 纵向加加速度
   */
  @TableField(value = "vertical_a_plus")
  private float verticalAPlus;
  /**
   * 横向加速度
   */
  @TableField(value = "horizontal_a")
  private float horizontalA;
  /**
   * 横向加加速度
   */
  @TableField(value = "horizontal_a_plus")
  private float horizontalAPlus;
  /**
   * 转弯加速度
   */
  @TableField(value = "turn_a")
  private float turnA;
  /**
   * 转弯加加速度
   */
  @TableField(value = "turn_a_plus")
  private float turnAPlus;
  /**
   * 就是你们传过来的taskId基于任务
   */
  @TableField(value = "task_id")
  private Integer taskId;
  @TableField(value = "case_id")
  private Integer caseId;
  /**
   * 也是你们传过来的
   */
  @TableField(value = "av_name")
  private String avName;

}
