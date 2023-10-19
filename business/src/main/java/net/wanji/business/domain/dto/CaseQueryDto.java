package net.wanji.business.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.QueryGroup;

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

    @ApiModelProperty("页码")
    @NotNull(message = "请选择页码", groups = QueryGroup.class)
    private Integer pageNum;

    @ApiModelProperty("页大小")
    @NotNull(message = "请选择页大小", groups = QueryGroup.class)
    private Integer pageSize;

    @ApiModelProperty("用例文件夹")
    @NotBlank(message = "请选择所属文件夹", groups = {QueryGroup.class, InsertGroup.class})
    private String treeId;

    @ApiModelProperty("用例编号")
    private String caseNumber;

    @ApiModelProperty("用例状态")
    private Integer status;

    @ApiModelProperty("开始时间")
    private String startTime;

    @ApiModelProperty("结束时间")
    private String endTime;

    @ApiModelProperty("场景分类")
    private Integer label;
}
