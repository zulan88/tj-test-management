package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PointType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.DeviceConnInfo;
import net.wanji.business.domain.param.DeviceConnRule;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.service.RestService;
import net.wanji.business.service.TestingService;
import net.wanji.common.utils.GeoUtil;
import net.wanji.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    private TjCaseMapper caseMapper;

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
        boolean canStart = false;
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
            caseConfigBo.setPositionStatus(YN.Y_INT);
        }
        Map<String, List<CaseConfigBo>> statusMap = configs.stream().collect(
                Collectors.groupingBy(CaseConfigBo::getParticipantRole));
        RealVehicleVerificationPageVo result = new RealVehicleVerificationPageVo();
        result.setStatusMap(statusMap);
        validStatus(result);
        return result;
    }

    @Override
    public boolean start(Integer caseId) throws BusinessException {
        return false;
    }

    @Override
    public boolean sendRule(Integer caseId, Integer action) throws BusinessException {
        CaseInfoBo caseInfoBo = caseMapper.selectCaseInfo(caseId);
        this.validConfig(caseInfoBo);
        List<CaseConfigBo> configs = caseInfoBo.getCaseConfigs().stream().filter(deviceId ->
                !ObjectUtils.isEmpty(deviceId)).collect(Collectors.toList());
        List<DeviceConnRule> deviceConnRules = generateDeviceConnRules(configs);
        restService.sendRuleUrl(new CaseRuleControl(System.currentTimeMillis(), String.valueOf(caseId), action,
                deviceConnRules));
        return true;
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
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_OFFLINE_TEMPLATE, config.getDeviceName()));
            }
            if (YN.Y_INT != config.getPositionStatus()) {
                messageBuilder.append(StringUtils.format(ContentTemplate.DEVICE_POS_ERROR_TEMPLATE, config.getDeviceName()));
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
