package net.wanji.web.controller.business;

import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import net.wanji.business.common.Constants.BatchGroup;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.DeleteGroup;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.QueryGroup;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.CaseTreeDto;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseTree;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjCaseTreeService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.poi.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 13:23
 * @Descriptoin:
 */
@Api("测试用例服务")
@RestController
@RequestMapping("/case")
public class CaseController extends BaseController {


    @Autowired
    private TjCaseTreeService caseTreeService;

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private TjFragmentedScenesService scenesService;

    @Autowired
    private TjFragmentedSceneDetailService sceneDetailService;

    @Autowired
    private TjCasePartConfigService casePartConfigService;

    @ApiOperation("查询测试用例树")
    @PreAuthorize("@ss.hasPermi('case:selectTree')")
    @GetMapping("/selectTree")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "name", value = "名称", dataType = "String", paramType = "query")
    })
    public AjaxResult selectTree(String type, String name) {
        return AjaxResult.success(caseTreeService.selectTree(type, name));
    }

    @ApiOperation("修改测试用例树")
    @PreAuthorize("@ss.hasPermi('case:saveTree')")
    @PostMapping("/saveTree")
    public AjaxResult saveTree(@RequestBody CaseTreeDto caseTreeDto) {
        return AjaxResult.success(caseTreeService.saveTree(caseTreeDto));
    }

    @ApiOperation("测试用例列表页初始化")
    @PreAuthorize("@ss.hasPermi('case:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(caseService.init());
    }

    @PreAuthorize("@ss.hasPermi('case:initEditPage')")
    @GetMapping("/initEditPage")
    public AjaxResult initEditPage(@RequestParam("sceneDetailId") Integer sceneDetailId,
                                   @RequestParam(value = "caseId", required = false) Integer caseId
    ) throws BusinessException {
        return AjaxResult.success(caseService.initEditPage(sceneDetailId, caseId));
    }

    @PreAuthorize("@ss.hasPermi('case:pageForCase')")
    @PostMapping("/pageForCase")
    public TableDataInfo pageForCase(@RequestBody CaseQueryDto caseQueryDto) {
        PageHelper.startPage(caseQueryDto.getPageNum(), caseQueryDto.getPageSize());
        return getDataTable(caseService.getCases(caseQueryDto));
    }

    @PreAuthorize("@ss.hasPermi('case:getSubscenesList')")
    @PostMapping("/getSubscenesList")
    public AjaxResult getSubscenesList(@RequestBody SceneQueryDto sceneQueryDto)
            throws BusinessException {
        return AjaxResult.success(sceneDetailService.selectScene(sceneQueryDto));
    }



    @PreAuthorize("@ss.hasPermi('case:saveCase')")
    @PostMapping("/saveCase")
    public AjaxResult createCase(@Validated(value = InsertGroup.class) @RequestBody TjCaseDto tjCaseDto)
            throws BusinessException {
        return AjaxResult.success(caseService.saveCase(tjCaseDto));
    }

    @PreAuthorize("@ss.hasPermi('case:saveCaseDevice')")
    @PostMapping("/saveCaseDevice")
    public AjaxResult saveCaseDevice(@RequestBody List<PartConfigSelect> partConfigSelects)
            throws BusinessException {
        return AjaxResult.success(casePartConfigService.saveFromSelected(partConfigSelects));
    }

    @PreAuthorize("@ss.hasPermi('case:configDetail')")
    @GetMapping("/configDetail")
    public AjaxResult configDetail(@RequestParam("id") Integer id) throws BusinessException, InterruptedException,
            ExecutionException {
        return AjaxResult.success(caseService.getConfigDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('case:detail')")
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam("id") Integer id) throws BusinessException {
        return AjaxResult.success(caseService.getSimulationDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('case:cloneCase')")
    @PostMapping("/cloneCase")
    public AjaxResult cloneCase(@Validated(value = DeleteGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        return caseService.cloneCase(tjCaseDto)
                ? AjaxResult.success("克隆成功")
                : AjaxResult.error("克隆失败");
    }

    @PreAuthorize("@ss.hasPermi('case:delete')")
    @PostMapping("/delete")
    public AjaxResult delete(@Validated(value = DeleteGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        return caseService.deleteCase(tjCaseDto)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }


    @PreAuthorize("@ss.hasPermi('case:batchDelete')")
    @PostMapping("/batchDelete")
    public AjaxResult batchDelete(@Validated(value = BatchGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        return caseService.deleteCase(tjCaseDto)
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('case:updateStatus')")
    @PostMapping("/updateStatus")
    public AjaxResult updateStatus(@Validated(value = BatchGroup.class) @RequestBody TjCaseDto tjCaseDto)
            throws BusinessException {
        return caseService.updateStatus(tjCaseDto)
                ? AjaxResult.success("成功")
                : AjaxResult.error("失败");
    }

//    @PreAuthorize("@ss.hasPermi('case:export')")
//    @PostMapping("/export")
//    public void export(HttpServletResponse response,
//                       @Validated(value = BatchGroup.class) @RequestBody TjCaseDto tjCaseDto) {
//        List<CaseVo> caseVos = caseService.getCases(tjCaseDto);
//        TjCase tjCase = caseService.getById(tjCaseDto.getIds().get(0));
//        TjFragmentedScenes scenes = scenesService.getById(tjCase.getSceneDetailId());
//        String fileName = StringUtils.format(ContentTemplate.EXPORT_NAME_TEMPLATE, scenes.getName(),
//                DateUtils.getNowSecondString());
//        ExcelUtil<CaseVo> util = new ExcelUtil<CaseVo>(CaseVo.class);
//        util.exportExcel(response, caseVos, fileName);
//    }

    @PreAuthorize("@ss.hasPermi('case:playback')")
    @GetMapping("/playback")
    public AjaxResult playback(@RequestParam(value = "id") Integer id,
                             @RequestParam(value = "action") int action,
                             @RequestParam(value = "vehicleId", required = false) String vehicleId)
            throws BusinessException, IOException {
        caseService.playback(id, vehicleId, action);
        return AjaxResult.success();
    }
}
