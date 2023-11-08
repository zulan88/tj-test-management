package net.wanji.business.domain.dto.device;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.wanji.business.domain.dto.CaseQueryDto;

/**
 * @author: guanyuduo
 * @date: 2023/11/8 11:19
 * @descriptoin:
 */
@ApiModel("任务保存请求体")
@Data
public class TaskSaveDto {

    @ApiModelProperty(value = "任务ID")
    private Integer id;

    @ApiModelProperty(value = "节点", required = true)
    private Integer processNode;

    @ApiModelProperty(value = "用例查询请求体(节点=2，必传)")
    private CaseQueryDto caseQueryDto;
}
