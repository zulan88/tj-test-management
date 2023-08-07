package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.VehicleTrajectoryBo;
import net.wanji.business.domain.vo.CasePartConfigVo;
import net.wanji.business.entity.TjCasePartConfig;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCasePartConfigMapper;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.system.service.ISysDictDataService;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
@Service
public class TjCasePartConfigServiceImpl extends ServiceImpl<TjCasePartConfigMapper, TjCasePartConfig>
        implements TjCasePartConfigService {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private ISysDictDataService dictDataService;

    @Override
    public Map<String, List<CasePartConfigVo>> getConfigInfo(Integer caseId) throws BusinessException {
        List<SysDictData> partRole = dictTypeService.selectDictDataByType(SysType.PART_ROLE);
        if (CollectionUtils.isEmpty(partRole)) {
            throw new BusinessException("请配置业务字典：参与者角色part_role");
        }
        QueryWrapper<TjCasePartConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.CASE_ID_COLUMN, caseId);
        List<TjCasePartConfig> configs = this.list(queryWrapper);
        Map<String, List<CasePartConfigVo>> roleMap = CollectionUtils.emptyIfNull(configs).stream()
                .map(item -> {
                    CasePartConfigVo casePartConfigVo = new CasePartConfigVo();
                    BeanUtils.copyProperties(item, casePartConfigVo);
                    casePartConfigVo.setParticipantRoleName(dictDataService.selectDictLabel(SysType.PART_ROLE,
                            item.getParticipantRole()));
                    casePartConfigVo.setBusinessTypeName(dictDataService.selectDictLabel(SysType.PART_TYPE,
                            item.getBusinessType()));
                    casePartConfigVo.setName(casePartConfigVo.getBusinessTypeName() + casePartConfigVo.getBusinessId());
                    casePartConfigVo.setSelected(YN.Y_INT);
                    // todo 设备获取逻辑
                    casePartConfigVo.setDeviceName("畅行" + RandomUtils.nextInt());
                    return casePartConfigVo;
                }).collect(Collectors.groupingBy(TjCasePartConfig::getParticipantRole));
        Map<String, List<CasePartConfigVo>> result = new HashMap<>(partRole.size());
        for (SysDictData role : partRole) {
            result.put(role.getDictValue(), roleMap.get(role.getDictValue()));
        }
        return result;
    }

    @Override
    public Map<String, List<CasePartConfigVo>> trajectory2Config(SceneTrajectoryBo sceneTrajectoryBo) {
        List<CasePartConfigVo> result = new ArrayList<>();
        List<VehicleTrajectoryBo> vehicleTrajectory = sceneTrajectoryBo.getVehicle();
        List<CasePartConfigVo> vehicleParts = CollectionUtils.emptyIfNull(vehicleTrajectory).stream().map(vehicle -> {
            CasePartConfigVo config = new CasePartConfigVo();
            config.setBusinessId(vehicle.getId());
            config.setBusinessType(vehicle.getType());
            config.setName(dictDataService.selectDictLabel(SysType.PART_TYPE, vehicle.getType()) + vehicle.getId());
            return config;
        }).collect(Collectors.toList());
        result.addAll(vehicleParts);
        // todo 行人轨迹
        List<VehicleTrajectoryBo> pedestrianTrajectory = sceneTrajectoryBo.getPedestrian();
        List<CasePartConfigVo> pedestrianParts = CollectionUtils.emptyIfNull(pedestrianTrajectory).stream()
                .map(pedestrian -> {
                    CasePartConfigVo config = new CasePartConfigVo();
                    config.setBusinessId(pedestrian.getId());
                    config.setBusinessType(pedestrian.getType());
                    config.setName(dictDataService.selectDictLabel(SysType.PART_TYPE, pedestrian.getType())
                            + pedestrian.getId());
                    return config;
        }).collect(Collectors.toList());
        result.addAll(pedestrianParts);
        // todo 障碍物
        return result.stream().collect(Collectors.groupingBy(CasePartConfigVo::getBusinessType));
    }
}
