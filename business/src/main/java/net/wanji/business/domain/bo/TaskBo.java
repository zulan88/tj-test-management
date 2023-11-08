package net.wanji.business.domain.bo;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.TjTaskDc;

import javax.validation.constraints.NotNull;

/**
 * @author: guowenhao
 * @date: 2023/8/31 19:46
 * @description:
 */
@ApiModel(value = "TaskBo", description = "保存测试任务实体类")
@Data
public class TaskBo {

    @ApiModelProperty(value = "节点")
    @NotNull(message = "请确认当前节点")
    private Integer processNode;

    @ApiModelProperty(value = "任务ID（创建任务信息）")
    private Integer id;

    @ApiModelProperty(value = "委托单位（创建任务信息）")
    private String client;

    @ApiModelProperty(value = "委托人（创建任务信息）")
    private String consigner;

    @ApiModelProperty(value = "联系人（创建任务信息）")
    private String contract;

    @ApiModelProperty(value = "测试类型（创建任务信息）")
    private String testType;

    @ApiModelProperty(value = "测试设备ID集合（创建任务信息）")
    private List<Integer> avDeviceIds;

    @ApiModelProperty(value = "测试用例ID集合（选择测试用例）")
    private List<Integer> caseIds;



    @ApiModelProperty(value = "任务描述")
    private List<TjTaskDataConfig> dataConfigs;

    @ApiModelProperty(value = "任务指标")
    private List<TjTaskDc> diadynamicCriterias;
}
