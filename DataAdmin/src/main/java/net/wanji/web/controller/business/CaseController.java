package net.wanji.web.controller.business;

import net.wanji.business.common.Constants.BatchGroup;
import net.wanji.business.common.Constants.CaseStatusEnum;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.DeleteGroup;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.PlaybackAction;
import net.wanji.business.common.Constants.QueryGroup;
import net.wanji.business.common.Constants.UpdateGroup;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjFragmentedScenesService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.poi.ExcelUtil;
import net.wanji.quartz.domain.SysJobLog;
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
import java.util.Arrays;
import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 13:23
 * @Descriptoin:
 */
@RestController
@RequestMapping("/case")
public class CaseController extends BaseController {

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private TjFragmentedScenesService scenesService;

    @PreAuthorize("@ss.hasPermi('case:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(caseService.init());
    }

    @PreAuthorize("@ss.hasPermi('case:selectTree')")
    @GetMapping("/selectTree")
    public AjaxResult selectTree(@RequestParam(value = "testType") String testType,
                                 @RequestParam(value = "type") String type,
                                 @RequestParam(value = "name", required = false) String name) {
        List<TjFragmentedScenes> scenes = caseService.selectScenesInCase(testType, type, name);
        List<BusinessTreeSelect> tree = scenesService.buildSceneTreeSelect(scenes);
        return AjaxResult.success(tree);
    }

    @PreAuthorize("@ss.hasPermi('case:createCase')")
    @PostMapping("/createCase")
    public AjaxResult createCase(@Validated(value = InsertGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        return caseService.createCase(tjCaseDto)
                ? AjaxResult.success("创建成功")
                : AjaxResult.error("创建失败");
    }

    @PreAuthorize("@ss.hasPermi('case:getSceneBaseInfo')")
    @GetMapping("/getSceneBaseInfo")
    public AjaxResult getSceneBaseInfo(@RequestParam("fragmentedSceneId") Integer fragmentedSceneId)
            throws BusinessException {
        return AjaxResult.success(caseService.getSceneBaseInfo(fragmentedSceneId));
    }

    @PreAuthorize("@ss.hasPermi('case:pageForCase')")
    @PostMapping("/pageForCase")
    public TableDataInfo pageForCase(@Validated(value = QueryGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        startPage();
        return getDataTable(caseService.getCases(tjCaseDto));
    }

    @PreAuthorize("@ss.hasPermi('case:saveDetail')")
    @PostMapping("/saveDetail")
    public AjaxResult saveDetail(@Validated(value = UpdateGroup.class) @RequestBody TjCaseDto tjCaseDto)
            throws BusinessException, IOException {
        caseService.saveDetail(tjCaseDto);
        return AjaxResult.success("上传成功");
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

    @PreAuthorize("@ss.hasPermi('case:joinTask')")
    @PostMapping("/joinTask")
    public AjaxResult joinTask(@Validated(value = BatchGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        return caseService.joinTask(tjCaseDto.getIds())
                ? AjaxResult.success("加入成功")
                : AjaxResult.error("加入失败");
    }

    @PreAuthorize("@ss.hasPermi('case:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response,
                             @Validated(value = BatchGroup.class) @RequestBody TjCaseDto tjCaseDto) {
        TjCase tjCase = caseService.getById(tjCaseDto.getIds().get(0));
        TjFragmentedScenes scenes = scenesService.getById(tjCase.getFragmentedSceneId());
        String fileName = StringUtils.format(ContentTemplate.EXPORT_NAME_TEMPLATE, scenes.getName(),
                CaseStatusEnum.getDescByCode(tjCase.getStatus()), DateUtils.getNowSecondString());
        List<TjCase> cases = caseService.listByIds(tjCaseDto.getIds());
        ExcelUtil<TjCase> util = new ExcelUtil<TjCase>(TjCase.class);
        util.exportExcel(response, cases, fileName);
    }

    @PreAuthorize("@ss.hasPermi('case:verifyTrajectory')")
    @GetMapping("/verifyTrajectory")
    public AjaxResult verifyTrajectory(@RequestParam("id") Integer id) throws IOException {
        return caseService.verifyTrajectory(id) ? AjaxResult.success("校验完成") : AjaxResult.error("校验失败");
    }

    @PreAuthorize("@ss.hasPermi('case:detail')")
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam("id") Integer id) throws BusinessException {
        return AjaxResult.success(caseService.getDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('case:playback')")
    @GetMapping("/playback")
    public AjaxResult detail(@RequestParam(value = "id") Integer id,
                             @RequestParam(value = "action") int action,
                             @RequestParam(value = "vehicleId", required = false) String vehicleId)
            throws BusinessException, IOException {
        if (!Arrays.asList(PlaybackAction.START, PlaybackAction.SUSPEND, PlaybackAction.CONTINUE, PlaybackAction.STOP)
                .contains(action)) {
            throw new BusinessException("请选择正确操作");
        }
        caseService.playback(id, vehicleId, action);
        return AjaxResult.success();
    }
}
