package net.wanji.web.controller.business;

import net.wanji.business.common.Constants.DeleteGroup;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.StatusGroup;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.dto.TjResourcesDto;
import net.wanji.business.entity.TjResources;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjResourcesDetailService;
import net.wanji.business.service.TjResourcesService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/resource")
public class ResourcesController extends BaseController {

    @Autowired
    private TjResourcesService tjResourcesService;

    @Autowired
    private TjResourcesDetailService tjResourcesDetailService;

    @PreAuthorize("@ss.hasPermi('resource:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(tjResourcesService.init());
    }

    @PreAuthorize("@ss.hasPermi('resource:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam("type") String type,
                                 @RequestParam(value = "name", required = false) String name) {
        List<TjResources> usingResources = tjResourcesService.selectUsingResources(type);
        List<BusinessTreeSelect> tree = tjResourcesService.buildResourcesTreeSelect(usingResources, name);
        return AjaxResult.success(tree);
    }

    @PreAuthorize("@ss.hasPermi('resource:deleteTree')")
    @GetMapping("/deleteTree")
    public AjaxResult deleteTree(@RequestParam("resourceId") Integer resourceId) throws BusinessException {
        return tjResourcesService.deleteTree(resourceId)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:saveTree')")
    @PostMapping("/saveTree")
    public AjaxResult saveTree(@Validated(InsertGroup.class) @RequestBody TjResourcesDto resourcesDto) {
        return tjResourcesService.saveTree(resourcesDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('resources:getDetailList')")
    @GetMapping("/getDetailList")
    public AjaxResult getDetailList(@RequestParam("resourceId") Integer resourceId,
                                    @RequestParam(value = "name", required = false) String name) {
        return AjaxResult.success(tjResourcesDetailService.getDetailList(resourceId, name));
    }

    @PreAuthorize("@ss.hasPermi('resource:saveResourceDetail')")
    @PostMapping("/saveResourceDetail")
    public AjaxResult saveResourceDetail(@Validated @RequestBody TjResourcesDetailDto resourcesDetailDto)
            throws BusinessException{
        return tjResourcesDetailService.saveResourcesDetail(resourcesDetailDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:deleteResourceDetail')")
    @GetMapping("/deleteResourceDetail")
    public AjaxResult deleteResourceDetail(@RequestParam("id") Integer id)
            throws BusinessException{
        return tjResourcesDetailService.deleteByDetailId(id)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:collectResourceDetail')")
    @GetMapping("/collectResourceDetail")
    public AjaxResult collectResourceDetail(@RequestParam("id") Integer id) {
        return tjResourcesDetailService.collectByDetailId(id)
                ? AjaxResult.success("收藏成功")
                : AjaxResult.error("收藏失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:getResourceSelect')")
    @GetMapping("/getResourceSelect")
    public AjaxResult getResourceSelect(@RequestParam(value = "type", required = false) String type,
                                        @RequestParam(value = "sceneTreeType", required = false) String sceneTreeType,
                                        @RequestParam(value = "roadWayType", required = false) String roadWayType,
                                        @RequestParam(value = "laneNum", required = false) Integer laneNum) {
        return AjaxResult.success(tjResourcesDetailService.getMapSelect(type, sceneTreeType, roadWayType, laneNum));
    }

}
