package net.wanji.business.domain.dto;

import lombok.Data;
import net.wanji.business.common.Constants.BatchGroup;
import net.wanji.business.common.Constants.DeleteGroup;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.QueryGroup;
import net.wanji.business.common.Constants.UpdateGroup;
import net.wanji.business.entity.TjCasePartConfig;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 13:45
 * @Descriptoin:
 */
@Data
public class TjCaseDto {

    @NotNull(message = "请选择页码", groups = QueryGroup.class)
    private Integer pageNum;

    @NotNull(message = "请选择页大小", groups = QueryGroup.class)
    private Integer pageSize;

    @NotNull(message = "请选择一条数据", groups = {UpdateGroup.class, DeleteGroup.class})
    private Integer id;

    private Integer sceneDetailId;

    @NotBlank(message = "请选择所属文件夹", groups = {QueryGroup.class, InsertGroup.class})
    private String treeId;

    private String caseNumber;

    private String label;

    private String status;

    @NotEmpty(message = "请选择至少一条数据", groups = {BatchGroup.class})
    private List<Integer> ids;

    @NotNull(message = "请配置角色", groups = {InsertGroup.class, UpdateGroup.class})
    private Map<String, List<TjCasePartConfig>> partConfig;
}
