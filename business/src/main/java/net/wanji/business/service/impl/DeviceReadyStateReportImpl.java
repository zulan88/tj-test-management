package net.wanji.business.service.impl;

import net.wanji.business.annotion.DeviceReport;
import net.wanji.business.domain.dto.device.DeviceReadyStateDto;
import net.wanji.business.service.DeviceReportService;
import org.springframework.stereotype.Service;

/**
 * @author glace
 * @version 1.0
 * @className DeviceReadyStateReportImpl
 * @description TODO
 * @date 2023/10/7 10:24
 **/
@DeviceReport("1")
@Service
public class DeviceReadyStateReportImpl
    implements DeviceReportService<DeviceReadyStateDto> {
  @Override
  public void dataProcess(DeviceReadyStateDto deviceReadyStateDto) {

  }
}
