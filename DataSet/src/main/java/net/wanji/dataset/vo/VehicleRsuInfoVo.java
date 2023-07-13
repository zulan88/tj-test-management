package net.wanji.dataset.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : yangliang
 * @create 2022/11/15 11:10
 */
@Data
public class VehicleRsuInfoVo {
    private String recordId;

    private String siteName;

    private LocalDateTime headPicTime;



    private String licenseCode;

    private String identifyType;

    private Integer vehicleAxleCount;


    private String dangerousGoods;

}
