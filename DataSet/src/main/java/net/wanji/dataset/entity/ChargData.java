package net.wanji.dataset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : yangliang
 * @create 2022/11/3 13:47
 */
@Data
public class ChargData {
    @ApiModelProperty("计费PASSID")
    @TableField(exist = false)
    private String chargId;
    @ApiModelProperty("通行介质")
    @TableField(exist = false)
    private String trafficMedium;
    @ApiModelProperty("计费时间")
    @TableField(exist = false)
    private String billingTime;
    @ApiModelProperty("应收金额")
    @TableField(exist = false)
    private String amountReceivable;
    @ApiModelProperty("优惠金额")
    @TableField(exist = false)
    private String discountAmount;
    @ApiModelProperty("计费金额")
    @TableField(exist = false)
    private String billingAmount;
    @ApiModelProperty("特情码")
    @TableField(exist = false)
    private String secretCode;
    @ApiModelProperty("交易结果")
    @TableField(exist = false)
    private String transactStatus;

}
