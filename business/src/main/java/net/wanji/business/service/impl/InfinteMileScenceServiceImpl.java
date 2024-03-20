package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.*;
import net.wanji.business.domain.dto.SceneDebugDto;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.param.TessParam;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.entity.InfinteMileScence;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.InfinteMileScenceMapper;
import net.wanji.business.mapper.TjDeviceDetailMapper;
import net.wanji.business.schedule.PlaybackSchedule;
import net.wanji.business.service.InfinteMileScenceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.service.RestService;
import net.wanji.business.trajectory.RedisTrajectory2Consumer;
import net.wanji.common.core.redis.RedisCache;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.SecurityUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wj
 * @since 2024-02-23
 */
@Service
public class InfinteMileScenceServiceImpl extends ServiceImpl<InfinteMileScenceMapper, InfinteMileScence> implements InfinteMileScenceService {

    @Autowired
    private TjDeviceDetailMapper deviceDetailMapper;

    @Autowired
    private RedisTrajectory2Consumer redisTrajectoryConsumer;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private RestService restService;

    @Override
    public List<InfinteMileScenceExo> selectInfinteMileScenceList() {
        List<InfinteMileScenceExo> list = baseMapper.selectInfinteMileScenceExo();
        return list;
    }

    @Override
    public synchronized String buildSceneNumber() {
        return StringUtils.format(Constants.ContentTemplate.INFINTE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getRandomChar());
    }

    @Override
    public Integer saveInfinteMileScence(InfinteMileScenceExo infinteMileScence) {
        Gson gson = new Gson();
        if (infinteMileScence.getInElements()!= null && infinteMileScence.getInElements().size() > 0){
            infinteMileScence.setElement(gson.toJson(infinteMileScence.getInElements()));
        }
        if (infinteMileScence.getTrafficFlows()!= null && infinteMileScence.getTrafficFlows().size() > 0){
            infinteMileScence.setTrafficFlow(gson.toJson(infinteMileScence.getTrafficFlows()));
        }
        if (infinteMileScence.getSiteSlices()!= null && infinteMileScence.getSiteSlices().size() > 0){
            List<SiteSlice> siteSlices = new ArrayList<>();
            for (SiteSlice slice : infinteMileScence.getSiteSlices()) {
                SiteSlice exo = new SiteSlice();
                exo.setSliceId(slice.getSliceId());
                exo.setRoute(slice.getRoute());
                exo.setSliceName(slice.getSliceName());
                siteSlices.add(exo);
                if (slice.getImgData() != null) {
                    String data = slice.getImgData().replace("/[\r\n]/g","");
                    slice.setImgData(data);
                }
            }
            infinteMileScence.setSiteSlice(gson.toJson(siteSlices));
            infinteMileScence.setSliceImg(gson.toJson(infinteMileScence.getSiteSlices()));
        }
        if(infinteMileScence.getTrafficFlowConfigs()!= null && infinteMileScence.getTrafficFlowConfigs().size() > 0){
            infinteMileScence.setTrafficFlowConfig(gson.toJson(infinteMileScence.getTrafficFlowConfigs()));
        }
        if (null == infinteMileScence.getId()){
            infinteMileScence.setCreateDate(LocalDateTime.now());
            this.save(infinteMileScence);
        }else{
            Map<Integer, SiteSlice> sliceMap = this.getSiteSlice(infinteMileScence.getId()).stream().collect(Collectors.toMap(SiteSlice::getSliceId, siteSlice -> siteSlice));
            List<SiteSlice> siteSlices = Arrays.asList(gson.fromJson(infinteMileScence.getSliceImg(), SiteSlice[].class));
            for (SiteSlice slice : siteSlices) {
                if (sliceMap.containsKey(slice.getSliceId())) {
                    if (slice.getImgData() == null) {
                        String data = sliceMap.get(slice.getSliceId()).getImgData();
                        slice.setImgData(data);
                    }
                }
            }
            infinteMileScence.setSliceImg(gson.toJson(siteSlices));
            infinteMileScence.setUpdateDate(LocalDateTime.now());
            this.updateById(infinteMileScence);
        }
        return infinteMileScence.getId();
    }

