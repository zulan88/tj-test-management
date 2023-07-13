package net.wanji.dataset.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
//import net.wanji.common.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class VehicleRsuInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty("流水号")
    @Excel(name = "流水号",orderNum = "1",width = 20)
    private String recordId;
    @ApiModelProperty("门架号")
    @Excel(name = "门架号",orderNum = "2",width = 20)
    private String erId;
    @ApiModelProperty("站点名称")
    @Excel(name = "站点名称",orderNum = "3",width = 20)
    private String siteName;
    @JsonFormat(pattern ="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8",shape = JsonFormat.Shape.STRING)
    @ApiModelProperty("车头抓拍时间")
    @Excel(name = "车头抓拍时间",orderNum = "4",width = 20)
    private LocalDateTime headPicTime;
    @ApiModelProperty("车牌号")
    @Excel(name = "车牌号",orderNum = "5",width = 20)
    private String licenseCode;
    @ApiModelProperty("识别车辆类型")
    @Excel(name = "识别车辆类型",orderNum = "6",width = 20)
    private String identifyType;
    @ApiModelProperty("车辆类型")
    @Excel(name = "车辆类型",orderNum = "7",width = 20)
    private String etcVehClass;
    @ApiModelProperty("识别轴数")
    @Excel(name = "识别轴数",orderNum = "8",width = 20)
    private Integer vehicleAxleCount;
    @ApiModelProperty("etc数")
    @Excel(name = "etc数",orderNum = "9",width = 20)
    private String etcEntryAxles;
    @ApiModelProperty("危险品车辆标识")
    @Excel(name = "危险品车辆标识",orderNum = "10",width = 20)
    private Integer dangerousGoods;
    @ApiModelProperty("车头抓拍图片")
    @Excel(name = "车头抓拍图片",orderNum = "11",type = 3,width=15,height = 30)
    private String headImage;
    @ApiModelProperty("车牌抓拍图片")
    @Excel(name = "车牌抓拍图片",orderNum = "12",type = 3,width=15,height = 30)
    private String licenseImage;
    @ApiModelProperty("车侧抓拍图片")
    @Excel(name = "车侧抓拍图片",orderNum = "13",type = 3,width=15,height = 30)
    private String sideImage;
    @ApiModelProperty("车尾抓拍图片")
    @Excel(name = "车尾抓拍图片",orderNum = "14",type = 3,width=15,height = 30)
    private String tailImage;
    @ApiModelProperty("车辆抓拍视频")
    private String video;

    private String userId;
    @ApiModelProperty("更新时间")
    @Excel(name = "更新时间",orderNum = "15",width=20)
    @JsonFormat(pattern ="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8",shape = JsonFormat.Shape.STRING)
    private String updateTime;
    @ApiModelProperty("检测来源")
    private String devType;
//0是未审核1是已审核
@ApiModelProperty("审核状态 0是未审核1是已审核")
@Excel(name = "审核状态 0是未审核1是已审核",orderNum = "15",width=20)
    private String checkCondition;

    private String beTrue;
    @ApiModelProperty("缺失频次")
    private Long MissingFrequency;
    @ApiModelProperty("id数组")
    private String[] recordIds;
    private  String etcMediaType;
    private String etcContractSerial;
    private String relateEtcContractSerial;
    private String relateEtcMac;
    private String relateEtcMediaType;
    @JsonFormat(pattern ="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8",shape = JsonFormat.Shape.STRING)
    private LocalDateTime etcTransTime;
    private String etcMac;
    private List<OneMoreLine> oneMoreLines;
    private String driveDir;
    private String vehTradeResult;
    @ApiModelProperty("置信度")
    private String confidenceDegree;
    @ApiModelProperty("计费车牌")
    private String etcLicense;
    @ApiModelProperty("稽核状态")
    private String unlawfulMold;

    public String getCheckCondition() {
        if(checkCondition==null)
            return "0";
        else
            return checkCondition;
    }
}
