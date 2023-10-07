package net.wanji.business.service.impl;

import net.wanji.business.annotion.DeviceReport;
import net.wanji.business.domain.dto.device.DeviceStateDto;
import net.wanji.business.service.DeviceReportService;
import org.springframework.stereotype.Service;

/**
 * @author glace
 * @version 1.0
 * @className DeviceStateReportImpl
 * @description TODO
 * @date 2023/10/7 10:24
 **/
@DeviceReport("0")
@Service
public class DeviceStateReportImpl implements DeviceReportService<DeviceStateDto> {
  @Override
  public void dataProcess(DeviceStateDto deviceStateDto) {
    
  }
}
