package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.business.service.TjResourcesDetailService;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private TjCaseMapper caseMapper;

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private TjResourcesDetailService tjResourcesDetailService;

    @Override
    public FragmentedScenesDetailVo getDetailVo(Integer id) throws BusinessException {
        QueryWrapper<TjFragmentedSceneDetail> queryWrapper = new QueryWrapper();
        queryWrapper.eq(ColumnName.SCENE_DETAIL_ID_COLUMN, id);
        TjFragmentedSceneDetail detail = this.getOne(queryWrapper);
        FragmentedScenesDetailVo detailVo = new FragmentedScenesDetailVo();
        if(null != detail && detail.getFragmentedSceneId() != null){
            TjFragmentedScenes scenes = scenesMapper.selectById(detail.getFragmentedSceneId());
            if (ObjectUtils.isEmpty(scenes)) {
                throw new BusinessException("场景不存在");
            }
            if (YN.N_INT == scenes.getIsFolder() && !ObjectUtils.isEmpty(detail)) {
                BeanUtils.copyBeanProp(detailVo, detail);
                detailVo.setTypeName(dictDataService.selectDictLabel(SysType.SCENE_TREE_TYPE, scenes.getType()));
                detailVo.setRoadWayName(dictDataService.selectDictLabel(SysType.ROAD_WAY_TYPE, detail.getRoadWayType()));
                detailVoTranslate(detailVo);
            }
        }
        return detailVo;
    }

    private void detailVoTranslate(FragmentedScenesDetailVo detailVo) {
        // 地图
        TjResourcesDetail tjResourcesDetail = tjResourcesDetailService.getById(
            detailVo.getResourcesDetailId());
        detailVo.setResourcesName(tjResourcesDetail.getName());
        detailVo.setGeoJsonPath(tjResourcesDetail.getAttribute4());
        // 道路类型
        detailVo.setRoadTypeName(dictDataService.selectDictLabel(SysType.ROAD_TYPE
            , detailVo.getRoadType()));
        // 场景复杂度
        detailVo.setSceneComplexityName(dictDataService.selectDictLabel(SysType.SCENE_COMPLEXITY
            , detailVo.getSceneComplexity()));
        // 交通流状态
        detailVo.setTrafficFlowStatusName(dictDataService.selectDictLabel(SysType.TRAFFIC_FLOW_STATUS
            , detailVo.getTrafficFlowStatus()));
        // 路面状况
        detailVo.setRoadConditionName(dictDataService.selectDictLabel(SysType.ROAD_CONDITION
            , detailVo.getRoadCondition()));
        // 天气
        detailVo.setWeatherName(dictDataService.selectDictLabel(SysType.WEATHER
            , detailVo.getWeather()));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveSceneDetail(TjFragmentedSceneDetailDto sceneDetailDto) throws BusinessException {
        TjFragmentedScenes scenes = scenesMapper.selectById(sceneDetailDto.getFragmentedSceneId());
        if (ObjectUtils.isEmpty(scenes)) {
            throw new BusinessException("场景未找到");
        }
        if (YN.Y_INT == scenes.getIsFolder()) {
            throw new BusinessException("文件夹下无法保存子场景");
        }
        if (ObjectUtils.isEmpty(sceneDetailDto.getId())) {
            TjFragmentedSceneDetail detail = new TjFragmentedSceneDetail();
            BeanUtils.copyBeanProp(detail, sceneDetailDto);
            detail.setLabel(String.join(",", sceneDetailDto.getLabelList()));
            detail.setNumber(this.buildSceneNumber());
            detail.setCollectStatus(YN.N_INT);
            detail.setCreatedBy(SecurityUtils.getUsername());
            detail.setCreatedDate(LocalDateTime.now());
            return this.save(detail);
        }
        TjFragmentedSceneDetail detail = new TjFragmentedSceneDetail();
        BeanUtils.copyBeanProp(detail, sceneDetailDto);
        if (CollectionUtils.isNotEmpty(sceneDetailDto.getLabelList())) {
            detail.setLabel(String.join(",", sceneDetailDto.getLabelList()));
        }
        if (!ObjectUtils.isEmpty(sceneDetailDto.getTrajectoryJson())) {
            detail.setTrajectoryInfo(JSONObject.toJSONString(sceneDetailDto.getTrajectoryJson()));
        }
        detail.setUpdatedBy(SecurityUtils.getUsername());
        detail.setUpdatedDate(LocalDateTime.now());
        return this.updateById(detail);
    }

    @Override
    public List<FragmentedScenesDetailVo> selectScene(SceneQueryDto queryDto) throws BusinessException {
        TjFragmentedScenes scenes = scenesMapper.selectById(queryDto.getFragmentedSceneId());
        if (ObjectUtils.isEmpty(scenes)) {
            throw new BusinessException("场景不存在");
        }
        String typeName = dictDataService.selectDictLabel(SysType.SCENE_TREE_TYPE, scenes.getType());

        List<FragmentedScenesDetailVo> detailVos = null;
        if (YN.Y_INT == scenes.getIsFolder()) {
            List<TjFragmentedScenes> collector = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(collector)) {
                scenesService.selectChildrenFromFolder(scenes.getId(), collector);
                List<Integer> sceneIds = collector.stream().map(TjFragmentedScenes::getId).collect(Collectors.toList());
                queryDto.setFragmentedSceneId(null);
                queryDto.setFragmentedSceneIds(sceneIds);
            }
        }
        detailVos = sceneDetailMapper.selectByCondition(queryDto);
        CollectionUtils.emptyIfNull(detailVos).forEach(detail -> {
            detail.setTypeName(typeName);
            detail.setRoadTypeName(dictDataService.selectDictLabel(SysType.ROAD_TYPE, detail.getRoadType()));
            detail.setRoadWayName(dictDataService.selectDictLabel(SysType.ROAD_WAY_TYPE, detail.getRoadWayType()));
            detail.setRoadConditionName(dictDataService.selectDictLabel(SysType.ROAD_CONDITION, detail.getRoadCondition()));
            detail.setWeatherName(dictDataService.selectDictLabel(SysType.WEATHER, detail.getWeather()));
            detail.setTrafficFlowStatusName(dictDataService.selectDictLabel(SysType.TRAFFIC_FLOW_STATUS,
                    detail.getTrafficFlowStatus()));
            detail.setSceneComplexityName(dictDataService.selectDictLabel(SysType.SCENE_COMPLEXITY,
                    detail.getSceneComplexity()));
            detail.setSceneTypeName(dictDataService.selectDictLabel(SysType.SCENE_TYPE, detail.getSceneType()));
        });
        return detailVos;
    }

    @Override
    public boolean deleteSceneDetail(Integer id) throws BusinessException {
        int count = caseMapper.selectCountBySceneDetailIds(Collections.singletonList(id));
        if (count > 0) {
            throw new BusinessException("当前子场景下存在测试用例");
        }
        return removeById(id);
    }

    public synchronized String buildSceneNumber() {
        return StringUtils.format(ContentTemplate.SCENE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.SCENE_NUMBER_TEMPLATE));
    }
}
