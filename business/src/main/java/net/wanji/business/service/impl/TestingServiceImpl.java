package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PointType;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TestingStatus;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.service.RestService;
import net.wanji.business.service.TestingService;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.StringUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 10:00
 * @Descriptoin:
 */
@Service
public class TestingServiceImpl implements TestingService {

    @Autowired
    private RestService restService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjFragmentedScenesMapper scenesMapper;

    @Autowired
    private TjFragmentedSceneDetailMapper sceneDetailMapper;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Override
    public RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
        CaseTrajectoryDetailBo trajectoryDetail = JSONObject.parseObject(caseInfoBo.getDetailInfo(),
                CaseTrajectoryDetailBo.class);
        Map<String, String> partStartMap =
                CollectionUtils.emptyIfNull(trajectoryDetail.getParticipantTrajectories()).stream().collect(
                        Collectors.toMap(
                                ParticipantTrajectoryBo::getId,
                                item -> CollectionUtils.emptyIfNull(item.getTrajectory()).stream()
                                        .filter(t -> PointType.START.equals(t.getType())).findFirst()
                                        .orElse(new TrajectoryDetailBo()).getPosition()));
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(info ->
                !ObjectUtils.isEmpty(info.getDeviceId())).collect(Collectors.toList());
        for (CaseConfigBo caseConfigBo : configs) {
            // todo 设备信息查询逻辑待实现
            Map<String, Object> map = restService.searchDeviceInfo(caseConfigBo.getIp(), HttpMethod.POST);
            caseConfigBo.setStatus((int) map.get("status"));
            String start = partStartMap.get(caseConfigBo.getBusinessId());
            if (StringUtils.isEmpty(start)) {
                continue;
            }
            String[] position = start.split(",");
            double longitude = (double) map.get("longitude");
            double latitude = (double) map.get("latitude");
            double courseAngle = (double) map.get("courseAngle");
            caseConfigBo.setStartLongitude(Double.parseDouble(position[0]));
            caseConfigBo.setStartLatitude(Double.parseDouble(position[1]));
            caseConfigBo.setLongitude(longitude);
            caseConfigBo.setLatitude(latitude);
            caseConfigBo.setCourseAngle(courseAngle);
            double v = GeoUtil.calculateDistance(latitude, longitude, Double.parseDouble(position[1]),
                    Double.parseDouble(position[0]));
            if (v > 5) {
                // todo 具体距离待沟通

            }
            caseConfigBo.setPositionStatus(YN.N_INT);
        }
        Map<String, List<CaseConfigBo>> statusMap = configs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole));
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setCaseId(caseId);
        result.setFilePath(caseInfoBo.getFilePath());
        result.setGeoJsonPath(caseInfoBo.getGeoJsonPath());
        result.setStatusMap(statusMap);
        validStatus(result);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CaseRealTestVo prepare(Integer caseId) throws BusinessException {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("查询用例失败");
        }
        TjFragmentedSceneDetail sceneDetail = sceneDetailMapper.selectById(caseInfoBo.getSceneDetailId());
        if (ObjectUtils.isEmpty(sceneDetail) || StringUtils.isEmpty(sceneDetail.getTrajectoryInfo())) {
            throw new BusinessException("请先配置轨迹信息");
        }
        CaseTrajectoryDetailBo caseTrajectoryDetailBo =
                JSONObject.parseObject(sceneDetail.getTrajectoryInfo(), CaseTrajectoryDetailBo.class);
        Map<String, List<CaseConfigBo>> partMap = caseInfoBo.getCaseConfigs().stream().collect(
                Collectors.groupingBy(CaseConfigBo::getSupportRoles));
        int avNum = partMap.containsKey(PartRole.AV) ? partMap.get(PartRole.AV).size() : 0;
        int simulationNum = partMap.containsKey(PartRole.MV_SIMULATION) ? partMap.get(PartRole.MV_SIMULATION).size() : 0;
        int pedestrianNum = partMap.containsKey(PartRole.SP) ? partMap.get(PartRole.SP).size() : 0;
        caseTrajectoryDetailBo.setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, avNum,
                simulationNum, pedestrianNum));
        TjFragmentedScenes scenes = scenesMapper.selectById(sceneDetail.getFragmentedSceneId());
        caseTrajectoryDetailBo.setSceneDesc(scenes.getName());

        QueryWrapper<TjCaseRealRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        caseRealRecordMapper.delete(queryWrapper);

        TjCaseRealRecord tjCaseRealRecord = new TjCaseRealRecord();
        tjCaseRealRecord.setCaseId(caseId);
        tjCaseRealRecord.setDetailInfo(JSONObject.toJSONString(caseTrajectoryDetailBo));
        tjCaseRealRecord.setStatus(TestingStatus.NOT_START);
        caseRealRecordMapper.insert(tjCaseRealRecord);

        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(tjCaseRealRecord, caseRealTestVo);
        caseRealTestVo.setSceneName(scenes.getName());
        caseRealTestVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, caseInfoBo.getTestType()));
        return caseRealTestVo;
    }

    @Override
    public CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException {
        TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectById(recordId);
        if (ObjectUtils.isEmpty(caseRealRecord) || caseRealRecord.getStatus() > TestingStatus.NOT_START) {
            throw new BusinessException("未就绪");
        }
        Integer caseId = caseRealRecord.getCaseId();
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
        List<DeviceConnRule> deviceConnRules = generateDeviceConnRules(configs);
        restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(), String.valueOf(caseId), action,
                deviceConnRules));
        caseRealRecord.setStatus(TestingStatus.RUNNING);
        caseRealRecord.setStartTime(LocalDateTime.now());
        caseRealRecordMapper.updateById(caseRealRecord);
        CaseRealTestVo caseRealTestVo = new CaseRealTestVo();
        BeanUtils.copyProperties(caseRealRecord, caseRealTestVo);
        caseRealTestVo.setStartTime(DateUtils.getTime());
        SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(caseRealRecord.getDetailInfo(),
                SceneTrajectoryBo.class);
        Map<String, List<TrajectoryDetailBo>> mainTrajectoryMap = sceneTrajectoryBo.getParticipantTrajectories()
                .stream().filter(item -> PartType.MAIN.equals(item.getType())).collect(Collectors.toMap(
                        ParticipantTrajectoryBo::getId,
                        ParticipantTrajectoryBo::getTrajectory
                ));
        caseRealTestVo.setMainTrajectories(mainTrajectoryMap);

        Map<String, String> param = configs.stream().collect(Collectors.toMap(CaseConfigBo::getName, CaseConfigBo::getDataChannel));
        restService.imitateClientUrl(configs);
        return caseRealTestVo;
    }

    @Override
    public boolean getResult(Integer caseId) throws BusinessException {
        return false;
    }

    private void validStatus(RealVehicleVerificationPageVo pageVo) {
        Map<String, List<CaseConfigBo>> statusMap = pageVo.getStatusMap();
        List<CaseConfigBo> configs = new ArrayList<>();
        statusMap.values().stream().flatMap(List::stream).forEach(configs::add);
        StringBuilder messageBuilder = new StringBuilder();
        for (CaseConfigBo config : configs) {
            if (YN.Y_INT != config.getStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_OFFLINE_TEMPLATE,
                        config.getDeviceName()));
            }
            if (YN.Y_INT != config.getPositionStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_POS_ERROR_TEMPLATE,
                        config.getDeviceName()));
            }
        }
        String message = messageBuilder.toString();
        if (StringUtils.isNotEmpty(message)) {
            pageVo.setMessage(message);
        } else {
            pageVo.setCanStart(true);
        }
    }

    /**
     * 校验用例信息
     *
     * @param caseInfoBo
     * @throws BusinessException
     */
    private void validConfig(CaseInfoBo caseInfoBo) throws BusinessException {
        if (ObjectUtils.isEmpty(caseInfoBo)) {
            throw new BusinessException("用例查询异常");
        }
        List<CaseConfigBo> casePartConfigs = caseInfoBo.getCaseConfigs();
        if (CollectionUtils.isEmpty(casePartConfigs)) {
            throw new BusinessException("用例未进行角色配置");
        }
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(configs)) {
            throw new BusinessException("用例未进行设备配置");
        }
    }

    private List<DeviceConnRule> generateDeviceConnRules(List<CaseConfigBo> caseConfigs) {
        List<DeviceConnRule> rules = new ArrayList<>();
        for (int i = 0; i < caseConfigs.size(); i++) {
            CaseConfigBo sourceDevice = caseConfigs.get(i);
            for (int j = 0; j < caseConfigs.size(); j++) {
                if (j == i) {
                    continue;
                }
                CaseConfigBo targetDevice = caseConfigs.get(j);
                DeviceConnRule rule = new DeviceConnRule();
                rule.setSource(createConnInfo(sourceDevice));
                rule.setTarget(createConnInfo(targetDevice));
                rules.add(rule);
            }
        }
        return rules;
    }

    private static DeviceConnInfo createConnInfo(CaseConfigBo config) {
        DeviceConnInfo deviceConnInfo = new DeviceConnInfo();
        deviceConnInfo.setChannel(config.getDataChannel());
        deviceConnInfo.setControlChannel(config.getCommandChannel());
        deviceConnInfo.setId(String.valueOf(config.getDeviceId()));
        return deviceConnInfo;
    }
}
