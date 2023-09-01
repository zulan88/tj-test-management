package net.wanji.business.schedule;

import net.wanji.business.common.Constants.WebsocketKey;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 18:41
 * @Descriptoin:
 */

public class PlaybackSchedule {

    private static final Logger log = LoggerFactory.getLogger("business");

    static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
    static Map<String, PlaybackDomain> futureMap = new HashMap<>(16);


    public static void startSendingData(String vehicleId, List<List<TrajectoryValueDto>> data) throws BusinessException {
        String key = getFutureKeyIfNull(vehicleId);
        if (futureMap.containsKey(key)) {
            throw new BusinessException(StringUtils.format("回放任务{}已存在", key));
        }
        PlaybackDomain playbackDomain = new PlaybackDomain(executorService, key, data);
        futureMap.put(key, playbackDomain);
        log.info("创建回放任务{}完成", vehicleId);
    }

    public static void suspend(String futureKey) throws BusinessException {
        String key = getFutureKeyIfNull(futureKey);
        if (!futureMap.containsKey(key)) {
            throw new BusinessException(StringUtils.format("回放任务{}不存在", futureKey));
        }
        futureMap.get(key).suspend();
    }

    public static void goOn(String futureKey) throws BusinessException {
        String key = getFutureKeyIfNull(futureKey);
        if (!futureMap.containsKey(key)) {
            return;
        }
        futureMap.get(key).goOn();
    }

    public static void stopSendingData(String futureKey) throws BusinessException {
        String key = getFutureKeyIfNull(futureKey);
        if (!futureMap.containsKey(key)) {
            return;
        }
        futureMap.get(key).stopSendingData();
        futureMap.remove(key);
    }

    private static String getFutureKeyIfNull(String futureKey) {
        return StringUtils.isEmpty(futureKey) ? WebsocketKey.DEFAULT_KEY : futureKey;
    }
}
