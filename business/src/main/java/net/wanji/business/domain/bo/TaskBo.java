package net.wanji.business.domain.bo;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.TjTaskDc;

/**
 * @author: guowenhao
 * @date: 2023/8/31 19:46
 * @description:
 */
@ApiModel(value = "TaskBo", description = "测试任务")
@Data
public class TaskBo {

    @ApiModelProperty(value = "任务ID")
    private Integer id;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "任务描述")
    private String caseIds;

    @ApiModelProperty(value = "任务描述")
    private List<TjTaskDataConfig> dataConfigs;

    @ApiModelProperty(value = "任务指标")
    private List<TjTaskDc> diadynamicCriterias;
}
