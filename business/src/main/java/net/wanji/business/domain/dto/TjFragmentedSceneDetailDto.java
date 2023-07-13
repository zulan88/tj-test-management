package net.wanji.business.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/28 14:05
 * @Descriptoin:
 */
@Data
public class TjFragmentedSceneDetailDto {

    private Integer id;

    /**
     * 片段式场景id
     */
    @NotNull(message = "请确认场景")
    private Integer fragmentedSceneId;

    /**
     * 测试道路说明
     */
    @NotBlank(message = "请填写测试道路说明")
    private String testRoadDesc;

    /**
     * 测试场景说明
     */
    @NotBlank(message = "请填写测试场景说明")
    private String testSceneDesc;

    /**
     * 测试方法说明
     */
    @NotBlank(message = "请填写测试方法说明")
    private String testMethodDesc;

    /**
     * 测试要求说明
     */
    @NotBlank(message = "请填写测试要求说明")
    private String testRequirementDesc;

    /**
     * 场景来源
     */
    @NotBlank(message = "请填写场景来源")
    private String sceneSource;

    /**
     * 图片路径
     */
    @NotBlank(message = "请上传图片")
    private String imgUrl;

    /**
     * 标签
     */
    @NotEmpty(message = "请选择标签")
    private List<String> labelList;

    /**
     * 轨迹信息
     */
    @NotNull(message = "请填写轨迹信息")
    private Map trajectoryJson;
}
