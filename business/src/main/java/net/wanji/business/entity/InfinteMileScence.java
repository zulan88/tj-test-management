package net.wanji.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author wj
 * @since 2024-02-23
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("infinte_mile_scence")
public class InfinteMileScence implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 场景名称
     */
    @TableField("name")
    private String name;

    /**
     * 场景编号
     */
    @TableField("view_id")
    private String viewId;

    /**
     * 测试难度 0-简单 1-中等 2-困难
     */
    @TableField("difficulty")
    private Integer difficulty;

    /**
     * 地图id
     */
    @TableField("map_id")
    private Integer mapId;

    /**
     * 元素
     */
    @TableField("element")
    private String element;

    /**
     * 交通流
     */
    @TableField("traffic_flow")
    private String trafficFlow;

    /**
     * 场地切片
     */
    @TableField("site_slice")
    private String siteSlice;

    /**
     * 场景状态
     */
    @TableField("status")
    private Integer status;

    @TableField("create_date")
    private LocalDateTime createDate;

    @TableField("update_date")
    private LocalDateTime updateDate;


}
