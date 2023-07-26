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
     * 源文件存储路径
     */
    @NotBlank(message = "请上传资源文件")
    private String filePath;

    /**
     * 示意图存储路径
     */
    @NotBlank(message = "请上传缩略图")
    private String imgPath;

    /**
     * 自定义字段1（地图：道路类型；）
     */
    private String attribute1;

    /**
     * 自定义字段2（地图：道路属性（单双向）；）
     */
    private String attribute2;

    /**
     * 自定义字段3（地图：车道数；）
     */
    private String attribute3;

    /**
     * 自定义字段4（地图：geojson文件；）
     */
    private String attribute4;

    /**
     * 自定义字段5
     */
    private String attribute5;

}
