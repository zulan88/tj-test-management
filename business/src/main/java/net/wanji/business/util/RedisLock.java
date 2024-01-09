package net.wanji.business.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String lockKey, String name) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, name, 30, TimeUnit.SECONDS);
        return result != null && result;
    }

    public boolean renewLock(String lockKey) {
        String value = stringRedisTemplate.opsForValue().get(lockKey);
        if (value != null) {
            stringRedisTemplate.expire(lockKey, 30, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    public void setUser(String lockKey, String name) {
        stringRedisTemplate.opsForValue().set(lockKey, name, 30, TimeUnit.MINUTES);
    }

    public String getUser(String lockKey) {
        return stringRedisTemplate.opsForValue().get(lockKey);
    }

    public void releaseLock(String lockKey, String name) {
        String value = stringRedisTemplate.opsForValue().get(lockKey);
        if (value != null && value.equals(name)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

}
