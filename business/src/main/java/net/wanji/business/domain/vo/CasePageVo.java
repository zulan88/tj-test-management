package net.wanji.business.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.common.annotation.Excel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: guanyuduo
 * @date: 2023/10/19 16:18
 * @descriptoin:
 */
@Api("测试用例分页实体类")
@Data
public class CasePageVo {

    @ApiModelProperty("主键")
    private Integer id;

    @ApiModelProperty("所属文件夹")
    private Integer treeId;

    @ApiModelProperty("用例编号")
    @Excel(name = "用例编号")
    private String caseNumber;

    @ApiModelProperty("标签")
    @JsonIgnore
    private String label;

    @ApiModelProperty("场景分类")
    private String sceneSort;

    @ApiModelProperty("角色配置")
    private String roleConfigSort;

    @ApiModelProperty("用例状态")
    private String status;

    @ApiModelProperty("用例状态名称")
    private String statusName;

    @ApiModelProperty("创建人")
    private String createdBy;

    @ApiModelProperty("创建日期")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @ApiModelProperty("修改人")
    private String updatedBy;

    @ApiModelProperty("修改日期")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedDate;

    @ApiModelProperty("角色配置")
    @JsonIgnore
    private List<TjCasePartConfig> partConfigs;
}
