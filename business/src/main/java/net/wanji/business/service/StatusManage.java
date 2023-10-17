package net.wanji.business.service;

import net.wanji.business.component.CountDownValueDto;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: guanyuduo
 * @date: 2023/10/13 11:13
 * @descriptoin:
 */
@Component
public class StatusManage {

    public static Map<String, CountDownValueDto> countDownLatchMap = new HashMap<>();

    public static void addCountDownLatch(String key) throws InterruptedException {
        CountDownValueDto countDownValueDto = new CountDownValueDto();
        countDownLatchMap.put(key, countDownValueDto);
        countDownValueDto.getCountDownLatch().await(1000, TimeUnit.MILLISECONDS);
    }

    public static void countDown(String key, Object value) {
        CountDownValueDto countDownValueDto = countDownLatchMap.get(key);
        if (!ObjectUtils.isEmpty(countDownValueDto) && !ObjectUtils.isEmpty(countDownValueDto.getCountDownLatch())) {
            countDownValueDto.setValue(value);
            countDownValueDto.getCountDownLatch().countDown();
        }
    }

    public static Object getValue(String key) {
        try {
            CountDownValueDto countDownValueDto = countDownLatchMap.get(key);
            if (!ObjectUtils.isEmpty(countDownValueDto)) {
                return countDownValueDto.getValue();
            }
            return null;
        } finally {
            countDownLatchMap.remove(key);
        }
    }
}
