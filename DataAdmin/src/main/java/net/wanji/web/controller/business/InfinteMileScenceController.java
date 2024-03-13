package net.wanji.web.controller.business;

import com.google.gson.Gson;
import net.wanji.business.domain.*;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.bo.TrajectoryDetailBo;
import net.wanji.business.entity.InfinteMileScence;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.InfinteMileScenceService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.core.redis.RedisCache;
import net.wanji.common.utils.SecurityUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/infintemile")
public class InfinteMileScenceController extends BaseController {

    @Autowired
    private InfinteMileScenceService infinteMileScenceService;

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<InfinteMileScence> list = infinteMileScenceService.selectInfinteMileScenceList();
        Gson gson = new Gson();
        List<InfinteMileScenceExo> infinteMileScenceExoList = list.stream().map(item -> {
            InfinteMileScenceExo infinteMileScenceExo = new InfinteMileScenceExo();
            BeanUtils.copyProperties(item, infinteMileScenceExo);
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
            return infinteMileScenceExo;
        }).collect(Collectors.toList());
        return getDataTable(infinteMileScenceExoList, list);
    }

    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(infinteMileScenceService.buildSceneNumber());
    }

    @PostMapping("/save")
    public AjaxResult save(@RequestBody InfinteMileScenceExo infinteMileScence) {
        return toAjax(infinteMileScenceService.saveInfinteMileScence(infinteMileScence));
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return toAjax(infinteMileScenceService.removeById(id));
    }

    @PostMapping("/debugging")
    public AjaxResult debugging(@RequestBody InfinteMileScenceExo infinteMileScence) throws BusinessException {
        String repeatKey = "DEBUGGING_INSCENE_" + infinteMileScence.getViewId();
        if (redisCache.hasKey(repeatKey) && !redisCache.getCacheObject(repeatKey).equals(SecurityUtils.getUsername())) {
            return AjaxResult.error("有其他用户正在调试该场景，请稍后再试");
        }
        redisCache.setCacheObject(repeatKey, SecurityUtils.getUsername(), 3, TimeUnit.MINUTES);
        String key = "DEBUGGING_SUBMIT_" + infinteMileScence.getViewId();
        if (!redisCache.lock(key, key, 10)) {
            return AjaxResult.error("正在连接仿真软件，请稍后再试");
        }
        infinteMileScenceService.debugging(infinteMileScence);
        redisCache.unlock2(key, key);
        return AjaxResult.success();
    }

}
