package net.wanji.business.component;

import lombok.extern.slf4j.Slf4j;
import net.wanji.common.utils.StringUtils;
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
@Slf4j
public class DeviceStateToRedis {
  public static final String DEVICE_STATE_PREFIX = "deviceState";
  public static final String DEVICE_READY_STATE_PREFIX = "deviceReadyState";

  private final RedisTemplate<String, Integer> redisTemplate;

  public DeviceStateToRedis(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void save(Integer deviceId, Integer state, String prefix, String suffix) {
    redisTemplate.opsForValue().set(getKey(deviceId, prefix, suffix), state,
        Duration.of(2, ChronoUnit.SECONDS));
  }

  public void save(Integer deviceId, String prefix, String suffix) {
    save(deviceId, 1, prefix, suffix);
  }

  public Integer query(Integer deviceId, String prefix, String suffix) {
    return redisTemplate.opsForValue().get(getKey(deviceId, prefix, suffix));
  }

  private static String getKey(Integer deviceId, String prefix, String suffix) {
    return prefix + "_" + deviceId + "_" + suffix;
  }
}
