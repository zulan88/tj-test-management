package net.wanji.web.controller.business;

import com.alibaba.fastjson2.JSON;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.SceneTreeTypeDto;
import net.wanji.business.domain.dto.TjFragmentedSceneDetailDto;
import net.wanji.business.domain.dto.TjFragmentedScenesDto;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/27 16:25
 * @Descriptoin: 场景库控制器
 */
@RestController
@RequestMapping("/sceneBase")
public class SceneBaseController extends BaseController {

    @Autowired
    private TjFragmentedScenesService tjFragmentedScenesService;

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;


    @PreAuthorize("@ss.hasPermi('sceneBase:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(JSON.toJSON(tjFragmentedScenesService.init()));
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:initEditPage')")
    @GetMapping("/initEditPage")
    public AjaxResult initEditPage() {
        return AjaxResult.success(tjFragmentedScenesService.initEditPage());
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:saveTreeType')")
    @PostMapping("/saveTreeType")
    public AjaxResult saveTreeType(@Validated @RequestBody SceneTreeTypeDto treeTypeDto) throws BusinessException {
        return tjFragmentedScenesService.saveSceneTreeType(treeTypeDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:deleteTreeType')")
    @GetMapping("/deleteTreeType/{dictCode}")
    public AjaxResult deleteTreeType(@PathVariable("dictCode") Long dictCode) throws BusinessException {
        return tjFragmentedScenesService.deleteTreeType(dictCode)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam(value = "type") String type,
                                 @RequestParam(value = "name", required = false) String name) {
        List<TjFragmentedScenes> usingScenes = tjFragmentedScenesService.selectUsingScenes(type);
        List<BusinessTreeSelect> tree = tjFragmentedScenesService.buildSceneTreeSelect(usingScenes, name);
        return AjaxResult.success(tree);
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:saveSceneTree')")
    @PostMapping("/saveSceneTree")
    public AjaxResult saveSceneTree(@Validated @RequestBody TjFragmentedScenesDto fragmentedScenesDto) {
        return tjFragmentedScenesService.saveSceneTree(fragmentedScenesDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:deleteScene')")
    @GetMapping("/deleteScene/{sceneId}")
    public AjaxResult deleteScene(@PathVariable("sceneId") Integer sceneId) throws BusinessException {
        return tjFragmentedScenesService.deleteSceneById(sceneId)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:cloneScene')")
    @GetMapping("/cloneScene/{sceneId}")
    public AjaxResult cloneScene(@PathVariable("sceneId") Integer sceneId) throws BusinessException {
        return AjaxResult.success(tjFragmentedScenesService.cloneScene(sceneId));
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:getDetailVo')")
    @GetMapping("/getDetailVo/{sceneId}")
    public AjaxResult getDetailVo(@PathVariable("sceneId") Integer sceneId) throws BusinessException {
        FragmentedScenesDetailVo detailVo = tjFragmentedSceneDetailService.getDetailVo(sceneId);
        return AjaxResult.success(detailVo);
    }

    @PreAuthorize("@ss.hasPermi('sceneBase:saveSceneDetail')")
    @PostMapping("/saveSceneDetail")
    public AjaxResult saveSceneDetail(@Validated @RequestBody TjFragmentedSceneDetailDto sceneDetailDto)
            throws BusinessException {
        return tjFragmentedSceneDetailService.saveSceneDetail(sceneDetailDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }
}
