package net.wanji.business.listener;

import com.alibaba.fastjson.JSONObject;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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

    public static Map<String, Map<Integer, List<List<SimulationTrajectoryDto>>>> collectorMap = new ConcurrentHashMap<>();


    public boolean collector(String key, Integer caseId, List<SimulationTrajectoryDto> data) {
//        // 没有key
//        if (!collectorMap.containsKey(key)) {
//            Map<Integer, List<List<SimulationTrajectoryDto>>> caseTrajectoryMap = new HashMap<>();
//            List<List<SimulationTrajectoryDto>> trajectoriesList = new ArrayList<>();
//            trajectoriesList.add(data);
//            caseTrajectoryMap.put(caseId, trajectoriesList);
//            collectorMap.put(key, caseTrajectoryMap);
//            return true;
//        }
//        Map<Integer, List<SimulationTrajectoryDto>> caseTrajectoryMap = collectorMap.get(key);
//        // 有key，没有用例
//        if (!caseTrajectoryMap.containsKey(caseId)) {
//            caseTrajectoryMap.put(caseId, data);
//            return true;
//        }
//        // 有key，有用例
//        List<SimulationTrajectoryDto> trajectories = caseTrajectoryMap.get(caseId);
//        trajectories.addAll(data);
        return true;
    }

    public int getSize(String key, Integer caseId) {
        return collectorMap.containsKey(key)
                ? (collectorMap.get(key).containsKey(caseId) ? collectorMap.get(key).get(caseId).size() : 0)
                : 0;
    }

    public List<SimulationTrajectoryDto> take(String key, Integer caseId) {
        return null;
//        return collectorMap.containsKey(key)
//                ? (collectorMap.get(key).getOrDefault(caseId, null))
//                : null;
    }

    public void remove(String key) {
        collectorMap.remove(key);
    }

}
