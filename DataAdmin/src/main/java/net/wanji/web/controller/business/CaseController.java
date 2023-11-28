package net.wanji.web.controller.business;

import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiOperationSort;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.UpdateGroup;
import net.wanji.business.domain.PartConfigSelect;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.CaseTreeDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjCasePartConfigService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjCaseTreeService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.SecurityUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 13:23
 * @Descriptoin:
 */
@Api(tags = "特色测试服务-测试配置")
@RestController
@RequestMapping("/case")
public class CaseController extends BaseController {

    @Autowired
    private TjCaseTreeService caseTreeService;

    @Autowired
    private TjCaseService caseService;

    @Autowired
    private TjCasePartConfigService casePartConfigService;

    @Autowired
    private TjFragmentedSceneDetailService sceneDetailService;

    @ApiOperationSort(1)
    @ApiOperation(value = "1.查询测试用例树")
    @GetMapping("/selectTree")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", required = true, dataType = "String", paramType = "query", example = "virtualRealFusion"),
            @ApiImplicitParam(name = "name", value = "名称", dataType = "String", paramType = "query", example = "场景")
    })
    public AjaxResult selectTree(String type, String name) {
        return AjaxResult.success(caseTreeService.selectTree(type, name));
    }

    @ApiOperationSort(2)
    @ApiOperation(value = "2.保存测试用例树")
    @PostMapping("/saveTree")
    public AjaxResult saveTree(@Validated @RequestBody CaseTreeDto caseTreeDto) throws BusinessException {
        return AjaxResult.success(caseTreeService.saveTree(caseTreeDto));
    }

    @ApiOperationSort(3)
    @ApiOperation(value = "3.删除测试用例树")
    @GetMapping("/deleteTree")
    @ApiImplicitParam(name = "treeId", value = "树节点ID", required = true, dataType = "Integer", paramType = "query", example = "28")
    public AjaxResult deleteTree(Integer treeId) {
        return AjaxResult.success(caseTreeService.deleteTree(treeId));
    }

    @ApiOperationSort(4)
    @ApiOperation(value = "4.测试用例列表页初始化")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(caseService.init());
    }

    @ApiOperationSort(5)
    @ApiOperation(value = "5.测试用例列表页查询")
    @PostMapping("/pageForCase")
    public TableDataInfo pageForCase(@Validated @RequestBody CaseQueryDto caseQueryDto) {
        if (CollectionUtils.isNotEmpty(caseQueryDto.getLabelList())) {
            List<SceneDetailVo> sceneDetails = null;
            if (ObjectUtils.isEmpty(caseQueryDto) || 0 == caseQueryDto.getChoice()) {
                sceneDetails = sceneDetailService.selectTjSceneDetailListOr(caseQueryDto.getLabelList(), null);
            } else {
                sceneDetails = sceneDetailService.selectTjSceneDetailListAnd(caseQueryDto.getLabelList(), null);
            }
            List<Integer> sceneDetailIds = CollectionUtils.emptyIfNull(sceneDetails).stream().map(SceneDetailVo::getId)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(sceneDetailIds)) {
                return getDataTable(sceneDetailIds);
            }
            caseQueryDto.setSceneDetailIds(sceneDetailIds);
        }
        PageHelper.startPage(caseQueryDto.getPageNum(), caseQueryDto.getPageSize());
        return getDataTable(caseService.pageList(caseQueryDto));
    }

    @ApiOperationSort(6)
    @ApiOperation(value = "6.查询用例详情")
    @GetMapping("/selectDetail")
    @ApiImplicitParam(name = "caseId", value = "用例id", required = true, dataType = "Integer", paramType = "query", example = "276")
    public AjaxResult selectDetail(Integer caseId) {
        return AjaxResult.success(caseService.selectCaseDetail(caseId));
    }

    @ApiOperationSort(7)
    @ApiOperation(value = "7.创建用例")
    @PostMapping("/createCase")
    public AjaxResult createCase(@Validated(value = InsertGroup.class) @RequestBody TjCaseDto tjCaseDto)
            throws BusinessException {
        return AjaxResult.success(caseService.saveCase(tjCaseDto) ? "创建成功" : "创建失败");
    }

    @ApiOperationSort(8)
    @ApiOperation(value = "8.修改用例")
    @PostMapping("/updateCase")
    public AjaxResult updateCase(@Validated(value = UpdateGroup.class) @RequestBody TjCaseDto tjCaseDto)
            throws BusinessException {
        return AjaxResult.success(caseService.saveCase(tjCaseDto) ? "修改成功" : "修改失败");
    }

    @ApiOperationSort(9)
    @ApiOperation(value = "9.启停")
    @GetMapping("/updateStatus")
    @ApiImplicitParam(name = "caseId", value = "用例id", required = true, dataType = "Integer", paramType = "query", example = "276")
    public AjaxResult updateStatus(Integer caseId)
            throws BusinessException {
        return AjaxResult.success(caseService.updateStatus(caseId) ? "成功" : "失败");
    }

    @ApiOperationSort(10)
    @ApiOperation(value = "10.批量启停")
    @GetMapping("/batchUpdateStatus")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "caseIds", value = "用例id", required = true, dataType = "List", paramType = "query", example = "[1,2,3]"),
            @ApiImplicitParam(name = "action", value = "动作（1：启用；2：停用）", required = true, dataType = "Integer", paramType = "query", example = "1")
    })
    public AjaxResult batchUpdateStatus(@RequestParam("caseIds") List<Integer> caseIds, @RequestParam("action") Integer action)
            throws BusinessException {
        return AjaxResult.success(caseService.batchUpdateStatus(caseIds, action) ? "成功" : "失败");
    }

    @ApiOperationSort(11)
    @ApiOperation(value = "11.删除")
    @GetMapping("/delete")
    @ApiImplicitParam(name = "caseId", value = "用例id", required = true, dataType = "Integer", paramType = "query", example = "276")
    public AjaxResult delete(Integer caseId)
            throws BusinessException {
        return caseService.batchDelete(Collections.singletonList(caseId))
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @ApiOperationSort(12)
    @ApiOperation(value = "12.批量删除")
    @GetMapping("/batchDelete")
    @ApiImplicitParam(name = "caseIds", value = "用例id", required = true, dataType = "List", paramType = "query", example = "1,2,3")
    public AjaxResult batchDelete(@RequestParam("caseIds") List<Integer> caseIds)
            throws BusinessException {
        return AjaxResult.success(caseService.batchDelete(caseIds) ? "成功" : "失败");
    }

    @ApiOperationSort(13)
    @ApiOperation(value = "13.查询用例设备配置")
    @GetMapping("/configDetail")
    @ApiImplicitParam(name = "id", value = "用例id", required = true, dataType = "Integer", paramType = "query", example = "276")
    public AjaxResult configDetail(@RequestParam("id") Integer id) throws BusinessException, InterruptedException,
            ExecutionException {
        return AjaxResult.success(caseService.getConfigDetail(id));
    }

    @ApiOperationSort(14)
    @ApiOperation(value = "14.保存用例设备配置")
    @PostMapping("/saveCaseDevice")
    public AjaxResult saveCaseDevice(@RequestBody List<PartConfigSelect> partConfigSelects) {
        return AjaxResult.success(casePartConfigService.saveFromSelected(partConfigSelects));
    }

    @ApiOperationSort(15)
    @ApiOperation(value = "15.编辑页初始化")
    @GetMapping("/initEdit")
    @ApiImplicitParam(name = "caseId", value = "用例ID", required = true, dataType = "Integer", paramType = "query", example = "280")
    public AjaxResult initEdit(Integer caseId) {
        return AjaxResult.success(caseService.initEdit(caseId));
    }

    @ApiOperationSort(16)
    @ApiOperation(value = "预览")
    @GetMapping("/preview")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用例ID", required = true, dataType = "Integer", paramType = "query", example = "280"),
            @ApiImplicitParam(name = "action", value = "动作（1:开始; 2:暂停; 3:继续; 4:结束）", required = true, dataType = "Integer", paramType = "query", example = "1"),
            @ApiImplicitParam(name = "vehicleId", value = "参与者ID", required = false, dataType = "Integer", paramType = "query", example = "1")
    })
    public AjaxResult preview(@RequestParam(value = "id") Integer id,
                              @RequestParam(value = "action") int action,
                              @RequestParam(value = "vehicleId", required = false) String vehicleId)
            throws BusinessException, IOException {
        caseService.playback(id, vehicleId, action);
        return AjaxResult.success();
    }

    @ApiOperationSort(7)
    @ApiOperation(value = "删除任务记录")
    @GetMapping("/deleteRecord")
    @ApiImplicitParam(name = "recordId", value = "测试记录ID", required = true, dataType = "Integer", paramType = "query", example = "499")
    public AjaxResult deleteRecord(Integer recordId) throws BusinessException {
        return  caseService.deleteRecord(recordId) ? AjaxResult.success("删除成功") : AjaxResult.error("删除失败");
    }
}