    @Override
    public void debugging(InfinteMileScenceExo infinteMileScence) throws BusinessException {
        validDebugParam(infinteMileScence);
        InfinteMileScence scenes = this.getById(infinteMileScence.getId());
        if (ObjectUtils.isEmpty(scenes)) {
            throw new BusinessException("模板信息不存在，请先保存");
        }
        TjDeviceDetailDto deviceDetailDto = new TjDeviceDetailDto();
        deviceDetailDto.setSupportRoles(Constants.PartRole.MV_SIMULATION);
        List<DeviceDetailVo> deviceDetailVos = deviceDetailMapper.selectByCondition(deviceDetailDto);
        if (CollectionUtils.isEmpty(deviceDetailVos)) {
            throw new BusinessException("当前无可用仿真程序");
        }
        BeanUtils.copyBeanProp(infinteMileScence, scenes);

        redisTrajectoryConsumer.addRunningChannelInfinite(infinteMileScence);

        List<String> mapList = new ArrayList<>();
        if (ObjectUtils.isEmpty(infinteMileScence.getMapId())) {
            mapList.add("10");
        }else {
            mapList.add(String.valueOf(infinteMileScence.getMapId()));
        }

        List<TrafficFlow> trafficFlows = new ArrayList<>();
        for(TrafficFlow trafficFlow : infinteMileScence.getTrafficFlows()){
            if (trafficFlow.getDeparturePoints()!= null && trafficFlow.getDeparturePoints().size() > 0){
                trafficFlows.add(trafficFlow);
            }
        }

        InfiniteTessParm testStartParam = new InfiniteTessParm();
        testStartParam.setInElements(infinteMileScence.getInElements());
        testStartParam.setTrafficFlows(trafficFlows);
        testStartParam.setTrafficFlowConfigs(infinteMileScence.getTrafficFlowConfigs());
        for(InElement element : testStartParam.getInElements()){
            SitePoint firstpoint = element.getRoute().get(0);
            SitePoint point = new SitePoint();
            point.setLatitude(firstpoint.getLatitude());
            point.setLongitude(firstpoint.getLongitude());
            element.getRoute().add(point);
        }

        String channel = Constants.ChannelBuilder.buildInfiniteSimulationChannel(SecurityUtils.getUsername(), infinteMileScence.getViewId());
        DeviceDetailVo detailVo = deviceDetailVos.get(0);
        boolean start = restService.startServer(detailVo.getIp(), Integer.valueOf(detailVo.getServiceAddress()),
                new TessParam().buildnfiniteSimulationParam("1", channel, testStartParam, mapList));
        if (!start) {
            String repeatKey = "DEBUGGING_INSCENE_" + infinteMileScence.getViewId();
            redisCache.deleteObject(repeatKey);
            throw new BusinessException("仿真程序连接失败");
        }

    }

    @Override
    public InfinteMileScenceExo selectInfinteMileScenceById(Integer id) {
        InfinteMileScence infinteMileScence = this.getById(id);
        InfinteMileScenceExo infinteMileScenceExo = new InfinteMileScenceExo();
        BeanUtils.copyBeanProp(infinteMileScenceExo, infinteMileScence);
        dualInfiniteSimulation(infinteMileScenceExo);
        return infinteMileScenceExo;
    }

    @Override
    public InfinteMileScenceExo selectInfinteMileScenceById2(Integer id) {
        InfinteMileScence infinteMileScence = this.getById(id);
        InfinteMileScenceExo infinteMileScenceExo = new InfinteMileScenceExo();
        infinteMileScenceExo.setRouteFile(infinteMileScence.getRouteFile());
        infinteMileScenceExo.setMapFile(infinteMileScence.getMapFile());
        infinteMileScenceExo.setMapId(infinteMileScence.getMapId());
        return infinteMileScenceExo;
    }

