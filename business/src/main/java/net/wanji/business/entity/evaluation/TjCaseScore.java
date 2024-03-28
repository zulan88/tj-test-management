package net.wanji.business.entity.evaluation;

/**
 * @author hcy
 * @version 1.0
 * @className TjCaseScore
 * @description TODO
 * @date 2024/3/27 14:05
 **/

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@TableName("tj_case_score")
public class TjCaseScore {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;
  /**
   * '任务id'
   */
  @TableField()
  private int taskId;
  /**
   * 业务唯一ID，taskId_caseId
   */
  @TableField()
  private String businessId;
  /**
   * 测试记录ID
   */
  @TableField()
  private int recordId;
  /**
   * 测试开始时间戳
   */
  @TableField()
  private int startTime;
  /**
   * 测试结束时间戳
   */
  @TableField()
  private int endTime;
  /**
   * 测试总时长,单位秒
   */
  @TableField()
  private int testDuration;
  /**
   * 测试危险总时长占比 例子52%
   */
  @TableField()
  private float dangerTimeProportion;
  /**
   * 场景总得分
   */
  @TableField()
  private float allSenseScore;

  /**
   * 仅用于json转换
   * 详细得分
   */
  private List<TjCaseSceneScore> testScene;
}
