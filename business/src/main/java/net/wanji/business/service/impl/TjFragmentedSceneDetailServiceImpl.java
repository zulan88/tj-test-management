package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.PointTypeEnum;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.domain.dto.SceneDebugDto;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjFragmentedSceneDetailMapper;
import net.wanji.business.mapper.TjFragmentedScenesMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.service.*;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.trajectory.RedisTrajectory2Consumer;
import net.wanji.common.common.TrajectoryValueDto;
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

import java.io.IOException;
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

    @Autowired
    private RestService restService;

    @Autowired
    private RedisTrajectory2Consumer redisTrajectoryConsumer;

    @Autowired
    private RouteService routeService;

    @Override
    public FragmentedScenesDetailVo getDetailVo(Integer id) throws BusinessException {
        QueryWrapper<TjFragmentedSceneDetail> queryWrapper = new QueryWrapper();
        queryWrapper.eq(ColumnName.SCENE_DETAIL_ID_COLUMN, id);
        TjFragmentedSceneDetail detail = this.getOne(queryWrapper);
        FragmentedScenesDetailVo detailVo = new FragmentedScenesDetailVo();
        if (null != detail && detail.getFragmentedSceneId() != null) {
            TjFragmentedScenes scenes = scenesMapper.selectById(detail.getFragmentedSceneId());
            if (ObjectUtils.isEmpty(scenes)) {
                throw new BusinessException("场景不存在");
            }
            if (YN.N_INT == scenes.getIsFolder() && !ObjectUtils.isEmpty(detail)) {
                BeanUtils.copyBeanProp(detailVo, detail);
                detailVo.setTypeName(dictDataService.selectDictLabel(SysType.SCENE_TREE_TYPE, scenes.getType()));
//                detailVo.setRoadWayName(dictDataService.selectDictLabel(SysType.ROAD_WAY_TYPE, detail.getRoadWayType()));
//                detailVoTranslate(detailVo);
            }
        }
        return detailVo;
    }

    @Override
    public void playback(Integer id, String participantId, int action) throws BusinessException, IOException {
        FragmentedScenesDetailVo caseInfoBo = this.getDetailVo(id);
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), String.valueOf(id), WebSocketManage.SIMULATION, null);
        switch (action) {
            case PlaybackAction.START:
                if (StringUtils.isEmpty(caseInfoBo.getRouteFile())) {
                    throw new BusinessException("未进行仿真验证");
                }
                List<List<TrajectoryValueDto>> routeList = routeService.readTrajectoryFromRouteFile(
                        caseInfoBo.getRouteFile(), participantId);
                if (CollectionUtils.isEmpty(routeList)) {
                    throw new BusinessException("轨迹文件读取异常");
                }
                PlaybackSchedule.startSendingData(key, routeList);
                break;
            case PlaybackAction.SUSPEND:
                PlaybackSchedule.suspend(key);
                break;
            case PlaybackAction.CONTINUE:
                PlaybackSchedule.goOn(key);
                break;
            case PlaybackAction.STOP:
                PlaybackSchedule.stopSendingData(key);
                break;
            default:
                break;

        }

    }

    @Override
    public List<List<TrajectoryValueDto>> getroutelist(Integer id, String participantId) throws IOException, BusinessException {
        FragmentedScenesDetailVo caseInfoBo = this.getDetailVo(id);
        return routeService.readTrajectoryFromRouteFile(
                caseInfoBo.getRouteFile(), participantId);
    }

    @Override
    public List<String> getalllabel(String id) {
        return sceneDetailMapper.getalllabel(id);
    }

    @Override
    public List<SceneDetailVo> selectTjSceneDetailListBylabels(List<List<Integer>> lists) {
        return sceneDetailMapper.selectTjSceneDetailListBylabels(lists);
    }

    @Override
    public List<SceneDetailVo> selectTjSceneDetailListAnd(List<Integer> labellist) {
        return sceneDetailMapper.selectTjSceneDetailListAnd(labellist);
    }

    @Override
    public List<SceneDetailVo> selectTjSceneDetailListOr(List<Integer> labellist) {
        return sceneDetailMapper.selectTjSceneDetailListOr(labellist);
    }

    private void detailVoTranslate(FragmentedScenesDetailVo detailVo) {
        // 地图
        TjResourcesDetail tjResourcesDetail = tjResourcesDetailService.getById(
                detailVo.getResourcesDetailId());
        detailVo.setResourcesName(tjResourcesDetail.getName());
        detailVo.setFilePath(tjResourcesDetail.getFilePath());
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
        TjFragmentedSceneDetail detail = new TjFragmentedSceneDetail();
        BeanUtils.copyBeanProp(detail, sceneDetailDto);
        if (ObjectUtils.isEmpty(sceneDetailDto.getId())) {
            detail.setNumber(StringUtils.isEmpty(sceneDetailDto.getNumber()) ? buildSceneNumber() : sceneDetailDto.getNumber());
            detail.setCreatedBy(SecurityUtils.getUsername());
            detail.setCreatedDate(LocalDateTime.now());
        } else {
            detail.setUpdatedBy(SecurityUtils.getUsername());
            detail.setUpdatedDate(LocalDateTime.now());
        }
//        if (StringUtils.isNotEmpty(sceneDetailDto.getRouteFile())) {
//            detail.setFinished(true);
//        }
        List<String> labellist = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(sceneDetailDto.getLabelList())) {
            for (String id : sceneDetailDto.getLabelList()) {
                labellist.addAll(this.getalllabel(id));
            }
        }
        if(Integer.valueOf(1).equals(sceneDetailDto.getFinished())){
            detail.setFinished(true);
        }
        detail.setLabel(CollectionUtils.isNotEmpty(sceneDetailDto.getLabelList())
                ? String.join(",", sceneDetailDto.getLabelList())
                : null);
        detail.setAllStageLabel(CollectionUtils.isNotEmpty(labellist)
                ? labellist.stream().distinct().collect(Collectors.joining(","))
                : null);
        detail.setTrajectoryInfo(!ObjectUtils.isEmpty(sceneDetailDto.getTrajectoryJson())
                ? sceneDetailDto.getTrajectoryJson().buildId().toJsonString()
                : null);

        boolean flag = this.saveOrUpdate(detail);
        sceneDetailDto.setId(detail.getId());

        return flag;
    }

    public synchronized String buildSceneNumber() {
        return StringUtils.format(ContentTemplate.SCENE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getRandomChar());
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
            scenesService.selectChildrenFromFolder(scenes.getId(), collector);
            List<Integer> sceneIds = collector.stream().map(TjFragmentedScenes::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(sceneIds)) {
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

    @Override
    public void debugging(SceneDebugDto sceneDebugDto) throws BusinessException, IOException {
        String key = WebSocketManage.buildKey(SecurityUtils.getUsername(), sceneDebugDto.getNumber(),
                WebSocketManage.SIMULATION, null);
        switch (sceneDebugDto.getAction()) {
            case PlaybackAction.START:
                validDebugParam(sceneDebugDto);
                sceneDebugDto.getTrajectoryJson().buildId();
                TestStartParam testStartParam = buildStartParam(sceneDebugDto);
                TjFragmentedScenes scenes = scenesService.getById(sceneDebugDto.getFragmentedSceneId());
                if (ObjectUtils.isEmpty(scenes)) {
                    throw new BusinessException("场景节点不存在");
                }
                sceneDebugDto.getTrajectoryJson().setSceneDesc(scenes.getName());
                sceneDebugDto.getTrajectoryJson().setSceneForm(StringUtils.format(ContentTemplate.SCENE_FORM_TEMPLATE, testStartParam.getAvNum(),
                        testStartParam.getSimulationNum(), testStartParam.getPedestrianNum()));
                redisTrajectoryConsumer.subscribeAndSend(sceneDebugDto);
                boolean start = restService.start(testStartParam);
                if (!start) {
                    throw new BusinessException("仿真程序连接失败");
                }
                break;
            case PlaybackAction.SUSPEND:
                PlaybackSchedule.suspend(key);
                break;
            case PlaybackAction.CONTINUE:
                PlaybackSchedule.goOn(key);
                break;
            case PlaybackAction.STOP:
//                WebSocketManage.remove(key);
                redisTrajectoryConsumer.removeListener(key);
                break;
            default:
                break;

        }
    }

    public void validDebugParam(SceneDebugDto sceneDebugDto) throws BusinessException {
        List<ParticipantTrajectoryBo> participantTrajectories = sceneDebugDto.getTrajectoryJson().getParticipantTrajectories();
        if (CollectionUtils.isEmpty(participantTrajectories)) {
            throw new BusinessException("参与者轨迹不能为空");
        }
        for (ParticipantTrajectoryBo participantTrajectory : participantTrajectories) {
            List<TrajectoryDetailBo> trajectory = participantTrajectory.getTrajectory();
            if (CollectionUtils.isEmpty(trajectory)) {
                throw new BusinessException("参与者轨迹不能为空");
            }
            if (trajectory.size() < 2) {
                throw new BusinessException("参与者轨迹点不能少于2个");
            }
            if (!PointTypeEnum.START.getPointType().equals(trajectory.get(0).getType())) {
                throw new BusinessException("参与者轨迹点必须以起点开始");
            }
            if (!PointTypeEnum.END.getPointType().equals(trajectory.get(trajectory.size() - 1).getType())) {
                throw new BusinessException("参与者轨迹点必须以终点结束");
            }
            if (trajectory.stream().anyMatch(t -> StringUtils.isEmpty(t.getPosition()))) {
                throw new BusinessException("参与者轨迹点位置不能为空");
            }
        }

    }


    public TestStartParam buildStartParam(SceneDebugDto sceneDebugDto) {
        SceneTrajectoryBo sceneTrajectoryBo = sceneDebugDto.getTrajectoryJson();
        // 计算caseInfo.getCaseConfigs()中各个角色的数量
        long avNum = (int) sceneTrajectoryBo.getParticipantTrajectories().stream().filter(trajectoryBo ->
                PartType.MAIN.equals(trajectoryBo.getType())).count();
        long simulationNum = (int) sceneTrajectoryBo.getParticipantTrajectories().stream().filter(trajectoryBo ->
                PartType.SLAVE.equals(trajectoryBo.getType())).count();
        long pedestrianNum = (int) sceneTrajectoryBo.getParticipantTrajectories().stream().filter(trajectoryBo ->
                PartType.PEDESTRIAN.equals(trajectoryBo.getType())).count();

        for (ParticipantTrajectoryBo participantTrajectory : sceneTrajectoryBo.getParticipantTrajectories()) {
            for (TrajectoryDetailBo trajectoryDetailBo : participantTrajectory.getTrajectory()) {
                String[] pos = trajectoryDetailBo.getPosition().split(",");
                if (!ObjectUtils.isEmpty(pos)) {
                    trajectoryDetailBo.setLongitude(pos[0]);
                    trajectoryDetailBo.setLatitude(pos[1]);
                }
            }
        }
        return new TestStartParam(1,
                WebSocketManage.buildKey(SecurityUtils.getUsername(), sceneDebugDto.getNumber(),
                        WebSocketManage.SIMULATION, null), (int) avNum, (int) simulationNum,
                (int) pedestrianNum, sceneTrajectoryBo.getParticipantTrajectories());
    }

    @Override
    public List<SceneDetailVo> selectTjFragmentedSceneDetailList(SceneDetailVo sceneDetailVo){
        return sceneDetailMapper.selectTjFragmentedSceneDetailList(sceneDetailVo);
    }

    @Override
    public boolean deleteSceneByIds(Integer[] ids) throws BusinessException {
        int count = caseMapper.selectCountBySceneDetailIds(Arrays.asList(ids));
        if (count > 0) {
            throw new BusinessException("当前子场景下存在测试用例");
        }
        return this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public boolean updateBatch(TjFragmentedSceneDetail sceneDetail){
        return this.update().update(sceneDetail);
    }


}
