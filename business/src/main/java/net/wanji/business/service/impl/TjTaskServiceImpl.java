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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.CreateTaskDto;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskListVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.domain.vo.TaskVo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.TjTaskDc;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.mapper.TjTaskDataConfigMapper;
import net.wanji.business.mapper.TjTaskDcMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.business.util.CustomMergeStrategy;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.common.utils.file.FileUtils;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author guowenhao
 * @description 针对表【tj_task(测试任务表)】的数据库操作Service实现
 * @createDate 2023-08-31 17:39:16
 */
@Service
public class TjTaskServiceImpl extends ServiceImpl<TjTaskMapper, TjTask>
        implements TjTaskService {

    @Autowired
    private TjTaskMapper tjTaskMapper;

    @Autowired
    private TjTaskCaseMapper tjTaskCaseMapper;

    @Autowired
    private TjTaskDataConfigMapper tjTaskDataConfigMapper;

    @Autowired
    private TjTaskDcMapper tjTaskDcMapper;

    @Autowired
    private TjCaseService tjCaseService;

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    @Override
    public TableDataInfo pageList(TaskDto in) {
        TableDataInfo tableDataInfo = new TableDataInfo();
        QueryWrapper<TjTask> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(in.getTaskName()))
            wrapper.eq("task_name", in.getTaskName());
        if (ObjectUtil.isNotEmpty(in.getStartTime()))
            wrapper.gt("start_time", in.getStartTime());
        if (ObjectUtil.isNotEmpty(in.getEndTime()))
            wrapper.lt("end_time", in.getEndTime());
        Page<TjTask> tjTaskPage = tjTaskMapper.selectPage(
                new Page<>(in.getPageNum(), in.getPageSize()), wrapper);
        tableDataInfo.setTotal(tjTaskPage.getTotal());
        List<TaskListVo> taskListVos = new ArrayList<>();
        for (TjTask record : tjTaskPage.getRecords()) {
            TaskListVo taskListVo = new TaskListVo();
            TjTaskDataConfig tjTaskDataConfig = tjTaskDataConfigMapper.selectOne(
                    new QueryWrapper<TjTaskDataConfig>().eq("task_id",
                            record.getId()).eq("type", "av"));
            List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.gstList(record.getId());
            int num = 0;
            for (TaskCaseVo taskCaseVo : taskCaseVos) {
                taskCaseVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
                if (taskCaseVo.getStatus().equals("待测试"))
                    num++;
            }
            BeanUtils.copyBeanProp(taskListVo, record);
            if (num == 0)
                num = taskCaseVos.size();
            taskListVo.setStatus(num + "/" + taskCaseVos.size());
            taskListVo.setMainCarName(tjTaskDataConfig.getParticipatorName());
            for (TaskCaseVo taskCaseVo : CollectionUtils.emptyIfNull(taskCaseVos)) {
                if (StringUtils.isNotEmpty(taskCaseVo.getTestTotalTime())) {
                    taskCaseVo.setTestTotalTime(DateUtils.secondsToDuration(Integer.parseInt(taskCaseVo.getTestTotalTime())));
                }
            }
            taskListVo.setTaskCaseVos(taskCaseVos);
            taskListVos.add(taskListVo);
        }
        tableDataInfo.setData(taskListVos);
        return tableDataInfo;
    }

    @Override
    public TaskVo createTask(CreateTaskDto in)
            throws BusinessException, ExecutionException, InterruptedException {
        TaskVo taskVo = new TaskVo();
        String[] split = in.getCaseIds().split(",");
        taskVo.setCaseCount(in.getCaseIds().split(",").length);
        taskVo.setTaskName("task-" + sf.format(new Date()));
        taskVo.setCaseIds(in.getCaseIds());
        taskVo.setDataConfigs(tjCaseService.getTaskConfigDetail(Integer.parseInt(split[0])));
        return taskVo;
    }

    @Override
    @Transactional
    public int saveTask(TaskBo in) {
        String[] cases = in.getCaseIds().split(",");
        TjTask tjTask = new TjTask();
        tjTask.setTaskName(in.getTaskName());
        tjTask.setCaseCount(cases.length);
        tjTask.setCreateTime(new Date());
        tjTaskMapper.insert(tjTask);
        for (String aCase : cases) {
            TjTaskCase tjTaskCase = new TjTaskCase();
            tjTaskCase.setTaskId(tjTask.getId());
            tjTaskCase.setCaseId(Integer.parseInt(aCase));
            tjTaskCase.setCreateTime(new Date());
            tjTaskCase.setStatus("待测试");
            tjTaskCaseMapper.insert(tjTaskCase);
        }
        List<TjTaskDataConfig> dataConfigs = in.getDataConfigs();
        for (TjTaskDataConfig dataConfig : dataConfigs) {
            dataConfig.setTaskId(tjTask.getId());
            tjTaskDataConfigMapper.insert(dataConfig);
        }
        List<TjTaskDc> diadynamicCriterias = in.getDiadynamicCriterias();
        for (TjTaskDc diadynamicCriteria : diadynamicCriterias) {
            diadynamicCriteria.setTaskId(tjTask.getId());
            tjTaskDcMapper.insert(diadynamicCriteria);
        }
        return 1;
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
        response.setHeader("download-filename", FileUtils.percentEncode(task.getTaskName().concat(".xlsx")));


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




