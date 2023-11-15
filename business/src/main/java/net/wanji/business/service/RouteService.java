package net.wanji.business.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.Extension;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.vo.ParticipantTrajectoryVo;
import net.wanji.business.domain.vo.TrajectoryDetailVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.common.common.RealTestTrajectoryDto;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.Calculate;
import net.wanji.common.utils.DateUtils;
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
    private TjTaskMapper taskMapper;

    @Autowired
    private TjCaseMapper tjCaseMapper;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Autowired
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

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

    public void saveTaskRouteFile(Integer recordId, List<RealTestTrajectoryDto> data,
                                  CaseTrajectoryDetailBo originalTrajectory)
            throws ExecutionException, InterruptedException {
        log.info(StringUtils.format("保存实车测试{}路径文件", recordId));
        TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectById(recordId);
        // 保存本地文件
        try {
            int pointNum = 0;
            int passNum = 0;
            String path = FileUtils.writeRoute(data, WanjiConfig.getRoutePath(), Extension.TXT);
            for (ParticipantTrajectoryBo participantTrajectory : originalTrajectory.getParticipantTrajectories()) {
                for (TrajectoryDetailBo trajectoryDetailBo : participantTrajectory.getTrajectory()) {
                    pointNum++;
                    if (trajectoryDetailBo.isPass()) {
                        passNum++;
                        continue;
                    }
                }

            }
            log.info("saveTaskRouteFile routePath:{}", path);
            taskCaseRecord.setRouteFile(path);
            taskCaseRecord.setStatus(TestingStatus.FINISHED);
            taskCaseRecord.setEndTime(LocalDateTime.now());
            taskCaseRecordMapper.updateById(taskCaseRecord);

            TjTaskCase taskCase = new TjTaskCase();
            taskCase.setPassingRate(pointNum == 0 ? "100%" : Calculate.getPercent(passNum, pointNum));
            QueryWrapper<TjTaskCase> updateMapper = new QueryWrapper<>();
            updateMapper.eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId()).eq(ColumnName.CASE_ID_COLUMN,
                    taskCaseRecord.getCaseId());
            taskCaseMapper.update(taskCase, updateMapper);

            QueryWrapper<TjTaskCase> queryMapper = new QueryWrapper<>();
            queryMapper.eq(ColumnName.TASK_ID, taskCaseRecord.getTaskId());
            List<TjTaskCase> tjTaskCases = taskCaseMapper.selectList(queryMapper);
            if (CollectionUtils.emptyIfNull(tjTaskCases).stream().allMatch(item -> "已完成".equals(item.getStatus()))) {
                Integer taskId = tjTaskCases.get(0).getTaskId();
                TjTask tjTask = new TjTask();
                tjTask.setId(taskId);
                tjTask.setEndTime(new Date());
                tjTask.setTestTotalTime(DateUtils.secondsToDuration(tjTaskCases.stream().mapToInt(caseObj ->
                        Integer.parseInt(caseObj.getTestTotalTime())).sum()));
                taskMapper.updateById(tjTask);

            }
            log.info("更新完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int numerator = 5;
        int denominator = 9;

        // 将分数转换为浮点数
        double fraction = (double) numerator / denominator * 100;

        // 创建DecimalFormat对象，指定保留两位小数的格式
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        // 使用DecimalFormat格式化浮点数
        String formattedFraction = decimalFormat.format(fraction);

        System.out.println("Formatted fraction: " + formattedFraction + "%");
    }


    /**
     * 仿真轨迹点位检查
     *
     * @param caseId
     * @param oldDetail
     * @param data
     */
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


    /**
     * 仿真轨迹点位检查
     *
     * @param oldDetail
     * @param data
     */
    public List<ParticipantTrajectoryVo> checkSimulaitonRoute2(CaseTrajectoryDetailBo oldDetail, List<TrajectoryValueDto> data, Long time) throws ParseException {
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(oldDetail)
                || CollectionUtils.isEmpty(oldDetail.getParticipantTrajectories())) {
            return null;
        }
        List<ParticipantTrajectoryVo> res = new ArrayList<>();
        check(oldDetail, data);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        for (ParticipantTrajectoryBo trajectoryBo : oldDetail.getParticipantTrajectories()) {
            ParticipantTrajectoryVo participantTrajectoryVo = new ParticipantTrajectoryVo();
            participantTrajectoryVo.setId(trajectoryBo.getId());
            for (TrajectoryDetailBo trajectoryDetailBo : trajectoryBo.getTrajectory()) {
                Long restime = 0l;
                if(!trajectoryDetailBo.getTime().equals("0")) {
                    restime = (dateFormat.parse(trajectoryDetailBo.getTime()).getTime() - time) / 1000;
                }
                TrajectoryDetailVo trajectoryDetailVo = new TrajectoryDetailVo(trajectoryDetailBo.getFrameId(), trajectoryDetailBo.isPass(), trajectoryDetailBo.getSpeed(), String.valueOf(restime));
                participantTrajectoryVo.addtrajectory(trajectoryDetailVo);
            }
            res.add(participantTrajectoryVo);
        }
        return res;
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

    public void checkTaskRoute(Integer recordId, CaseTrajectoryDetailBo oldDetail, List<TrajectoryValueDto> data) {
        if (CollectionUtils.isEmpty(data) || ObjectUtils.isEmpty(oldDetail)
                || CollectionUtils.isEmpty(oldDetail.getParticipantTrajectories())) {
            return;
        }
        if (check(oldDetail, data)) {
            TjTaskCaseRecord taskCaseRecord = new TjTaskCaseRecord();
            taskCaseRecord.setId(recordId);
            taskCaseRecord.setDetailInfo(JSONObject.toJSONString(oldDetail));
            taskCaseRecordMapper.updateById(taskCaseRecord);
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
                    if (instance <= 3) {
                        trajectoryDetailBo.setPass(true);
                        trajectoryDetailBo.setReason("已校验完成");
                        trajectoryDetailBo.setTime(trajectory.getTimestamp());
                        trajectoryDetailBo.setSpeed(Double.valueOf(trajectory.getSpeed()));
                    } else {
                        trajectoryDetailBo.setReason("未经过该点位3米范围区域");
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
        try {
            for (List<TrajectoryValueDto> vehicleList : data) {
                Map<String, Map<String, Double>> idAndPoint = vehicleList.stream().collect(Collectors.toMap(
                        item -> String.valueOf(item.getId()),
                        value -> {
                            Map<String, Double> map = new HashMap<>();
                            map.put("longitude", Double.parseDouble(String.valueOf(value.getLongitude())));
                            map.put("latitude", Double.parseDouble(String.valueOf(value.getLatitude())));
                            return map;
                        }, (oldValue, newValue) -> oldValue));
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
        } catch (Exception e) {
            log.error("轨迹文件读取异常，请重新进行验证");
        }
        return result;
    }

    /**
     * 读取仿真验证轨迹文件
     *
     * @param fileName
     * @param participantId
     * @return
     * @throws IOException
     */
    public List<List<TrajectoryValueDto>> readTrajectoryFromRouteFile(String fileName, String participantId) throws IOException {
        List<List<TrajectoryValueDto>> data = readRouteFile(fileName);
        return readTrajectoryFromData(data, participantId);
    }

    public List<List<TrajectoryValueDto>> readRouteFile(String fileName) throws IOException {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readTrajectory(routeFile);
    }

    public List<List<TrajectoryValueDto>> readTrajectoryFromData(List<List<TrajectoryValueDto>> data, String participantId) {
        return CollectionUtils.emptyIfNull(data).stream().map(item -> filterParticipant(item, participantId))
                .collect(Collectors.toList());
    }

    public List<TrajectoryValueDto> filterParticipant(List<TrajectoryValueDto> data, String participantId) {
        return StringUtils.isNotEmpty(participantId)
                ? CollectionUtils.emptyIfNull(data).stream().filter(route ->
                participantId.equals(route.getId())).collect(Collectors.toList())
                : data;
    }

    /**
     * 读取仿真验证轨迹原始文件
     *
     * @param fileName
     * @param participantId
     * @return
     * @throws IOException
     */
    public List<SimulationTrajectoryDto> readOriTrajectoryFromRouteFile(String fileName, String participantId) throws IOException {
        List<SimulationTrajectoryDto> data = readOriRouteFile(fileName);
        return readOriTrajectoryFromData(data, participantId);
    }

    public List<SimulationTrajectoryDto> readOriRouteFile(String fileName) throws IOException {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readOriTrajectory(routeFile);
    }

    public List<SimulationTrajectoryDto> readOriTrajectoryFromData(List<SimulationTrajectoryDto> data, String participantId) {
        for (SimulationTrajectoryDto simulationTrajectoryDto : data) {
            simulationTrajectoryDto.setValue(filterParticipant(simulationTrajectoryDto.getValue(), participantId));
        }
        return data;
    }


    /**
     * 读取实车验证轨迹文件
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public List<RealTestTrajectoryDto> readRealTrajectoryFromRouteFile(String fileName) {
        return readRealRouteFile(fileName);
    }

    public List<RealTestTrajectoryDto> readRealRouteFile(String fileName) {
        String routeFile = FileUploadUtils.getAbsolutePathFileName(fileName);
        return FileUtils.readRealRouteFile(routeFile);
    }
}