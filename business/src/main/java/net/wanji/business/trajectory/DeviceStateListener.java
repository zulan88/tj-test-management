package net.wanji.business.trajectory;

import lombok.extern.slf4j.Slf4j;
import net.wanji.business.component.DeviceReportFactory;
import net.wanji.business.domain.dto.device.DeviceStateDto;
import net.wanji.business.service.DeviceReportService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author glace
 * @version 1.0
 * @className DeviceStateConsumer
 * @description TODO
 * @date 2023/10/7 14:24
 **/
@Component
@Slf4j
public class DeviceStateListener implements MessageListener {
  @Resource
  private RedisTemplate<String, String> redisTemplate;
  @Resource
  private DeviceReportFactory deviceReportFactory;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    Object object = redisTemplate.getValueSerializer()
        .deserialize(message.getBody());
    if (null == object) {
      if (log.isWarnEnabled()) {
        log.info("Report message is null!");
      }
      return;
    }
    DeviceStateDto deviceStateDto = (DeviceStateDto) object;
    DeviceReportService<Object> deviceReportService = deviceReportFactory.create(
        deviceStateDto.getType());
    deviceReportService.dataProcess(object);
  }
}
