package net.wanji.common.utils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 16:18
 * @Descriptoin:
 */

public class CounterUtil {

    private static int MAX_VALUE = 9999;
    private static AtomicInteger counter = new AtomicInteger(1);
    private static DecimalFormat df = new DecimalFormat("0000");

    static {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long initialDelay = ChronoUnit.SECONDS.between(LocalDateTime.now(), LocalDateTime.of(LocalDateTime.now().plusDays(1).toLocalDate(), LocalTime.MIDNIGHT));
        scheduler.scheduleAtFixedRate(CounterUtil::resetCounter, initialDelay, TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }

    /**
     * 获取一个长度为4的由0和一个递增整数组成的字符串
     *
     * @return
     */
    public static synchronized String getNextNumber() {
        int currentCounter = counter.getAndIncrement();
        if (currentCounter > MAX_VALUE) {
            resetCounter();
            currentCounter = counter.getAndIncrement();
        }
        return df.format(currentCounter);
    }

    private static void resetCounter() {
        counter.set(1);
    }
}
