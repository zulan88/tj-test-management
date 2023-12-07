package net.wanji.approve.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeviceListVo {

    String type;

    List<TjDeviceDetailVo> tjDeviceDetailVos;

}
