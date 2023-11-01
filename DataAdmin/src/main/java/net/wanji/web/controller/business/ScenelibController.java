package net.wanji.web.controller.business;

import com.alibaba.fastjson2.JSON;
import io.swagger.annotations.Api;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.domain.dto.TreeTypeDto;
import net.wanji.business.domain.vo.ScenelibVo;
import net.wanji.business.entity.TjScenelib;
import net.wanji.business.entity.TjScenelibTree;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.ITjScenelibService;
import net.wanji.business.service.TjScenelibTreeService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
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

import java.util.List;

@Api(tags = "场景库")
@RestController
@RequestMapping("/scenelib")
public class ScenelibController extends BaseController {

    @Autowired
    private SceneLabelMap sceneLabelMap;

    @Autowired
    ITjScenelibService scenelibService;

    @Autowired
    TjScenelibTreeService scenelibTreeService;

    @PostMapping("/list")
    public TableDataInfo scenelist(@RequestBody ScenelibVo scenelibVo) throws BusinessException {
        startPage();
        List<ScenelibVo> list = scenelibService.selectScenelibVoList(scenelibVo);
        for(ScenelibVo scenelibVo1 : list){
            String labels = scenelibVo1.getLabels();
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
            scenelibVo1.setSceneSort(labelshows.toString());
        }
        return getDataTable(list);
    }

    @PostMapping("/libadd")
    public AjaxResult add(TjScenelib tjScenelib) throws BusinessException{
        return toAjax(scenelibService.insertTjScenelib(tjScenelib));
    }

    @PutMapping("/libedit")
    public AjaxResult edit(TjScenelib tjScenelib) throws BusinessException{
        return toAjax(scenelibService.updateTjScenelib(tjScenelib));
    }


    @PutMapping("/libstatus")
    public AjaxResult updatestatus(@RequestBody List<TjScenelib> scenelibs){
        return toAjax(scenelibService.updateBatch(scenelibs));
    }

    @DeleteMapping("/lib/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) throws BusinessException {
        return toAjax(scenelibService.deleteTjScenelibByIds(ids));
    }

    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(JSON.toJSON(scenelibTreeService.init()));
    }

    @GetMapping("/initEditPage")
    public AjaxResult initEditPage() {
        return AjaxResult.success(scenelibTreeService.initEditPage());
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:saveTreeType')")
    @PostMapping("/saveTreeType")
    public AjaxResult saveTreeType(@Validated @RequestBody TreeTypeDto treeTypeDto) throws BusinessException {
        return scenelibTreeService.saveSceneTreeType(treeTypeDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:deleteTreeType')")
    @GetMapping("/deleteTreeType/{dictCode}")
    public AjaxResult deleteTreeType(@PathVariable("dictCode") Long dictCode) throws BusinessException {
        return scenelibTreeService.deleteTreeType(dictCode)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam(value = "type") String type,
                                 @RequestParam(value = "name", required = false) String name) {
        List<TjScenelibTree> usingScenes = scenelibTreeService.selectUsingScenes(type);
        List<BusinessTreeSelect> tree = scenelibTreeService.buildSceneTreeSelect(usingScenes, name);
        return AjaxResult.success(tree);
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:saveSceneTree')")
    @PostMapping("/saveSceneTree")
    public AjaxResult saveSceneTree(@Validated @RequestBody TjFragmentedScenesDto fragmentedScenesDto) {
        return scenelibTreeService.saveSceneTree(fragmentedScenesDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    //@PreAuthorize("@ss.hasPermi('sceneBase:deleteScene')")
    @GetMapping("/deleteScene/{sceneId}")
    public AjaxResult deleteScene(@PathVariable("sceneId") Integer sceneId) throws BusinessException {
        return scenelibTreeService.deleteSceneById(sceneId)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

}
