package net.wanji.business.service;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import net.wanji.business.common.Constants.Extension;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.bo.VehicleTrajectoryBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.proto.E1FrameProto.Demo;
import net.wanji.business.util.TrajectoryConsumer;
import net.wanji.common.config.KafkaConsumerConfig;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.common.utils.file.FileUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/7 13:05
 * @Descriptoin:
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Autowired
    private TjCaseMapper tjCaseMapper;

    @Async
    public void saveRouteFile(Integer caseId, String topic, String user) throws ExecutionException, InterruptedException {
        TjCase tjCase = tjCaseMapper.selectById(caseId);
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(tjCase.getDetailInfo(), CaseTrajectoryDetailBo.class);
        List<byte[]> routeData = new TrajectoryConsumer(kafkaConsumerConfig).consumeMessages(topic);
        if (CollectionUtils.isEmpty(routeData) || ObjectUtils.isEmpty(caseTrajectoryDetailBo)) {
            log.error("详情为空或路线为空");
            return;
        }
        List<List<Map>> e1List = byte2List(routeData);
        // 保存本地文件
        FutureTask<String> saveTask = new FutureTask<>(() -> {
            String path = FileUtils.writeE1Bytes(e1List, WanjiConfig.getRoutePath(), Extension.TXT);
            return path;
        });
        // 点位校验
        FutureTask<CaseTrajectoryDetailBo> verifyTask = new FutureTask<>(() -> {
            CaseTrajectoryDetailBo temp = caseTrajectoryDetailBo;
            verifyRoute(e1List, caseTrajectoryDetailBo);
            return temp;
        });

        saveTask.run();
        verifyTask.run();

        String path = saveTask.get();
        CaseTrajectoryDetailBo newDetail = verifyTask.get();

        log.info("saveRouteFile newDetail:{}", JSONObject.toJSONString(newDetail));
        log.info("saveRouteFile routePath:{}", path);
        tjCase.setRouteFile(path);
        tjCase.setDetailInfo(JSONObject.toJSONString(newDetail));
        tjCase.setUpdatedBy(user);
        tjCase.setUpdatedDate(LocalDateTime.now());
        tjCaseMapper.updateById(tjCase);
        log.info("更新完成");
    }

    public void verifyRoute(List<List<Map>> data, CaseTrajectoryDetailBo caseTrajectoryDetailBo) {
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(caseTrajectoryDetailBo)
                || CollectionUtils.isEmpty(caseTrajectoryDetailBo.getVehicle())) {
            return;
        }
        for (List<Map> mapList : data) {
            for (Map item : mapList) {
                for (VehicleTrajectoryBo trajectoryBo : caseTrajectoryDetailBo.getVehicle()) {
                    if (!trajectoryBo.getId().equals(item.get("id"))) {
                        continue;
                    }
                    List<TrajectoryDetailBo> trajectory = trajectoryBo.getTrajectory();
                    for (TrajectoryDetailBo trajectoryDetailBo : trajectory) {
                        if (trajectoryDetailBo.getFrameId().intValue() == (int) item.get("frameId")) {
                            // todo 校验逻辑
                            trajectoryDetailBo.setPass(true);
                            trajectoryDetailBo.setReason("已校验完成");
                        }
                    }

                }
            }
        }
    }

    public Map<String, List<Map<String, Double>>> extractRoute(List<List<Map>> data) {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        Map<String, List<Map<String, Double>>> result = new HashMap<>();
        for (List<Map> vehicleList : data) {
            Map<String, Map<String, Double>> idAndPoint = vehicleList.stream().collect(Collectors.toMap(
                    item -> String.valueOf(item.get("id")),
                    value -> {
                        Map<String, Double> map = new HashMap<>();
                        map.put("longitude", Double.parseDouble(String.valueOf(value.get("longitude"))));
                        map.put("latitude", Double.parseDouble(String.valueOf(value.get("latitude"))));
                        return map;
                    }));
            for (Map.Entry<String, Map<String, Double>> item : idAndPoint.entrySet()) {
                if (!result.containsKey(item.getKey())) {
                    List<Map<String, Double>> list = new ArrayList<>();
                    list.add(item.getValue());
                    result.put(item.getKey(), list);
                    continue;
                }
                List<Map<String, Double>> list = result.get(item.getKey());
                list.add(item.getValue());
                result.put(item.getKey(), list);
            }
        }
        return result;
    }

    public List<List<Map>> readVehicleRouteFile(String fileName, String vehicleId) throws IOException {
        List<List<Map>> e1List = readRouteFile(fileName);
        if (StringUtils.isNotEmpty(vehicleId)) {
            e1List = e1List.stream().map(item -> item.stream().filter(route ->
                            route.containsKey("id") && vehicleId.equals(String.valueOf(route.get("id"))))
                    .collect(Collectors.toList())
            ).collect(Collectors.toList());
        }
        return e1List;
    }

    public List<List<Map>> readRouteFile(String fileName) throws IOException {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        List<List<Map>> lists = FileUtils.readE1(routeFile);
        return lists;
    }

    private List<List<Map>> byte2List(List<byte[]> bytes) {
        return bytes.stream().map(e1 -> {
            List<Map> mapList = new ArrayList<>();
            try {
                Demo e1Demo = Demo.parseFrom(e1);
                mapList = JSONObject.parseArray(e1Demo.getValue(), Map.class);
            } catch (InvalidProtocolBufferException e) {
                System.out.println(e.getMessage());
            }
            return mapList;
        }).collect(Collectors.toList());
    }
}
