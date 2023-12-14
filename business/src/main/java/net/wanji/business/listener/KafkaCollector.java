package net.wanji.business.listener;

import com.alibaba.fastjson.JSONObject;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author: guanyuduo
 * @date: 2023/12/8 15:08
 * @descriptoin:
 */
@Component
public class KafkaCollector {

    public static Map<String, List<Object>> collectorMap = new ConcurrentHashMap<>();


    public boolean collector(String key, Object data) {
        if (!collectorMap.containsKey(key)) {
            List<Object> list = new ArrayList<>();
            list.add(data);
            collectorMap.put(key, list);
            return true;
        }
        collectorMap.get(key).add(data);
        return true;
    }

    public <T> List<T> take(String key) {
        return CollectionUtils.emptyIfNull(collectorMap.get(key)).stream().map(p -> (T) p).collect(Collectors.toList());
    }

}
