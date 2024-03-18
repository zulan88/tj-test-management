package net.wanji.web.controller.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import io.swagger.models.auth.In;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.*;
import net.wanji.business.entity.InfinteMileScence;
import net.wanji.business.entity.TjAtlasVenue;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.ITjAtlasVenueService;
import net.wanji.business.service.InfinteMileScenceService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.core.redis.RedisCache;
import net.wanji.common.utils.SecurityUtils;
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
    private ITjAtlasVenueService tjAtlasVenueService;

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<InfinteMileScenceExo> list = infinteMileScenceService.selectInfinteMileScenceList();
        List<InfinteMileScenceExo> infinteMileScenceExoList = list.stream().peek(infinteMileScenceExo -> {
            infinteMileScenceService.dualInfiniteSimulation(infinteMileScenceExo);
            if (infinteMileScenceExo.getInElements()!= null) {
                long testNum = infinteMileScenceExo.getInElements().stream().filter(inElement -> inElement.getType().equals(0)).count();
                infinteMileScenceExo.setTestNum(testNum);
                infinteMileScenceExo.setOtherNum(infinteMileScenceExo.getInElements().size() - testNum);
            }
        }).collect(Collectors.toList());
        return getDataTable(infinteMileScenceExoList, list);
    }

    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(infinteMileScenceService.buildSceneNumber());
    }

    @PostMapping("/save")
    public AjaxResult save(@RequestBody InfinteMileScenceExo infinteMileScence) {
        Integer id = infinteMileScenceService.saveInfinteMileScence(infinteMileScence);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        return AjaxResult.success(jsonObject);
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

    @GetMapping("/getflowsite")
    public AjaxResult getflowsite(@RequestParam("mapId") Integer mapId) throws BusinessException {
//        TjAtlasVenue tjAtlasVenue = tjAtlasVenueService.getById(mapId);
//        List<TrafficFlow> jsonArray = JSON.parseArray(tjAtlasVenue.getAttribute3(), TrafficFlow.class);
        List<TrafficFlow> list = infinteMileScenceService.getTrafficFlow(mapId);
        return AjaxResult.success(list);
    }

    @GetMapping("/getinfo")
    public AjaxResult test(@RequestParam("id") Integer id) {
        InfinteMileScenceExo infinteMileScenceExo = infinteMileScenceService.selectInfinteMileScenceById(id);
        return AjaxResult.success(infinteMileScenceExo);
    }

    @GetMapping("/simustop")
    public AjaxResult simustop(@RequestParam("id") Integer id) throws BusinessException {
        return toAjax(infinteMileScenceService.stopInfinteSimulation(id));
    }

}
