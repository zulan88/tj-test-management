package net.wanji.business.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TaskProcessNode;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.vo.CaseDetailVo;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.domain.vo.DeviceDetailVo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.domain.vo.TaskVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjDeviceDetailMapper;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskDataConfigMapper;
import net.wanji.business.mapper.TjTaskDcMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskDataConfigService;
import net.wanji.business.service.TjTaskService;
import net.wanji.business.util.CustomMergeStrategy;
import net.wanji.common.constant.HttpStatus;
import net.wanji.common.core.domain.SimpleSelect;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictTypeService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author guowenhao
 * @description 针对表【tj_task(测试任务表)】的数据库操作Service实现
 * @createDate 2023-08-31 17:39:16
 */
@Service
public class TjTaskServiceImpl extends ServiceImpl<TjTaskMapper, TjTask>
        implements TjTaskService {

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private TjCaseService tjCaseService;

    @Autowired
    private TjTaskDataConfigService tjTaskDataConfigService;

    @Autowired
    private TjTaskCaseService tjTaskCaseService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjTaskMapper tjTaskMapper;

    @Autowired
    private TjTaskCaseMapper tjTaskCaseMapper;

    @Autowired
    private TjTaskDataConfigMapper tjTaskDataConfigMapper;

    @Autowired
    private TjTaskDcMapper tjTaskDcMapper;


    @Autowired
    private TjDeviceDetailMapper deviceDetailMapper;

    @Autowired
    private SceneLabelMap sceneLabelMap;

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    @Override
    public Object initProcessed(Integer processNode) {
        Map<String, Object> result = new HashMap<>();
        switch (processNode) {
            case TaskProcessNode.TASK_INFO:
                List<SysDictData> testType = dictTypeService.selectDictDataByType(SysType.TEST_TYPE);
                result.put(SysType.TEST_TYPE, CollectionUtils.emptyIfNull(testType).stream()
                        .map(SimpleSelect::new).collect(Collectors.toList()));
                break;
            case TaskProcessNode.SELECT_CASE:
                List<SysDictData> caseStatus = dictTypeService.selectDictDataByType(SysType.CASE_STATUS);
                result.put(SysType.CASE_STATUS, CollectionUtils.emptyIfNull(caseStatus).stream()
                        .map(SimpleSelect::new).collect(Collectors.toList()));
                break;
            case TaskProcessNode.CONFIG:

                break;
            case TaskProcessNode.VIEW_PLAN:

                break;
            default:
                break;
        }

        return result;
    }

    @Override
    public Object processedInfo(TaskSaveDto taskSaveDto) throws BusinessException {
        // 1.查询主车可用设备
        Map<String, Object> result = new HashMap<>();
        switch (taskSaveDto.getProcessNode()) {
            case TaskProcessNode.TASK_INFO:
                TjDeviceDetailDto deviceDetailDto = new TjDeviceDetailDto();
                deviceDetailDto.setStatus(YN.Y_INT);
                deviceDetailDto.setSupportRoles(PartRole.AV);
                List<DeviceDetailVo> avDevices = deviceDetailMapper.selectByCondition(deviceDetailDto);
                TjTask task = new TjTask();
                if (ObjectUtil.isNotEmpty(taskSaveDto.getId())) {
                    task = this.getById(taskSaveDto.getId());
                    // 已存在的设备
                    TjTaskDataConfig dataConfig = new TjTaskDataConfig();
                    dataConfig.setTaskId(taskSaveDto.getId());
                    List<TjTaskDataConfig> dataConfigs = tjTaskDataConfigMapper.selectByCondition(dataConfig);
                    CollectionUtils.emptyIfNull(avDevices).forEach(t -> {
                        if (CollectionUtils.emptyIfNull(dataConfigs).stream().anyMatch(dc -> dc.getDeviceId().equals(t.getDeviceId()))) {
                            t.setSelected(Boolean.TRUE);
                        }
                    });
                }
                result.put("taskInfo", task);
                result.put("avDevices", avDevices);
                break;
            case TaskProcessNode.SELECT_CASE:
                if (ObjectUtil.isEmpty(taskSaveDto.getId())) {
                    throw new BusinessException("请确认任务信息是否已保存");
                }
                // 分页查询用例
                CaseQueryDto caseQueryDto = taskSaveDto.getCaseQueryDto();
                PageHelper.startPage(caseQueryDto.getPageNum(), caseQueryDto.getPageSize());
                List<CasePageVo> casePageVos = tjCaseService.pageList(caseQueryDto);
                List<Integer> selectedIds = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(caseQueryDto.getSelectedIds())) {
                    CollectionUtils.emptyIfNull(casePageVos).forEach(t -> t.setSelected(Boolean.TRUE));
                    selectedIds = caseQueryDto.getSelectedIds();
                } else {
                    // 已选择的用例
                    TjTaskCase taskCase = new TjTaskCase();
                    taskCase.setTaskId(taskSaveDto.getId());
                    List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.selectByCondition(taskCase);
                    if (CollectionUtils.isNotEmpty(taskCaseVos)) {
                        selectedIds = taskCaseVos.stream().map(TaskCaseVo::getCaseId).collect(Collectors.toList());
                        CollectionUtils.emptyIfNull(casePageVos).forEach(t -> {
                            if (CollectionUtils.emptyIfNull(taskCaseVos).stream().anyMatch(tc -> tc.getCaseId().equals(t.getId()))) {
                                t.setSelected(Boolean.TRUE);
                            }
                        });
                    }
                }
                // 列表数据+已选择的用例
                TableDataInfo rspData = new TableDataInfo();
                rspData.setCode(HttpStatus.SUCCESS);
                rspData.setData(casePageVos);
                rspData.setTotal(new PageInfo(casePageVos).getTotal());
                result.put("tableData", rspData);
                result.put("selectedIds", selectedIds);
                break;
            case TaskProcessNode.CONFIG:
                // 已选择的用例
                TjTaskCase taskCase = new TjTaskCase();
                taskCase.setTaskId(taskSaveDto.getId());
                List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.selectByCondition(taskCase);


                CaseQueryDto param = new CaseQueryDto();
                param.setSelectedIds(CollectionUtils.emptyIfNull(taskCaseVos).stream().map(TaskCaseVo::getCaseId).collect(Collectors.toList()));
                List<CaseDetailVo> caseVos = caseMapper.selectCases(param);
                List<CasePageVo> cases = CollectionUtils.emptyIfNull(caseVos).stream().map(t -> {
                    CasePageVo casePageVo = new CasePageVo();
                    BeanUtils.copyBeanProp(casePageVo, t);
                    // 场景分类
                    if (StringUtils.isNotEmpty(casePageVo.getLabel())) {
                        StringBuilder labelSort = new StringBuilder();
                        for (String str : casePageVo.getLabel().split(",")) {
                            try {
                                long intValue = Long.parseLong(str);
                                String labelName = sceneLabelMap.getSceneLabel(intValue);
                                if (StringUtils.isNotEmpty(labelName)) {
                                    if (labelSort.length() > 0) {
                                        labelSort.append(",").append(labelName);
                                    } else {
                                        labelSort.append(labelName);
                                    }
                                }
                            } catch (NumberFormatException e) {
                                // 处理无效的整数字符串
                            }
                        }
                        casePageVo.setSceneSort(labelSort.toString());
                    }
                    return casePageVo;
                }).collect(Collectors.toList());
                result.put("cases", cases);
                break;
            case TaskProcessNode.VIEW_PLAN:
                result.put("weight", null);
                result.put("norm", null);
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    public TableDataInfo pageList(TaskDto in) {
//        TableDataInfo tableDataInfo = new TableDataInfo();
//        QueryWrapper<TjTask> wrapper = new QueryWrapper<>();
//        if (StringUtils.isNotEmpty(in.getTaskName()))
//            wrapper.eq("task_name", in.getTaskName());
//        if (ObjectUtil.isNotEmpty(in.getStartTime()))
//            wrapper.gt("start_time", in.getStartTime());
//        if (ObjectUtil.isNotEmpty(in.getEndTime()))
//            wrapper.lt("end_time", in.getEndTime());
//        Page<TjTask> tjTaskPage = tjTaskMapper.selectPage(
//                new Page<>(in.getPageNum(), in.getPageSize()), wrapper);
//        tableDataInfo.setTotal(tjTaskPage.getTotal());
//        List<TaskListVo> taskListVos = new ArrayList<>();
//        for (TjTask record : tjTaskPage.getRecords()) {
//            TaskListVo taskListVo = new TaskListVo();
//            TjTaskDataConfig tjTaskDataConfig = tjTaskDataConfigMapper.selectOne(
//                    new QueryWrapper<TjTaskDataConfig>().eq("task_id",
//                            record.getId()).eq("type", "av"));
//            List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.gstList(record.getId());
//            int num = 0;
//            for (TaskCaseVo taskCaseVo : taskCaseVos) {
//                taskCaseVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
//                if (taskCaseVo.getStatus().equals("待测试"))
//                    num++;
//            }
//            BeanUtils.copyBeanProp(taskListVo, record);
//            if (num == 0)
//                num = taskCaseVos.size();
//            taskListVo.setStatus(num + "/" + taskCaseVos.size());
//            taskListVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
//            for (TaskCaseVo taskCaseVo : CollectionUtils.emptyIfNull(taskCaseVos)) {
//                if (StringUtils.isNotEmpty(taskCaseVo.getTestTotalTime())) {
//                    taskCaseVo.setTestTotalTime(DateUtils.secondsToDuration(Integer.parseInt(taskCaseVo.getTestTotalTime())));
//                }
//            }
//            taskListVo.setTaskCaseVos(taskCaseVos);
//            taskListVos.add(taskListVo);
//        }
//        tableDataInfo.setData(taskListVos);
        return null;
    }

    @Override
    public TaskVo createTask(List<Integer> caseIds)
            throws BusinessException, ExecutionException, InterruptedException {
        List<TjCase> cases = tjCaseService.listByIds(caseIds);
        if (CollectionUtils.isEmpty(cases) || cases.size() != caseIds.size()) {
            throw new BusinessException("创建任务失败：未查询到用例信息");
        }
        if (cases.stream().anyMatch(t -> StringUtils.isEmpty(t.getDetailInfo()) || StringUtils.isEmpty(t.getRouteFile()))) {
            throw new BusinessException("创建任务失败：用例缺失轨迹信息");
        }
        TaskVo taskVo = new TaskVo();
        taskVo.setCaseCount(caseIds.size());
        taskVo.setTaskName("task-" + sf.format(new Date()));
        taskVo.setCaseIds(StringUtils.join(caseIds, ","));
        taskVo.setDataConfigs(tjCaseService.getTaskConfigDetail(caseIds.get(0)));
        return taskVo;
    }

    @Override
    @Transactional
    public int saveTask(TaskBo in) throws BusinessException {
        switch (in.getProcessNode()) {
            case TaskProcessNode.TASK_INFO:
                if (CollectionUtils.isEmpty(in.getAvDeviceIds())) {
                    throw new BusinessException("请选择主车被测设备");
                }
                TjTask tjTask = new TjTask();
                if (ObjectUtil.isNotEmpty(in.getId())) {
                    BeanUtils.copyBeanProp(tjTask, in);
                    tjTaskMapper.updateById(tjTask);
                    tjTaskDataConfigMapper.delete(new QueryWrapper<TjTaskDataConfig>().eq(ColumnName.TASK_ID, tjTask.getId()));
                } else {
                    BeanUtils.copyBeanProp(tjTask, in);
                    tjTask.setCreateTime(new Date());
                    tjTaskMapper.insert(tjTask);
                }
                List<TjTaskDataConfig> dataConfigs = in.getAvDeviceIds().stream().map(deviceId -> {
                    TjTaskDataConfig config = new TjTaskDataConfig();
                    config.setTaskId(tjTask.getId());
                    config.setType(PartRole.AV);
                    config.setDeviceId(deviceId);
                    return config;
                }).collect(Collectors.toList());
                tjTaskDataConfigService.saveBatch(dataConfigs);
                return tjTask.getId();
            case TaskProcessNode.SELECT_CASE:
                if (ObjectUtil.isEmpty(in.getId())) {
                    throw new BusinessException("请选择任务");
                }
                if (CollectionUtils.isEmpty(in.getCaseIds())) {
                    throw new BusinessException("请选择用例");
                }
                List<TjTaskCase> taskCases = in.getCaseIds().stream().map(caseId -> {
                    TjTaskCase taskCase = new TjTaskCase();
                    taskCase.setTaskId(in.getId());
                    taskCase.setCaseId(caseId);
                    return taskCase;
                }).collect(Collectors.toList());
                tjTaskCaseService.saveBatch(taskCases);
                return in.getId();
            default:
                break;
        }
        return 0;
    }

    @Override
    public void export(HttpServletResponse response, Integer taskId) throws IOException {
        TjTask task = this.getById(taskId);
        List<TaskReportVo> exportList = tjTaskMapper.getExportList(taskId);
        System.out.println(JSONObject.toJSONString(exportList));
        double score = 100;
        DecimalFormat decimalFormat = new DecimalFormat("#.0");

        // 使用DecimalFormat格式化浮点数
        try {
            for (TaskReportVo taskReportVo : CollectionUtils.emptyIfNull(exportList)) {
                if (taskReportVo.getScore().contains("-")) {
                    try {
                        score = score - Double.parseDouble(taskReportVo.getScore());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            for (TaskReportVo taskReportVo : CollectionUtils.emptyIfNull(exportList)) {
                taskReportVo.setScoreTotal(decimalFormat.format(score));
            }
        } catch (Exception e) {
            for (TaskReportVo taskReportVo : CollectionUtils.emptyIfNull(exportList)) {
                taskReportVo.setScoreTotal("100");
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
//        response.setHeader("download-filename", FileUtils.percentEncode(task.getTaskName().concat(".xlsx")));


        //ExcelWriter该对象用于通过POI将值写入Excel
        String templatePath = "template" + File.separator + "TaskTemplate.xlsx";


        //导出模板
        CustomMergeStrategy customMergeStrategy = new CustomMergeStrategy(TaskReportVo.class, 5);
        WriteCellStyle style = new WriteCellStyle();
        style.setHorizontalAlignment(HorizontalAlignment.CENTER); // 设置水平居中
        style.setVerticalAlignment(VerticalAlignment.CENTER); // 设置垂直居中
        style.setBorderTop(BorderStyle.THIN); // 设置上边框样式为细线
        style.setBorderBottom(BorderStyle.THIN); // 设置下边框样式为细线
        style.setBorderLeft(BorderStyle.THIN); // 设置左边框样式为细线
        style.setBorderRight(BorderStyle.THIN); // 设置右边框样式为细线
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(response.getOutputStream()).withTemplate(new ClassPathResource(templatePath)
                            .getInputStream())
                    .registerWriteHandler(customMergeStrategy)
                    .registerWriteHandler(new HorizontalCellStyleStrategy(style, style))
                    .build();
        } catch (IOException e) {
            log.error("导出文件异常", e);
        }
        //构建excel的sheet
        WriteSheet writeSheet = EasyExcel.writerSheet().build();
        excelWriter.write(exportList, writeSheet);
        excelWriter.finish();
        // 关闭流
        excelWriter.finish();

    }

    public class ExcelMergeCustomerCellHandler implements CellWriteHandler {
        /**
         * 一级合并的列，从0开始算
         */
        private int[] mergeColIndex;

        /**
         * 从指定的行开始合并，从0开始算
         */
        private int mergeRowIndex;

        /**
         * 在单元格上的所有操作完成后调用，遍历每一个单元格，判断是否需要向上合并
         */
        @Override
        public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
            // 获取当前单元格行下标
            int currRowIndex = cell.getRowIndex();
            // 获取当前单元格列下标
            int currColIndex = cell.getColumnIndex();
            // 判断是否大于指定行下标，如果大于则判断列是否也在指定的需要的合并单元列集合中
            if (currRowIndex > mergeRowIndex) {
                for (int i = 0; i < mergeColIndex.length; i++) {
                    if (currColIndex == mergeColIndex[i]) {
                        if (currColIndex <= 18) {
                            // 一级合并唯一标识
                            Object currLevelOneCode = cell.getRow().getCell(0).getStringCellValue();
                            Object preLevelOneCode = cell.getSheet().getRow(currRowIndex - 1).getCell(0).getStringCellValue();
                            // 判断两条数据的是否是同一集合，只有同一集合的数据才能合并单元格
                            if (preLevelOneCode.equals(currLevelOneCode)) {
                                // 如果都符合条件，则向上合并单元格
                                mergeWithPrevRow(writeSheetHolder, cell, currRowIndex, currColIndex);
                                break;
                            }
                        } else {
                            // 一级合并唯一标识
                            Object currLevelOneCode = cell.getRow().getCell(0).getStringCellValue();
                            Object preLevelOneCode = cell.getSheet().getRow(currRowIndex - 1).getCell(0).getStringCellValue();
                            // 二级合并唯一标识
                            Object currLevelTwoCode = cell.getRow().getCell(19).getStringCellValue();
                            Object preLevelTwoCode = cell.getSheet().getRow(currRowIndex - 1).getCell(19).getStringCellValue();
                            if (preLevelOneCode.equals(currLevelOneCode) && preLevelTwoCode.equals(currLevelTwoCode)) {
                                // 如果都符合条件，则向上合并单元格
                                mergeWithPrevRow(writeSheetHolder, cell, currRowIndex, currColIndex);
                                break;
                            }
                        }
                    }
                }
            }
        }

        /**
         * 当前单元格向上合并
         *
         * @param writeSheetHolder 表格处理句柄
         * @param cell             当前单元格
         * @param currRowIndex     当前行
         * @param currColIndex     当前列
         */
        private void mergeWithPrevRow(WriteSheetHolder writeSheetHolder, Cell cell, int currRowIndex, int currColIndex) {
            // 获取当前单元格数值
            Object currData = cell.getCellTypeEnum() == CellType.STRING ? cell.getStringCellValue() : cell.getNumericCellValue();
            // 获取当前单元格正上方的单元格对象
            Cell preCell = cell.getSheet().getRow(currRowIndex - 1).getCell(currColIndex);
            // 获取当前单元格正上方的单元格的数值
            Object preData = preCell.getCellTypeEnum() == CellType.STRING ? preCell.getStringCellValue() : preCell.getNumericCellValue();
            // 将当前单元格数值与其正上方单元格的数值比较
            if (preData.equals(currData)) {
                Sheet sheet = writeSheetHolder.getSheet();
                List<CellRangeAddress> mergeRegions = sheet.getMergedRegions();
                // 当前单元格的正上方单元格是否是已合并单元格
                boolean isMerged = false;
                for (int i = 0; i < mergeRegions.size() && !isMerged; i++) {
                    CellRangeAddress address = mergeRegions.get(i);
                    // 若上一个单元格已经被合并，则先移出原有的合并单元，再重新添加合并单元
                    if (address.isInRange(currRowIndex - 1, currColIndex)) {
                        sheet.removeMergedRegion(i);
                        address.setLastRow(currRowIndex);
                        sheet.addMergedRegion(address);
                        isMerged = true;
                    }
                }
                // 若上一个单元格未被合并，则新增合并单元
                if (!isMerged) {
                    CellRangeAddress cellRangeAddress = new CellRangeAddress(currRowIndex - 1, currRowIndex, currColIndex, currColIndex);
                    sheet.addMergedRegion(cellRangeAddress);
                }
            }
        }
    }
}




