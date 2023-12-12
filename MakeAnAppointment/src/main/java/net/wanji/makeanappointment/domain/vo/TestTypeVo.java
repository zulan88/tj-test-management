package net.wanji.makeanappointment.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * @ClassName TestTypeVo
 * @Description
 * @Author liruitao
 * @Date 2023-12-06
 * @Version 1.0
 **/
@Data
public class TestTypeVo {

    /**
     * 主键id
     */
    private Integer id;

    /**
     * 测试类型
     */
    private String testTypeName;

    /**
     * 测试类型描述
     */
    private String testTypeDescribe;

    /**
     * 测试类型图片
     */
    private String picture;

    /**
     * 状态（0-正常 1-删除）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新人
     */
    private String updateBy;

}