    private void validDebugParam(InfinteMileScenceExo infinteMileScenceExo) throws BusinessException{
        if (infinteMileScenceExo.getInElements()== null || infinteMileScenceExo.getInElements().size() == 0){
            throw new BusinessException("参与者轨迹不能为空");
        }else if (infinteMileScenceExo.getTrafficFlows()== null || infinteMileScenceExo.getTrafficFlows().size() == 0){
            throw new BusinessException("车流量信息不能为空");
        }
    }

    public void dualInfiniteSimulation(InfinteMileScenceExo infinteMileScenceExo) {
        Gson gson = new Gson();
        if(infinteMileScenceExo.getElement()!= null&&infinteMileScenceExo.getElement().length() > 0){
            List<InElement> inElements = Arrays.asList(gson.fromJson(infinteMileScenceExo.getElement(), InElement[].class));
            infinteMileScenceExo.setInElements(inElements);
        }
        if(infinteMileScenceExo.getTrafficFlow()!= null&&infinteMileScenceExo.getTrafficFlow().length() > 0){
            List<TrafficFlow> trafficFlows = Arrays.asList(gson.fromJson(infinteMileScenceExo.getTrafficFlow(), TrafficFlow[].class));
            infinteMileScenceExo.setTrafficFlows(trafficFlows);
        }
        if(infinteMileScenceExo.getSiteSlice()!= null&&infinteMileScenceExo.getSiteSlice().length() > 0){
            List<SiteSlice> siteSlices = Arrays.asList(gson.fromJson(infinteMileScenceExo.getSiteSlice(), SiteSlice[].class));
            infinteMileScenceExo.setSiteSlices(siteSlices);
        }
        if(infinteMileScenceExo.getTrafficFlowConfig()!= null&&infinteMileScenceExo.getTrafficFlowConfig().length() > 0){
            List<TrafficFlowConfig> trafficFlowConfigs = Arrays.asList(gson.fromJson(infinteMileScenceExo.getTrafficFlowConfig(), TrafficFlowConfig[].class));
            infinteMileScenceExo.setTrafficFlowConfigs(trafficFlowConfigs);
        }
    }

    @Override
    public boolean stopInfinteSimulation(Integer id) throws BusinessException {
        InfinteMileScence infinteMileScence = this.getById(id);
        String channel = Constants.ChannelBuilder.buildInfiniteSimulationChannel(SecurityUtils.getUsername(), infinteMileScence.getViewId());
        TjDeviceDetailDto deviceDetailDto = new TjDeviceDetailDto();
        deviceDetailDto.setSupportRoles(Constants.PartRole.MV_SIMULATION);
        List<DeviceDetailVo> deviceDetailVos = deviceDetailMapper.selectByCondition(deviceDetailDto);
        if (CollectionUtils.isEmpty(deviceDetailVos)) {
            throw new BusinessException("当前无可用仿真程序");
        }
        DeviceDetailVo detailVo = deviceDetailVos.get(0);
        return restService.stopInfinite(detailVo.getIp(), detailVo.getServiceAddress(),channel);
    }

    @Override
    public List<TrafficFlow> getTrafficFlow(Integer maoid) throws BusinessException {
        TjDeviceDetailDto deviceDetailDto = new TjDeviceDetailDto();
        deviceDetailDto.setSupportRoles(Constants.PartRole.MV_SIMULATION);
        List<DeviceDetailVo> deviceDetailVos = deviceDetailMapper.selectByCondition(deviceDetailDto);
        if (CollectionUtils.isEmpty(deviceDetailVos)) {
            throw new BusinessException("当前无可用仿真程序");
        }
        DeviceDetailVo detailVo = deviceDetailVos.get(0);
        List<TrafficFlow> list =  restService.getTrafficFlow(detailVo.getIp(), detailVo.getServiceAddress(),maoid);
        return list;
    }

    @Override
    public List<SiteSlice> getSiteSlice(Integer id) {
        Gson gson = new Gson();
        List<SiteSlice> siteSlices = Arrays.asList(gson.fromJson(baseMapper.getSliceImage(id), SiteSlice[].class));
        return siteSlices;
    }

}
