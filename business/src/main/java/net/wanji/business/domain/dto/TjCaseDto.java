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

    @NotBlank(message = "请选择测试类型", groups = {QueryGroup.class, InsertGroup.class})
    private String testType;

    private String caseNumber;

    private String label;

    @NotNull(message = "请确认所属场景", groups = {QueryGroup.class, InsertGroup.class})
    private Integer sceneDetailId;

    @NotBlank(message = "请输入测试目标", groups = {InsertGroup.class, UpdateGroup.class})
    private String testTarget;

    @NotBlank(message = "请输入测试对象", groups = {InsertGroup.class, UpdateGroup.class})
    private String evaObject;

    @NotBlank(message = "请输入测试场景", groups = {InsertGroup.class, UpdateGroup.class})
    private String testScene;

//    @NotBlank(message = "请输入topic", groups = UpdateGroup.class)
    private String topic;

//    @NotBlank(message = "请上传文件", groups = UpdateGroup.class)
    private String localFile;

    @NotNull(message = "请选择状态", groups = {QueryGroup.class})
    private Integer status;

    @NotEmpty(message = "请输入标签", groups = {InsertGroup.class, UpdateGroup.class})
    private List<String> labelList;

    @NotEmpty(message = "请选择至少一条数据", groups = {BatchGroup.class})
    private List<Integer> ids;

    @NotNull(message = "请配置角色", groups = {InsertGroup.class, UpdateGroup.class})
    private Map<String, List<TjCasePartConfig>> partConfig;
}
