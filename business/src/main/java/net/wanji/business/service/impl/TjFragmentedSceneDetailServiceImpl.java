package net.wanji.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.MapKey;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 片段式场景定义 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-06-27
 */
@Service
public class TjFragmentedSceneDetailServiceImpl
        extends ServiceImpl<TjFragmentedSceneDetailMapper, TjFragmentedSceneDetail>
        implements TjFragmentedSceneDetailService {

    @Autowired
    private TjFragmentedScenesService scenesService;

    @Autowired
    private TjFragmentedScenesMapper scenesMapper;

    @Autowired
    private TjFragmentedSceneDetailMapper sceneDetailMapper;

    @Autowired
    private ISysDictDataService dictDataService;

    @Override
    public FragmentedScenesDetailVo getDetailVo(Integer sceneId) throws BusinessException {
        TjFragmentedScenes scenes = scenesMapper.selectById(sceneId);
        if (ObjectUtils.isEmpty(scenes)) {
            throw new BusinessException("场景不存在");
        }
        QueryWrapper<TjFragmentedSceneDetail> queryWrapper = new QueryWrapper();
        queryWrapper.eq(ColumnName.SCENE_DETAIL_ID_COLUMN, sceneId);
        TjFragmentedSceneDetail detail = this.getOne(queryWrapper);
        FragmentedScenesDetailVo detailVo = new FragmentedScenesDetailVo();
        if (YN.N_INT == scenes.getIsFolder() && !ObjectUtils.isEmpty(detail)) {
            BeanUtils.copyBeanProp(detailVo, detail);
            detailVo.setRoadTypeName(dictDataService.selectDictLabel(SysType.SCENE_TREE_TYPE, scenes.getType()));
            detailVo.setRoadWayName(dictDataService.selectDictLabel(SysType.ROAD_WAY_TYPE, detail.getRoadWay()));
        }
        return detailVo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveSceneDetail(TjFragmentedSceneDetailDto sceneDetailDto) throws BusinessException {
        verifyTrajectoryJson(sceneDetailDto.getTrajectoryJson());
        TjFragmentedSceneDetail detail = new TjFragmentedSceneDetail();
        BeanUtils.copyBeanProp(detail, sceneDetailDto);
        detail.setLabel(String.join(",", sceneDetailDto.getLabelList()));
        detail.setTrajectoryInfo(JSONObject.toJSONString(sceneDetailDto.getTrajectoryJson()));
        boolean success = this.saveOrUpdate(detail);
        if (!ObjectUtils.isEmpty(detail.getTrajectoryInfo()) && !ObjectUtils.isEmpty(detail.getResourcesDetailId())) {
            success = scenesService.completeScene(sceneDetailDto);
        }
        return success;
    }

    @Override
    public List<TjFragmentedSceneDetail> selectScene(SceneQueryDto queryDto) {
        QueryWrapper<TjFragmentedSceneDetail> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(queryDto.getFragmentedSceneId())) {
            queryWrapper.eq("fragmented_scene_id", queryDto.getFragmentedSceneId());
        }
        if (ObjectUtil.isNotEmpty(queryDto.getResourcesDetailId())) {
            queryWrapper.in("resources_detail_id", queryDto.getResourcesDetailId().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getSceneType())) {
            queryWrapper.in("scene_type", queryDto.getSceneType().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getSceneComplexity())) {
            queryWrapper.in("scene_complexity", queryDto.getSceneComplexity().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getTrafficFlowStatus())) {
            queryWrapper.in("traffic_flow_status", queryDto.getTrafficFlowStatus().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getRoadType())) {
            queryWrapper.in("road_type", queryDto.getRoadType().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getWeather())) {
            queryWrapper.in("weather", queryDto.getWeather().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getRoadCondition())) {
            queryWrapper.in("road_condition",queryDto.getRoadCondition().split(","));
        }
        if (ObjectUtil.isNotEmpty(queryDto.getCollectStatus())) {
            queryWrapper.eq("collect_status", queryDto.getCollectStatus());
        }

        return sceneDetailMapper.selectList(queryWrapper);
    }

    private void verifyTrajectoryJson(Map trajectoryJson) throws BusinessException {
        if (ObjectUtils.isEmpty(trajectoryJson)) {
            throw new BusinessException("请填写轨迹信息");
        }
        if (!trajectoryJson.containsKey(MapKey.VEHICLE_KEY)
                && !trajectoryJson.containsKey(MapKey.PEDESTRIAN_KEY)
                && !trajectoryJson.containsKey(MapKey.OBSTACLE_KEY)) {
            throw new BusinessException("请至少包含一类轨迹");
        }
    }
}
