package net.wanji.web.controller.business;

import com.alibaba.fastjson2.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.OtherGroup;
import net.wanji.business.common.Constants.UpdateGroup;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.dto.SceneDebugDto;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.domain.dto.TreeTypeDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.domain.vo.TagtoSceneVo;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.core.redis.RedisCache;
import net.wanji.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/27 16:25
 * @Descriptoin: 场景库控制器
 */
@RestController
@RequestMapping("/sceneBase")
@Api(tags = "场景创建-场景管理")
public class SceneBaseController extends BaseController {

    @Autowired
    private TjFragmentedScenesService tjFragmentedScenesService;

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;

    @Autowired
    private SceneLabelMap sceneLabelMap;

    @Autowired
    private RedisCache redisCache;

    @PostConstruct
    public void initClass(){
        sceneLabelMap.reset(2l);
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(JSON.toJSON(tjFragmentedScenesService.init()));
    }

    @ApiOperation("创建场景编号")
    //@PreAuthorize("@ss.hasPermi('sceneBase:buildSceneNumber')")
    @GetMapping("/buildSceneNumber")
    public AjaxResult buildSceneNumber() {
        return AjaxResult.success(JSON.toJSON(tjFragmentedScenesService.buildSceneNumber()));
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:initEditPage')")
    @GetMapping("/initEditPage")
    public AjaxResult initEditPage() {
        return AjaxResult.success(tjFragmentedScenesService.initEditPage());
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:saveTreeType')")
    @PostMapping("/saveTreeType")
    public AjaxResult saveTreeType(@Validated @RequestBody TreeTypeDto treeTypeDto) throws BusinessException {
        return tjFragmentedScenesService.saveSceneTreeType(treeTypeDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:deleteTreeType')")
    @GetMapping("/deleteTreeType/{dictCode}")
    public AjaxResult deleteTreeType(@PathVariable("dictCode") Long dictCode) throws BusinessException {
        return tjFragmentedScenesService.deleteTreeType(dictCode)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam(value = "type") String type,
                                 @RequestParam(value = "name", required = false) String name) {
        List<TjFragmentedScenes> usingScenes = tjFragmentedScenesService.selectUsingScenes(type);
        List<BusinessTreeSelect> tree = tjFragmentedScenesService.buildSceneTreeSelect(usingScenes, name);
        return AjaxResult.success(tree);
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:saveSceneTree')")
    @PostMapping("/saveSceneTree")
    public AjaxResult saveSceneTree(@Validated @RequestBody TjFragmentedScenesDto fragmentedScenesDto) {
        return tjFragmentedScenesService.saveSceneTree(fragmentedScenesDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:deleteScene')")
    @GetMapping("/deleteScene/{sceneId}")
    public AjaxResult deleteScene(@PathVariable("sceneId") Integer sceneId) throws BusinessException {
        return tjFragmentedScenesService.deleteSceneById(sceneId)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @DeleteMapping("/detail/{ids}")
    public AjaxResult remove(@PathVariable Integer[] ids) throws BusinessException {
        return toAjax(tjFragmentedSceneDetailService.deleteSceneByIds(ids));
    }

//    @PutMapping("/detailstatus")
//    public AjaxResult updatestatus(@RequestBody List<TjFragmentedSceneDetail> sceneDetails){
//        return toAjax(tjFragmentedSceneDetailService.updateBatch(sceneDetails));
//    }

    @ApiOperation("查询场景详情")
    //@PreAuthorize("@ss.hasPermi('sceneBase:getDetailVo')")
    @GetMapping("/getDetailVo/{id}")
    public AjaxResult getDetailVo(@PathVariable("id") Integer id) throws BusinessException {
        FragmentedScenesDetailVo detailVo = tjFragmentedSceneDetailService.getDetailVo(id);
        return AjaxResult.success(detailVo);
    }

    @ApiOperation("保存场景详情")
    //@PreAuthorize("@ss.hasPermi('sceneBase:saveSceneDetail')")
    @PostMapping("/saveSceneDetail")
    public AjaxResult saveSceneDetail(@Validated(value = {InsertGroup.class, UpdateGroup.class})
                                          @RequestBody TjFragmentedSceneDetailDto sceneDetailDto)
            throws BusinessException {
        return tjFragmentedSceneDetailService.saveSceneDetail(sceneDetailDto)
                ? AjaxResult.success(sceneDetailDto.getId())
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:saveSceneTrajectory')")
    @PostMapping("/saveSceneTrajectory")
    public AjaxResult saveSceneTrajectory(@Validated(value = OtherGroup.class)
                                              @RequestBody TjFragmentedSceneDetailDto sceneDetailDto)
            throws BusinessException {
        return tjFragmentedSceneDetailService.saveSceneDetail(sceneDetailDto)
                ? AjaxResult.success(sceneDetailDto.getId())
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:deleteSceneDetail')")
    @PostMapping("/deleteSceneDetail/{id}")
    public AjaxResult deleteSceneDetail(@PathVariable("id") Integer id) throws BusinessException {
        return tjFragmentedSceneDetailService.deleteSceneDetail(id)
            ? AjaxResult.success("成功")
            : AjaxResult.error("失败");
    }

    /**
     * 查询场景叶子节点下的子场景列表
     */
    @PostMapping("/selectScene")
    public AjaxResult selectScene(@Validated @RequestBody SceneQueryDto queryDto) throws BusinessException {
        return AjaxResult.success(tjFragmentedSceneDetailService.selectScene(queryDto));
    }

    @ApiOperation("场景调试")
    //@PreAuthorize("@ss.hasPermi('sceneBase:debugging')")
    @PostMapping("/debugging")
    public AjaxResult debugging(@Validated(value = OtherGroup.class) @RequestBody SceneDebugDto sceneDebugDto)
            throws BusinessException, IOException {
        String repeatKey = "DEBUGGING_SCENE_" + sceneDebugDto.getNumber();
        if (redisCache.hasKey(repeatKey) && !redisCache.getCacheObject(repeatKey).equals(SecurityUtils.getUsername())) {
            return AjaxResult.error("有其他用户正在调试该场景，请稍后再试");
        }
        redisCache.setCacheObject(repeatKey, SecurityUtils.getUsername(), 3, TimeUnit.MINUTES);
        String key = "DEBUGGING_SUBMIT_" + sceneDebugDto.getNumber();
        if (!redisCache.lock(key, key, 10)) {
            return AjaxResult.error("正在连接仿真软件，请稍后再试");
        }
        List<ParticipantTrajectoryBo> participantTrajectoryBos = sceneDebugDto.getTrajectoryJson()
                .getParticipantTrajectories().stream().filter(item -> item.getIsHide()!=null&&!item.getIsHide()).collect(Collectors.toList());
        tjFragmentedSceneDetailService.debugging(sceneDebugDto);
        redisCache.unlock2(key, key);
        return AjaxResult.success();
    }

    @ApiOperation("场景回放")
    @GetMapping("/playback")
    public AjaxResult playback(@RequestParam(value = "id") Integer id,
                               @RequestParam(value = "vehicleId", required = false) String vehicleId)
            throws BusinessException, IOException {
        tjFragmentedSceneDetailService.playback(id, vehicleId, 1);
        return AjaxResult.success();
    }

    @PostMapping("/scenelist")
    public TableDataInfo scenelist(@RequestBody SceneDetailVo sceneDetailVo) throws BusinessException {
        startPage();
        List<SceneDetailVo> list = tjFragmentedSceneDetailService.selectTjFragmentedSceneDetailList(sceneDetailVo);
        for(SceneDetailVo sceneDetailVo1 : list){
            String labels = sceneDetailVo1.getLabel();
            if(labels==null){
                continue;
            }
            StringBuilder labelshows = new StringBuilder();
            for (String str : labels.split(",")) {
                try {
                    long intValue = Long.parseLong(str);
                    String labelshow = sceneLabelMap.getSceneLabel(intValue);
                    if(labelshow!=null) {
                        if(labelshows.length()>0) {
                            labelshows.append(",").append(labelshow);
                        }else {
                            labelshows.append(labelshow);
                        }
                    }
                } catch (NumberFormatException e) {
                    // 处理无效的整数字符串
                }
            }
            sceneDetailVo1.setSceneSort(labelshows.toString());
        }
        return getDataTable(list);
    }

    @PostMapping("/tagtoscene")
    public TableDataInfo test(@RequestBody TagtoSceneVo tagtoSceneVo){
        startPage();
        if(tagtoSceneVo.getChoice().equals(0)) {
            List<SceneDetailVo> res = tjFragmentedSceneDetailService.selectTjSceneDetailListOr(tagtoSceneVo.getLabellist(),tagtoSceneVo.getFragmentedSceneId());
            for(SceneDetailVo sceneDetailVo1 : res){
                String labels = sceneDetailVo1.getLabel();
                if(labels==null){
                    continue;
                }
                StringBuilder labelshows = new StringBuilder();
                for (String str : labels.split(",")) {
                    try {
                        long intValue = Long.parseLong(str);
                        String labelshow = sceneLabelMap.getSceneLabel(intValue);
                        if(labelshow!=null) {
                            if(labelshows.length()>0) {
                                labelshows.append(",").append(labelshow);
                            }else {
                                labelshows.append(labelshow);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 处理无效的整数字符串
                    }
                }
                sceneDetailVo1.setSceneSort(labelshows.toString());
            }
            return getDataTable(res);
        }else {
            List<SceneDetailVo> res = tjFragmentedSceneDetailService.selectTjSceneDetailListAnd(tagtoSceneVo.getLabellist(),tagtoSceneVo.getFragmentedSceneId());
            for(SceneDetailVo sceneDetailVo1 : res){
                String labels = sceneDetailVo1.getLabel();
                if(labels==null){
                    continue;
                }
                StringBuilder labelshows = new StringBuilder();
                for (String str : labels.split(",")) {
                    try {
                        long intValue = Long.parseLong(str);
                        String labelshow = sceneLabelMap.getSceneLabel(intValue);
                        if(labelshow!=null) {
                            if(labelshows.length()>0) {
                                labelshows.append(",").append(labelshow);
                            }else {
                                labelshows.append(labelshow);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 处理无效的整数字符串
                    }
                }
                sceneDetailVo1.setSceneSort(labelshows.toString());
            }
            return getDataTable(res);
        }
    }

    @ApiOperationSort(1)
    @ApiOperation(value = "轨迹优化")
    @PostMapping("/optimize")
    public AjaxResult batchUpdateStatus(@RequestBody SceneDebugDto debugDto)
            throws BusinessException {
        return AjaxResult.success(debugDto);
    }

}
