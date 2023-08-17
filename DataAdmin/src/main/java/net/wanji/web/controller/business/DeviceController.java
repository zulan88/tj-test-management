package net.wanji.web.controller.business;

import com.github.pagehelper.PageHelper;
import net.wanji.business.common.Constants.BatchGroup;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.DeleteGroup;
import net.wanji.business.common.Constants.InsertGroup;
import net.wanji.business.common.Constants.QueryGroup;
import net.wanji.business.common.Constants.UpdateGroup;
import net.wanji.business.domain.BusinessTreeSelect;
import net.wanji.business.domain.dto.SceneQueryDto;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.dto.TjDeviceDto;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedScenes;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjDeviceService;
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

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 13:23
 * @Descriptoin:
 */
@RestController
@RequestMapping("/devices")
public class DeviceController extends BaseController {

    @Autowired
    private TjDeviceService deviceService;

    @PreAuthorize("@ss.hasPermi('devices:init')")
    @GetMapping("/init")
    public AjaxResult init() {
        return AjaxResult.success(deviceService.init());
    }

    @PreAuthorize("@ss.hasPermi('devices:pageForDevice')")
    @PostMapping("/pageForDevice")
    public TableDataInfo pageForDevice(@Validated(value = QueryGroup.class) @RequestBody TjDeviceDto deviceDto) {
        PageHelper.startPage(deviceDto.getPageNum(), deviceDto.getPageSize());
        return getDataTable(deviceService.getAllDevices(deviceDto));
    }

    @PreAuthorize("@ss.hasPermi('case:save')")
    @PostMapping("/save")
    public AjaxResult save(@Validated(value = {InsertGroup.class, UpdateGroup.class})
                               @RequestBody TjDeviceDto deviceDto) {
        return deviceService.saveDevice(deviceDto) ? AjaxResult.success("保存成功") : AjaxResult.error("保存失败");
    }

    @PreAuthorize("@ss.hasPermi('case:detail')")
    @PostMapping("/detail")
    public AjaxResult detail(@Validated(value = DeleteGroup.class) @RequestBody TjDeviceDto deviceDto) {
        return AjaxResult.success(deviceService.getDeviceDetail(deviceDto));
    }

    @PreAuthorize("@ss.hasPermi('devices:delete')")
    @PostMapping("/delete")
    public AjaxResult delete(@Validated(value = DeleteGroup.class) @RequestBody TjDeviceDto deviceDto) {
        return deviceService.deleteDevice(deviceDto.getDeviceId())
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('devices:batchDelete')")
    @PostMapping("/batchDelete")
    public AjaxResult batchDelete(@Validated(value = BatchGroup.class) @RequestBody TjDeviceDto deviceDto) {
        return deviceService.batchDeleteDevice(deviceDto.getDeviceIds())
                ? AjaxResult.success("删除成功")
                : AjaxResult.error("删除失败");
    }
}
