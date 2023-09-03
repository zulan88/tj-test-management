package net.wanji.business.service;

import com.alibaba.fastjson.JSONObject;
import net.wanji.business.common.Constants.Extension;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.WebsocketKey;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCasePartConfigMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.GeoUtil;
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
    private TjCaseMapper tjCaseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Autowired
    private TjCasePartConfigMapper casePartConfigMapper;

    @Async
    public void saveRouteFile(Integer caseId, List<SimulationTrajectoryDto> data) throws ExecutionException, InterruptedException {
        log.info(StringUtils.format("保存{}路径文件", caseId));
        TjCase tjCase = new TjCase();
        tjCase.setId(caseId);
        // 保存本地文件
        try {
            String path = FileUtils.writeRoute(data, WanjiConfig.getRoutePath(), Extension.TXT);
            log.info("saveRouteFile routePath:{}", path);
            tjCase.setRouteFile(path);
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCaseMapper.updateById(tjCase);
            log.info("更新完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveRealRouteFile(Integer recordId, List<RealTestTrajectoryDto> data) throws ExecutionException, InterruptedException {
        log.info(StringUtils.format("保存实车测试{}路径文件", recordId));
        TjCaseRealRecord caseRealRecord = new TjCaseRealRecord();
        caseRealRecord.setId(recordId);
        // 保存本地文件
        try {
            String path = FileUtils.writeRoute(data, WanjiConfig.getRoutePath(), Extension.TXT);
            log.info("saveRealRouteFile routePath:{}", path);
            caseRealRecord.setRouteFile(path);
            caseRealRecord.setStatus(TestingStatus.FINISHED);
            caseRealRecord.setEndTime(LocalDateTime.now());
            caseRealRecordMapper.updateById(caseRealRecord);
            log.info("更新完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkSimulaitonRoute(Integer caseId, CaseTrajectoryDetailBo oldDetail, List<TrajectoryValueDto> data) {
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(oldDetail)
                || CollectionUtils.isEmpty(oldDetail.getParticipantTrajectories())) {
            return;
        }
        if (check(oldDetail, data)) {
            TjCase tjCase = new TjCase();
            tjCase.setId(caseId);
            tjCase.setDetailInfo(JSONObject.toJSONString(oldDetail));
            tjCaseMapper.updateById(tjCase);
        }
    }

    public void checkRealRoute(Integer recordId, CaseTrajectoryDetailBo oldDetail, List<TrajectoryValueDto> data) {
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(oldDetail)
                || CollectionUtils.isEmpty(oldDetail.getParticipantTrajectories())) {
            return;
        }
        if (check(oldDetail, data)) {
            TjCaseRealRecord caseRealRecord = new TjCaseRealRecord();
            caseRealRecord.setId(recordId);
            caseRealRecord.setDetailInfo(JSONObject.toJSONString(oldDetail));
            caseRealRecordMapper.updateById(caseRealRecord);
        }
    }

    public boolean check(CaseTrajectoryDetailBo oldDetail, List<TrajectoryValueDto> data) {
        boolean update = false;
        for (TrajectoryValueDto trajectory : data) {
            for (ParticipantTrajectoryBo trajectoryBo : oldDetail.getParticipantTrajectories()) {
                if (!StringUtils.equals(trajectoryBo.getName(), trajectory.getName())) {
                    continue;
                }
                List<TrajectoryDetailBo> points = trajectoryBo.getTrajectory();
                for (TrajectoryDetailBo trajectoryDetailBo : points) {
                    if (trajectoryDetailBo.isPass()) {
                        continue;
                    }
                    String[] positionArray = trajectoryDetailBo.getPosition().split(",");
                    double longitude = Double.parseDouble(positionArray[0]);
                    double latitude = Double.parseDouble(positionArray[1]);
                    double instance = GeoUtil.calculateDistance(latitude, longitude,
                            trajectory.getLatitude(), trajectory.getLongitude());
                    if (instance <= 40) {
                        trajectoryDetailBo.setPass(true);
                        trajectoryDetailBo.setReason("已校验完成");
                    } else {
                        trajectoryDetailBo.setReason("未经过该点位40米范围区域");
                    }
                    update = true;
                }
            }
        }
        return update;
    }

    public boolean verifyRoute(List<TrajectoryDetailBo> points) {
        // todo anyMatch -> allMatch
        return CollectionUtils.emptyIfNull(points).stream().anyMatch(TrajectoryDetailBo::isPass);
    }

    public Map<String, List<Map<String, Double>>> extractRoute(List<List<TrajectoryValueDto>> data) {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        Map<String, List<Map<String, Double>>> result = new HashMap<>();
        for (List<TrajectoryValueDto> vehicleList : data) {
            Map<String, Map<String, Double>> nameAndPoint = vehicleList.stream().collect(Collectors.toMap(
                    item -> String.valueOf(item.getName()),
                    value -> {
                        Map<String, Double> map = new HashMap<>();
                        map.put("longitude", Double.parseDouble(String.valueOf(value.getLongitude())));
                        map.put("latitude", Double.parseDouble(String.valueOf(value.getLatitude())));
                        return map;
                    }));
            for (Map.Entry<String, Map<String, Double>> item : nameAndPoint.entrySet()) {
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

    /**
     * 读取仿真验证轨迹文件
     * @param fileName
     * @param participantName
     * @return
     * @throws IOException
     */
    public List<List<TrajectoryValueDto>> readTrajectoryFromRouteFile(String fileName, String participantName) throws IOException {
        List<List<TrajectoryValueDto>> data = readRouteFile(fileName);
        return readTrajectoryFromData(data, participantName);
    }

    public List<List<TrajectoryValueDto>> readRouteFile(String fileName) throws IOException {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readE1(routeFile);
    }

    public List<List<TrajectoryValueDto>> readTrajectoryFromData(List<List<TrajectoryValueDto>> data, String participantName) {
        return CollectionUtils.emptyIfNull(data).stream().map(item -> filterParticipant(item, participantName))
                .collect(Collectors.toList());
    }

    public List<TrajectoryValueDto> filterParticipant(List<TrajectoryValueDto> data, String participantName) {
        return StringUtils.isNotEmpty(participantName) && !StringUtils.equals(WebsocketKey.DEFAULT_KEY, participantName)
                ? CollectionUtils.emptyIfNull(data).stream().filter(route ->
                participantName.equals(route.getName())).collect(Collectors.toList())
                : data;
    }

    /**
     * 读取实车验证轨迹文件
     * @param fileName
     * @return
     * @throws IOException
     */
    public  List<RealTestTrajectoryDto> readRealTrajectoryFromRouteFile(String fileName) {
        return readRealRouteFile(fileName);
    }

    public List<RealTestTrajectoryDto> readRealRouteFile(String fileName) {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readRealRouteFile(routeFile);
    }


}
