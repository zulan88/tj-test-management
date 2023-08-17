package net.wanji.business.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.Extension;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.common.utils.file.FileUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.geo.GeoUtils;
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

    @Async
    public void saveRouteFile(String caseNumber, List<SimulationTrajectoryDto> data) throws ExecutionException, InterruptedException {
        log.info(StringUtils.format("保存{}路径文件", caseNumber));
        QueryWrapper<TjCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_NUMBER_COLUMN, caseNumber);
        TjCase tjCase = tjCaseMapper.selectOne(queryWrapper);
        // 保存本地文件
        try {
            String path = FileUtils.writeE1List(data, WanjiConfig.getRoutePath(), Extension.TXT);
            log.info("saveRouteFile routePath:{}", path);
            tjCase.setRouteFile(path);
            tjCase.setUpdatedDate(LocalDateTime.now());
            tjCaseMapper.updateById(tjCase);
            log.info("更新完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkRoute(Integer caseId, List<TrajectoryValueDto> data) {
        TjCase tjCase = tjCaseMapper.selectById(caseId);
        CaseTrajectoryDetailBo caseTrajectoryDetailBo = JSONObject.parseObject(tjCase.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(caseTrajectoryDetailBo)
                || CollectionUtils.isEmpty(caseTrajectoryDetailBo.getParticipantTrajectories())) {
            return;
        }
        boolean update = false;
        for (TrajectoryValueDto trajectory : data) {
            for (ParticipantTrajectoryBo trajectoryBo : caseTrajectoryDetailBo.getParticipantTrajectories()) {
                if (!StringUtils.equals(trajectoryBo.getId(), trajectory.getId())) {
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
        if (update) {
            tjCase.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
            tjCaseMapper.updateById(tjCase);
        }
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
            Map<String, Map<String, Double>> idAndPoint = vehicleList.stream().collect(Collectors.toMap(
                    item -> String.valueOf(item.getId()),
                    value -> {
                        Map<String, Double> map = new HashMap<>();
                        map.put("longitude", Double.parseDouble(String.valueOf(value.getLongitude())));
                        map.put("latitude", Double.parseDouble(String.valueOf(value.getLatitude())));
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

    public List<List<TrajectoryValueDto>> readTrajectoryFromRouteFile(String fileName, String participantId) throws IOException {
        List<List<TrajectoryValueDto>> data = readRouteFile(fileName);
        return readTrajectoryFromData(data, participantId);
    }

    public List<List<TrajectoryValueDto>> readTrajectoryFromData(List<List<TrajectoryValueDto>> data, String participantId) {
        if (StringUtils.isNotEmpty(participantId)) {
            data = data.stream().map(item -> filterParticipant(item, participantId)).collect(Collectors.toList());
        }
        return data;
    }

    public List<TrajectoryValueDto> filterParticipant(List<TrajectoryValueDto> data, String participantId) {
        return StringUtils.isEmpty(participantId) ? data : CollectionUtils.emptyIfNull(data).stream().filter(route ->
                        participantId.equals(route.getId()))
                .collect(Collectors.toList());
    }

    public List<List<TrajectoryValueDto>> readRouteFile(String fileName) throws IOException {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readE1(routeFile);
    }


}
