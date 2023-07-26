package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.MapKey;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
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
