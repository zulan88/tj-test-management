package net.wanji.business.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String lockKey, String name, long expireTime) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, name, expireTime, TimeUnit.MILLISECONDS);
        return result != null && result;
    }

    public boolean renewLock(String lockKey, String name, long expireTime) {
        String value = stringRedisTemplate.opsForValue().get(lockKey);
        if (value != null && value.equals(name)) {
            stringRedisTemplate.expire(lockKey, expireTime, TimeUnit.MILLISECONDS);
            return true;
        }
        return false;
    }

    public void releaseLock(String lockKey, String requestId) {
        String value = stringRedisTemplate.opsForValue().get(lockKey);
        if (value != null && value.equals(requestId)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

}
