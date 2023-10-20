package net.wanji.business.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author: guanyuduo
 * @date: 2023/10/19 14:24
 * @descriptoin:
 */
@ApiModel("用例查询请求体")
@Data
public class CaseQueryDto {

    @ApiModelProperty(value = "页码", example = "1")
    @NotNull(message = "请选择页码")
    private Integer pageNum;

    @ApiModelProperty(value = "页大小", example = "10")
    @NotNull(message = "请选择页大小")
    private Integer pageSize;

    @ApiModelProperty(value = "用例id", example = "276")
    private Integer id;

    @ApiModelProperty(value = "用例文件夹", example = "1")
    @NotBlank(message = "请选择所属文件夹")
    private String treeId;

    @ApiModelProperty(value = "用例编号", example = "CASE20230925000")
    private String caseNumber;

    @ApiModelProperty(value = "用例状态", example = "1")
    private String status;

    @ApiModelProperty(value = "开始时间", example = "2023-10-19 18:40:55")
    private String startTime;

    @ApiModelProperty(value = "结束时间", example = "2024-10-19 18:40:55")
    private String endTime;

    @ApiModelProperty(value = "场景分类", example = "40")
    private Integer label;
}
