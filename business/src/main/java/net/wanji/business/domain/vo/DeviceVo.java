package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjDevice;
import net.wanji.common.annotation.Excel;
import net.wanji.common.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 17:23
 * @Descriptoin:
 */
@Data
public class DeviceVo extends TjDevice {

    @Excel(name = "设备名称")
    private String deviceTypeName;
    @Excel(name = "状态名称")
    private String statusName;


}
