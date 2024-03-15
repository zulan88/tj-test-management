package net.wanji.business.entity.infity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.wanji.business.entity.common.Lifecycle;

import java.io.Serializable;

/**
 * @author hcy
 * @version 1.0
 * @className TjInfinityTaskRecord
 * @description TODO
 * @date 2024/3/14 10:09
 **/
@TableName(value = "tj_infinity_task_record")
@Data
public class TjInfinityTaskRecord extends Lifecycle implements Serializable {

  private static final long serialVersionUID = 3894895063602370083L;

  /**
   * 主键
   */
  @TableId(value = "ID", type = IdType.AUTO)
  private Integer id;

  /**
   * 任务ID
   */
  @TableField("TASK_ID")
  private Integer taskId;

  /**
   * 持续时间(s)
   */
  @TableField("DURATION_TIME")
  private Integer durationTime;
}
