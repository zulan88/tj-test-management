package net.wanji.dataset.entity;


import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Transient;

/**
 * <p>
 * vehicle_rsu_info
 * </p>
 *
 * @author wj
 * @since 2022-10-31
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("vehicle_rsu_info")
public class VehicleRsuInfo extends ChargData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水号
     */
    @TableId(value = "record_id", type = IdType.INPUT)
    @ApiModelProperty("流水号")
    @ExcelProperty(value = "流水号")
    private String recordId;

    /**
     * 流水类型
     */
    @TableField("record_type")
    @ApiModelProperty("流水类型")

    private String recordType;
    @TableField("video")
    @ApiModelProperty("视频")
    private String video;

    /**
     * 机位编号
     */
    @TableField("er_id")
    @ApiModelProperty("机位编号")
    private String erId;

    /**
     * 稽核状态
     */
    @TableField("unlawful_mold")
    @ApiModelProperty("稽核状态")
    private String unlawfulMold;

    /**
     * 批次号
     */
    @TableField("hour_batch_no")
    @ApiModelProperty("批次号")
    private String hourBatchNo;

    /**
     * 行驶方向
     */
    @TableField("drive_dir")
    @ApiModelProperty("行驶方向")
    private Integer driveDir;

    /**
     * 设备类型
     */
    @TableField("dev_type")
    @ApiModelProperty("设备类型")
    private String devType;

    /**
     * 车头抓拍时间
     */
    @TableField(value = "head_pic_time")
    @ApiModelProperty("车头抓拍时间")
    //@JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime headPicTime;

    /**
     * 车侧抓拍时间
     */
    @TableField("side_pic_time")
    @ApiModelProperty("车侧抓拍时间")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sidePicTime;

    /**
     * 车尾抓拍时间
     */
    @TableField("tail_pic_time")
    @ApiModelProperty("车尾抓拍时间")
    @ExcelProperty(value = "车尾抓拍时间")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tailPicTime;

    /**
     * 车头物理车道编码
     */
    @TableField("head_lane_num")
    @ApiModelProperty("车头物理车道编码")
    private String headLaneNum;

    /**
     * 车侧物理车道编码
     */
    @TableField("side_lane_num")
    @ApiModelProperty("车侧物理车道编码")
    private String sideLaneNum;

    /**
     * 车尾物理车道编码
     */
    @TableField("tail_lane_num")
    @ApiModelProperty("车尾物理车道编码")
    private String tailLaneNum;

    /**
     * 车牌号码
     */
    @TableField("license_code")
    @ApiModelProperty("车牌号码")
    private String licenseCode;

    @TableField("license_abbre")
    @ApiModelProperty("车牌地址")
    private String licenseAbbre;

    /**
     * 车牌颜色
     */
    @TableField("license_color")
    @ApiModelProperty("车牌颜色")
    private Integer licenseColor;

    /**
     * 车辆速度
     */
    @TableField("vehicle_speed")
    @ApiModelProperty("车辆速度")
    @ExcelProperty(value = "车辆速度")
    private Integer vehicleSpeed;

    /**
     * 识别车型
     */
    @TableField("identify_type")
    @ApiModelProperty("识别车型")
    private String identifyType;

    @TableField(exist = false)
    private String checkCondition;


    /**
     * 车长
     */
    @TableField("vehicle_length")
    @ApiModelProperty("车长")
    private Integer vehicleLength;

    /**
     * 车宽
     */
    @TableField("vehicle_width")
    @ApiModelProperty("车宽")
    private Integer vehicleWidth;

    /**
     * 车高
     */
    @TableField("vehicle_height")
    @ApiModelProperty("车高")
    private Integer vehicleHeight;

    /**
     * 识别轴数
     */
    @TableField("vehicle_axle_count")
    @ApiModelProperty("识别轴数")
    private Integer vehicleAxleCount;

    /**
     * 轴型
     */
    @TableField("vehicle_axle_info")
    @ApiModelProperty("轴型")
    private String vehicleAxleInfo;

    /**
     * 危险品车辆标识
     */
    @TableField("dangerous_goods")
    @ApiModelProperty("危险品车辆标识")
    private Integer dangerousGoods;

    /**
     * 车头抓拍图片
     */
    @TableField("headImage")
    @ApiModelProperty("车头抓拍图片")
    private String headImage;

    /**
     * 车牌抓拍图片
     */
    @TableField("licenseImage")
    @ApiModelProperty("车牌抓拍图片")
    private String licenseImage;

    /**
     * 车侧抓拍图片
     */
    @TableField("sideImage")
    @ApiModelProperty("车侧抓拍图片")
    private String sideImage;

    /**
     * 车尾抓拍图片
     */
    @TableField("tailImage")
    @ApiModelProperty("车尾抓拍图片")
    private String tailImage;

    /**
     * 介质类型
     */
    @TableField("etc_media_type")
    @ApiModelProperty("介质类型")
    private String etcMediaType;

    /**
     * 介质编码
     */
    @TableField("etc_mac")
    @ApiModelProperty("介质编码")
    private String etcMac;

    /**
     * 合同序列号
     */
    @TableField("etc_contract_serial")
    @ApiModelProperty("合同序列号")
    private String etcContractSerial;

    /**
     * 交易时间
     */
    @TableField("etc_trans_time")
    @ApiModelProperty("交易时间")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime etcTransTime;

    /**
     * 交易结果
     */
    @TableField("etc_trade_result")
    @ApiModelProperty("交易结果")
    private String etcTradeResult;

    /**
     * 计费车牌号码
     */
    @TableField("etc_license")
    @ApiModelProperty("计费车牌号码")
    private String etcLicense;

    /**
     * 计费车牌颜色
     */
    @TableField("etc_license_color")
    @ApiModelProperty("计费车牌颜色")
    private String etcLicenseColor;

    /**
     * 计费车辆车型
     */
    @TableField("etc_veh_class")
    @ApiModelProperty("计费车辆车型")
    private String etcVehClass;

    /**
     * 车辆用户类型
     */
    @TableField("etc_veh_user_type")
    @ApiModelProperty("车辆用户类型")
    private String etcVehUserType;

    /**
     * 介质状态
     */
    @TableField("etc_status")
    @ApiModelProperty("介质状态")
    private String etcStatus;

    /**
     * 应用标识
     */
    @TableField("etc_valid_sign")
    @ApiModelProperty("应用标识")
    private String etcValidSign;

    /**
     * 入口车型
     */
    @TableField("etc_entry_class")
    @ApiModelProperty("入口车型")
    private String etcEntryClass;

    /**
     * 入口轴数
     */
    @TableField("etc_entry_axles")
    @ApiModelProperty("计费轴数")
    private String etcEntryAxles;

    /**
     * 入口时间
     */
    @TableField("etc_entry_time")
    @ApiModelProperty("入口时间")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime etcEntryTime;

    /**
     * 入口状态
     */
    @TableField("etc_general_state")
    @ApiModelProperty("入口状态")
    private String etcGeneralState;

    /**
     * 入口站点
     */
    @TableField("etc_toll_station")
    @ApiModelProperty("入口站点")
    private String etcTollStation;

    /**
     * 车辆交易结果
     */
    @TableField("veh_trade_result")
    @ApiModelProperty("车辆交易结果")
    private Integer vehTradeResult;

    /**
     * 介质关联个数
     */
    @TableField("relate_etc_count")
    @ApiModelProperty("介质关联个数")
    private Integer relateEtcCount;

    /**
     * 关联介质类型
     */
    @TableField("relate_etc_media_type")
    @ApiModelProperty("关联介质类型")
    private String relateEtcMediaType;

    /**
     * 关联介质编码
     */
    @TableField("relate_etc_mac")
    @ApiModelProperty("关联介质编码")
    private String relateEtcMac;

    /**
     * 关联合同序列号
     */
    @TableField("relate_etc_contract_serial")
    @ApiModelProperty("关联合同序列号")
    private String relateEtcContractSerial;

    /**
     * 置信度
     */
    @TableField("confidence_degree")
    @ApiModelProperty("置信度")
    private Integer confidenceDegree;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date startTime;

    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @TableField(exist = false)
    private Date endTime;
    @TableField(exist = false)
    @ApiModelProperty("站点名称")
    private String siteName;

    @ApiModelProperty("一卡多签")
    @TableField(exist = false)
    private List<OneMoreLine> oneMoreLines=new ArrayList<>();
    @ApiModelProperty("异常行为")
    @TableField(exist = false)
    private String errorStatus;

    @Transient
    @TableField(exist = false)
    private String provincialBoundaries;
    @Transient
    @TableField(exist = false)
    private String deviceCode;


    public String getErId() {
        if(erId!=null&&erId.equals(""))
            return null;
        else
            return erId;
    }
    public String getEtcLicense() {
        if(etcLicense!=null&&etcLicense.equals(""))
            return null;
        else
            return etcLicense;
    }
    public String getEtcMediaType() {
        if(etcMediaType!=null&&etcMediaType.equals(""))
            return null;
        else
            return etcMediaType;
    }
    public String getEtcVehClass() {
        if(etcVehClass!=null&&etcVehClass.equals(""))
            return null;
        else
            return etcVehClass;
    }

    public String getEtcEntryAxles() {
        if(etcEntryAxles!=null&&etcEntryAxles.equals(""))
            return null;
        else
            return etcEntryAxles;
    }
    public String getEtcMac() {
        if(etcMac!=null&&etcMac.equals(""))
            return null;
        else
            return etcMac;
    }
    public String getLicenseCode() {
        if(licenseCode!=null&&licenseCode.equals(""))
            return null;
        else
            return licenseCode;
    }

    public String getCheckCondition() {
        if(checkCondition!=null&&checkCondition.equals(""))
            return null;
        else
            return checkCondition;
    }
    public String getEtcTradeResult() {
        if(etcTradeResult!=null&&etcTradeResult.equals(""))
            return null;
        else
            return etcTradeResult;
    }

    public String getIdentifyType() {
        if(identifyType!=null&&identifyType.equals(""))
            return null;
        else
            return identifyType;
    }

}
