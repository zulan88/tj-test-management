package net.wanji.business.component;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author glace
 * @version 1.0
 * @className DeviceStateToRedis
 * @description TODO
 * @date 2023/10/7 16:31
 **/
@Component
public class DeviceStateToRedis {
  public static final String DEVICE_STATE_PREFIX = "deviceState";
  public static final String DEVICE_READY_STATE_PREFIX = "deviceReadyState";

  private final RedisTemplate<String, Integer> redisTemplate;

  public DeviceStateToRedis(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void save(Integer deviceId, Integer state, String prefix) {
    redisTemplate.opsForValue().set(getKey(deviceId, prefix), state,
        Duration.of(1, ChronoUnit.SECONDS));
  }

  public void save(Integer deviceId, String prefix) {
    save(deviceId, 1, prefix);
  }

  public Integer query(Integer deviceId, String prefix) {
    return redisTemplate.opsForValue().get(getKey(deviceId, prefix));
  }

  private static String getKey(Integer deviceId, String prefix) {
    return prefix + "_" + deviceId;
  }
}
