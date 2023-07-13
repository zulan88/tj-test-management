package net.wanji.web.controller.business;

import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.TjResourcesDetailDto;
import net.wanji.business.domain.dto.TjResourcesDto;
import net.wanji.business.domain.vo.ResourcesDetailVo;
import net.wanji.business.entity.TjResources;
import net.wanji.business.service.TjResourcesDetailService;
import net.wanji.business.service.TjResourcesService;
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
@RequestMapping("/resource")
public class ResourcesController extends BaseController {

    @Autowired
    private TjResourcesService tjResourcesService;

    @Autowired
    private TjResourcesDetailService tjResourcesDetailService;

    @PreAuthorize("@ss.hasPermi('resource:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam("type") String type) {
        List<TjResources> usingResources = tjResourcesService.selectUsingResources(type);
        List<BusinessTreeSelect> tree = tjResourcesService.buildResourcesTreeSelect(usingResources);
        return AjaxResult.success(tree);
    }

    @PreAuthorize("@ss.hasPermi('resources:getDetailVo')")
    @GetMapping("/getDetailVo/{resourcesId}")
    public AjaxResult getDetailVo(@PathVariable("resourcesId") Integer resourcesId) {
        ResourcesDetailVo detailVo = tjResourcesDetailService.getDetailVo(resourcesId);
        return AjaxResult.success(detailVo);
    }

    @PreAuthorize("@ss.hasPermi('resource:deleteResource')")
    @GetMapping("/deleteResource/{resourcesId}")
    public AjaxResult deleteResource(@PathVariable("resourcesId") Integer resourcesId) {
        return tjResourcesService.deleteResourcesById(resourcesId)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:saveResourceTree')")
    @PostMapping("/saveResourceTree")
    public AjaxResult saveResourceTree(@Validated @RequestBody TjResourcesDto resourcesDto) {
        return tjResourcesService.saveResource(resourcesDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:saveResourceDetail')")
    @PostMapping("/saveResourceDetail")
    public AjaxResult saveResourceDetail(@Validated @RequestBody TjResourcesDetailDto resourcesDetailDto) {
        return tjResourcesDetailService.saveResourcesDetail(resourcesDetailDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

    @PreAuthorize("@ss.hasPermi('resource:getResourceSelect')")
    @GetMapping("/getResourceSelect")
    public AjaxResult getResourceSelect(@RequestParam("type") String type) {
        return AjaxResult.success(tjResourcesDetailService.getResourceSelect(type));
    }
}
