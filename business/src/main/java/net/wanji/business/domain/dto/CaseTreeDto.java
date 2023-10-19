package net.wanji.business.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author: guanyuduo
 * @date: 2023/10/19 13:58
 * @descriptoin:
 */
@ApiModel("用例树请求体")
@Data
public class CaseTreeDto {

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "测试类型")
    @NotEmpty(message = "请选择测试类型")
    private String type;

    @ApiModelProperty(value = "文件夹名称")
    @NotEmpty(message = "请输入文件夹名称")
    private String name;
}
