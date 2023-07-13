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
public class TjResourcesDto {

    private Integer id;

    /**
     * 名称
     */
    @NotBlank(message = "名称不可为空")
    private String name;

    /**
     * 资源类型 map：地图；
     */
    @NotBlank(message = "请确认类型")
    private String type;

    /**
     * 状态 0：禁用；1：启用
     */
    @NotNull(message = "请确认启用状态")
    private Integer status;

    /**
     * 父级id
     */
    @NotNull(message = "请确认父级场景")
    private Integer parentId;

    /**
     * 级别
     */
    @NotNull(message = "请确认场景等级")
    private Integer level;

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
