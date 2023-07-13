package net.wanji.business.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/28 14:05
 * @Descriptoin:
 */
@Data
public class TjResourcesDetailDto {

    private Integer id;

    /**
     * 所属资源id
     */
    @NotNull(message = "请确认所属资源")
    private Integer resourcesId;

    /**
     * 名称
     */
    @NotBlank(message = "请填写名称")
    private String name;

    /**
     * 格式 opendrive；
     */
    @NotBlank(message = "请上传模型")
    private String format;

    /**
     * 源文件存储路径
     */
    @NotBlank(message = "请上传模型")
    private String filePath;

    /**
     * 示意图存储路径
     */
    @NotBlank(message = "请上传示意图")
    private String imgPath;

    /**
     * 自定义字段1
     */
    private String attribute1;

    /**
     * 自定义字段2
     */
    private String attribute2;

    /**
     * 自定义字段3
     */
    private String attribute3;

    /**
     * 自定义字段4
     */
    private String attribute4;

    /**
     * 自定义字段5
     */
    private String attribute5;

}
