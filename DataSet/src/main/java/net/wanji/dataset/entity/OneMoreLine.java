package net.wanji.dataset.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author : yangliang
 * @create 2022/11/15 16:20
 */
@Data
public class OneMoreLine {
    //passId
    private String passId;
    //通行介质
    @ApiModelProperty("通行介质")
    private String etcMediaType;
    //介质编码
    @ApiModelProperty("介质编码")
    private String etcMac;
    @ApiModelProperty("计费时间")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime billeDate;
}
