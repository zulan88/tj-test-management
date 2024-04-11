package net.wanji.business.schedule;

import net.wanji.business.exception.BusinessException;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 18:41
 * @Descriptoin:
 */

public class PlaybackSchedule {

    private static final Logger log = LoggerFactory.getLogger("business");

    static Map<String, PlaybackDomain> futureMap = new HashMap<>(16);


    public static void startSendingData(String key, List<List<TrajectoryValueDto>> data) throws BusinessException, IOException {
        stopSendingData(key);
        futureMap.put(key, new PlaybackDomain(key, data));
        log.info("成功创建回放任务{}", key);
    }

    public static void suspend(String key) throws BusinessException {
        if (!futureMap.containsKey(key)) {
            throw new BusinessException(StringUtils.format("回放任务{}不存在", key));
        }
        futureMap.get(key).suspend();
        log.info("暂停回放任务{}", key);
    }

    public static void goOn(String key) throws BusinessException {
        if (!futureMap.containsKey(key)) {
            return;
        }
        futureMap.get(key).goOn();
        log.info("继续回放任务{}", key);
    }

    public static void stopSendingData(String key) throws BusinessException, IOException {
        if (!futureMap.containsKey(key)) {
            return;
        }
        WebSocketManage.remove(key, true);
        futureMap.get(key).stopSendingData();
        futureMap.remove(key);
        log.info("删除回放任务{}", key);
    }
}
