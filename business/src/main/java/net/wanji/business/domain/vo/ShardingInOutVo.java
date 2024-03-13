package net.wanji.business.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author hcy
 * @version 1.0
 * @className ShardingInOutVo
 * @description TODO
 * @date 2024/3/12 11:07
 **/
@Data
@AllArgsConstructor
public class ShardingInOutVo implements Serializable {
  private static final long serialVersionUID = 4090979817563659549L;

  @ApiModelProperty(value = "任务ID",
      required = true,
      dataType = "Integer",
      position = 1)
  private Integer taskId;
  @ApiModelProperty(value = "场景ID",
      required = true,
      dataType = "Integer",
      position = 2)
  private Integer caseId;
  @ApiModelProperty(value = "参与者id",
      required = true,
      dataType = "String",
      position = 3)
  private String participantId;
  @ApiModelProperty(value = "参与者名称",
      required = true,
      dataType = "String",
      position = 4)
  private String participantName;
  @ApiModelProperty(value = "分片ID",
      required = true,
      dataType = "Integer",
      position = 5)
  private Integer shardingId;
  @ApiModelProperty(value = "分片交互状态，0：出，1：进",
      required = true,
      dataType = "Integer",
      position = 6)
  private int state;
}
