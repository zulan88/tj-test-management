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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.ContentTemplate;
import net.wanji.business.common.Constants.PartRole;
import net.wanji.business.common.Constants.PartType;
import net.wanji.business.common.Constants.SysType;
import net.wanji.business.common.Constants.TaskCaseStatusEnum;
import net.wanji.business.common.Constants.TaskProcessNode;
import net.wanji.business.common.Constants.TaskStatusEnum;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.bo.SceneTrajectoryBo;
import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.CaseQueryDto;
import net.wanji.business.domain.dto.RoutingPlanDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.dto.TjDeviceDetailDto;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.vo.*;
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
import net.wanji.business.service.RestService;
import net.wanji.business.service.RouteService;
import net.wanji.business.service.TjCaseService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskDataConfigService;
import net.wanji.business.service.TjTaskService;
import net.wanji.business.trajectory.RoutingPlanConsumer;
import net.wanji.business.util.CustomMergeStrategy;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.core.domain.SimpleSelect;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.common.core.page.TableDataInfo;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import net.wanji.system.service.ISysDictDataService;
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
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private ISysDictDataService dictDataService;

    @Autowired
    private TjCaseService tjCaseService;

    @Autowired
    private TjTaskDataConfigService tjTaskDataConfigService;

    @Autowired
    private TjTaskCaseService tjTaskCaseService;

    @Autowired
    private RouteService routeService;

    @Autowired
    private RestService restService;

    @Autowired
    private TjCaseMapper caseMapper;

    @Autowired
    private TjTaskMapper tjTaskMapper;

    @Autowired
    private TjTaskCaseMapper tjTaskCaseMapper;

    @Autowired
    private TjTaskDataConfigMapper tjTaskDataConfigMapper;


    @Autowired
    private TjDeviceDetailMapper deviceDetailMapper;

    @Autowired
    private SceneLabelMap sceneLabelMap;

    @Autowired
    private RoutingPlanConsumer routingPlanConsumer;

    private static final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");

    @Override
    public Map<String, List<SimpleSelect>> initPage() {
        Map<String, List<SimpleSelect>> result = new HashMap<>();
        List<SysDictData> testType = dictTypeService.selectDictDataByType(SysType.TEST_TYPE);
        result.put(SysType.TEST_TYPE, CollectionUtils.emptyIfNull(testType).stream()
                .map(SimpleSelect::new).collect(Collectors.toList()));
        // todo 被测对象类型
        SimpleSelect simpleSelect = new SimpleSelect();
        simpleSelect.setDictValue("域控制器");
        simpleSelect.setDictLabel("域控制器");
        result.put("object_type", Collections.singletonList(simpleSelect));
        // 任务状态
        result.put("task_status", TaskStatusEnum.getSelectList());
        return result;
    }

    @Override
    public Map<String, Long> selectCount(TaskDto taskDto) {
        List<Map<String, String>> statusMaps = tjTaskMapper.selectCountByStatus(taskDto);
        Map<String, Long> statusCountMap = CollectionUtils.emptyIfNull(statusMaps).stream().map(t -> t.get("status"))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        Map<String, Long> result = new HashMap<>();
        for (String status : TaskStatusEnum.getPageCountList()) {
            result.put(status, statusCountMap.getOrDefault(status, 0L));
        }
        return result;
    }

    @Override
    public List<TaskListVo> pageList(TaskDto in) {
        List<TaskListVo> pageList = tjTaskMapper.getPageList(in);
        List<DeviceDetailVo> deviceDetails = deviceDetailMapper.selectByCondition(new TjDeviceDetailDto());
        Map<Integer, DeviceDetailVo> deviceMap = CollectionUtils.emptyIfNull(deviceDetails).stream()
                .collect(Collectors.toMap(DeviceDetailVo::getDeviceId, value -> value));
        for (TaskListVo taskVo : CollectionUtils.emptyIfNull(pageList)) {
            // 测试类型名称
            taskVo.setTestTypeName(dictDataService.selectDictLabel(SysType.TEST_TYPE, taskVo.getTestType()));
            // 任务状态
            taskVo.setStatusName(TaskStatusEnum.getValueByCode(taskVo.getStatus()));
            // 已完成用例
            taskVo.setFinishedCaseCount((int) CollectionUtils.emptyIfNull(taskVo.getTaskCaseVos()).stream()
                    .filter(t -> TaskCaseStatusEnum.PASS.getCode().equals(t.getStatus())).count());
            // 用例配置
            Map<Integer, List<TjTaskDataConfig>> taskCaseConfigMap = CollectionUtils
                    .emptyIfNull(taskVo.getTaskCaseConfigs()).stream().collect(
                            Collectors.groupingBy(TjTaskDataConfig::getCaseId));
            // 主车数量
            String avNum = StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE,
                    dictDataService.selectDictLabel(SysType.PART_ROLE, PartRole.AV),
                    CollectionUtils.emptyIfNull(taskVo.getTaskCaseConfigs()).stream().filter(t ->
                            PartRole.AV.equals(t.getType())).count());
            // 用例
            for (TaskCaseVo taskCaseVo : CollectionUtils.emptyIfNull(taskVo.getTaskCaseVos())) {
                // 排期时间
                taskCaseVo.setPlanDate(taskVo.getPlanDate());
                // 状态
                taskCaseVo.setStatusName(TaskCaseStatusEnum.getValueByCode(taskCaseVo.getStatus()));
                // 场景分类
                if (StringUtils.isNotEmpty(taskCaseVo.getSceneSort())) {
                    StringBuilder labelSort = new StringBuilder();
                    for (String str : taskCaseVo.getSceneSort().split(",")) {
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
                    taskCaseVo.setSceneSort(labelSort.toString());
                }
                // 角色配置
                StringBuilder roleConfigSort = new StringBuilder();
                roleConfigSort.append(avNum);
                List<TjTaskDataConfig> caseConfigs = taskCaseConfigMap.get(taskCaseVo.getCaseId());
                Map<String, List<TjTaskDataConfig>> roleConfigMap = CollectionUtils.emptyIfNull(caseConfigs).stream()
                        .collect(Collectors.groupingBy(TjTaskDataConfig::getType));
                for (Entry<String, List<TjTaskDataConfig>> roleItem : roleConfigMap.entrySet()) {
                    String roleName = dictDataService.selectDictLabel(SysType.PART_ROLE, roleItem.getKey());
                    // 角色配置简述
                    if (roleItem.getValue().stream().anyMatch(item -> !ObjectUtils.isEmpty(item.getDeviceId()))) {
                        Map<Integer, List<TjTaskDataConfig>> deviceGroup = roleItem.getValue().stream().collect(Collectors.groupingBy(TjTaskDataConfig::getDeviceId));
                        for (Entry<Integer, List<TjTaskDataConfig>> deviceEntry : deviceGroup.entrySet()) {
                            if (!ObjectUtils.isEmpty(deviceEntry.getKey()) && deviceMap.containsKey(deviceEntry.getKey())) {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_DEVICE_TEMPLATE, deviceMap.get(deviceEntry.getKey()).getDeviceName(), roleName, deviceEntry.getValue().size()));
                            } else {
                                roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, deviceEntry.getValue().size()));
                            }
                        }
                    } else {
                        roleConfigSort.append(StringUtils.format(ContentTemplate.CASE_ROLE_TEMPLATE, roleName, roleItem.getValue().size()));
                    }
                }
                taskCaseVo.setRoleConfigSort(roleConfigSort.toString());
            }
        }

        return pageList;
    }

    @Override
    public List<CasePageVo> getTaskCaseList(Integer taskId) {
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.selectByCondition(taskCase);
        if (CollectionUtils.isEmpty(taskCaseVos)) {
            return null;
        }
        CaseQueryDto param = new CaseQueryDto();
        param.setSelectedIds(taskCaseVos.stream().map(TaskCaseVo::getCaseId).collect(Collectors.toList()));
        return tjCaseService.pageList(param);
    }

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
                result.put("avDevices", avDevices.stream().map(t -> {
                    TaskTargetVehicleVo targetVehicleVo = new TaskTargetVehicleVo();
                    targetVehicleVo.setDeviceId(t.getDeviceId());
                    targetVehicleVo.setObjectType(t.getDeviceType());
                    targetVehicleVo.setLevel("L3");
                    targetVehicleVo.setBrand(t.getDeviceName());
                    targetVehicleVo.setType(t.getDeviceName());
                    targetVehicleVo.setPerson("张三");
                    targetVehicleVo.setPhone("135****9384");
                    targetVehicleVo.setSelected(t.isSelected());
                    return targetVehicleVo;
                }).collect(Collectors.toList()));
                break;
            case TaskProcessNode.SELECT_CASE:
                if (ObjectUtil.isEmpty(taskSaveDto.getId())) {
                    throw new BusinessException("请确认任务信息是否已保存");
                }
                if (ObjectUtil.isEmpty(taskSaveDto.getCaseQueryDto())) {
                    throw new BusinessException("请确认用例查询条件");
                }
                // 分页查询用例
                List<Integer> selectedIds = new ArrayList<>();
                CaseQueryDto caseQueryDto = taskSaveDto.getCaseQueryDto();
                PageHelper.startPage(caseQueryDto.getPageNum(), caseQueryDto.getPageSize());
                List<CasePageVo> casePageVos = null;
                if (ObjectUtil.isEmpty(caseQueryDto.getShowType())) {
                    caseQueryDto.setSelectedIds(null);
                    casePageVos = tjCaseService.pageList(caseQueryDto);
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
                } else if (1 == caseQueryDto.getShowType()) {
                    // 仅查看选中用例
                    selectedIds = caseQueryDto.getSelectedIds();
                    casePageVos = tjCaseService.pageList(caseQueryDto);
                    CollectionUtils.emptyIfNull(casePageVos).forEach(t -> t.setSelected(Boolean.TRUE));
                } else {
                    selectedIds = caseQueryDto.getSelectedIds();
                    caseQueryDto.setSelectedIds(null);
                    casePageVos = tjCaseService.pageList(caseQueryDto);
                    for (CasePageVo casePageVo : CollectionUtils.emptyIfNull(casePageVos)) {
                        if (selectedIds.contains(casePageVo.getId())) {
                            casePageVo.setSelected(Boolean.TRUE);
                        }
                    }
                }
                // 列表数据+已选择的用例
                TableDataInfo rspData = new TableDataInfo();
                rspData.setData(casePageVos);
                rspData.setTotal(new PageInfo(casePageVos).getTotal());
                result.put("tableData", rspData);
                result.put("selectedIds", selectedIds);
                break;
            case TaskProcessNode.CONFIG:
                TjTask tjTask = this.getById(taskSaveDto.getId());
                if (ObjectUtil.isEmpty(tjTask)) {
                    throw new BusinessException("任务查询失败");
                }
                List<Map<String, Double>> mainTrajectories = null;
                if (ObjectUtil.isNotEmpty(tjTask.getRouteFile())) {
                    try {
                        List<List<TrajectoryValueDto>> trajectoryValues = routeService.readRouteFile(tjTask.getRouteFile());
                        mainTrajectories = trajectoryValues.stream()
                                .map(item -> item.get(0)).map(t -> {
                                    Map<String, Double> map = new HashMap<>();
                                    map.put("longitude", t.getLongitude());
                                    map.put("latitude", t.getLatitude());
                                    map.put("courseAngle", t.getCourseAngle());
                                    return map;
                                }).collect(Collectors.toList());
                    } catch (IOException e) {
                        log.error(StringUtils.format("读取任务{}主车规划后路线文件失败:{}", tjTask.getTaskCode(), e));
                    }
                }
                result.put("taskId", taskSaveDto.getId());
                result.put("planRoute", mainTrajectories);
                result.put("cases", getCaseContinuousInfo(taskSaveDto.getId()));
                break;
            case TaskProcessNode.VIEW_PLAN:
                List<SceneIndexSchemeVo> sceneIndexSchemeVos = restService.getSceneIndexSchemeList(taskSaveDto);
                result.put("data", sceneIndexSchemeVos);
                break;
            default:
                break;
        }
        return result;
    }

    public List<CaseContinuousVo> getCaseContinuousInfo(Integer taskId) {
        // 已选择的用例
        TjTaskCase taskCase = new TjTaskCase();
        taskCase.setTaskId(taskId);
        List<TaskCaseVo> taskCaseVos = tjTaskCaseMapper.selectByCondition(taskCase);
        CaseQueryDto param = new CaseQueryDto();
        param.setSelectedIds(CollectionUtils.emptyIfNull(taskCaseVos).stream().map(TaskCaseVo::getCaseId).collect(Collectors.toList()));

        List<CaseDetailVo> caseDetails = caseMapper.selectCases(param);
        Map<Integer, CaseDetailVo> caseDetailMap = CollectionUtils.emptyIfNull(caseDetails).stream().collect(Collectors.toMap(CaseDetailVo::getId, Function.identity()));
        return CollectionUtils.emptyIfNull(taskCaseVos).stream().map(t -> {
            CaseContinuousVo caseContinuousVo = new CaseContinuousVo();
            BeanUtils.copyBeanProp(caseContinuousVo, t);

            CaseDetailVo caseDetail = caseDetailMap.get(t.getCaseId());
            // 场景分类
            if (ObjectUtil.isNotEmpty(caseDetail) && StringUtils.isNotEmpty(caseDetail.getLabel())) {
                StringBuilder labelSort = new StringBuilder();
                for (String str : caseDetail.getLabel().split(",")) {
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
                caseContinuousVo.setSceneSort(labelSort.toString());
            }
            // 主车轨迹
            List<List<TrajectoryValueDto>> mainSimulations = null;
            try {
                mainSimulations = routeService.readTrajectoryFromRouteFile(caseDetail.getRouteFile(), "1");
                List<Map<String, Double>> mainSimuTrajectories = mainSimulations.stream()
                        .map(item -> item.get(0)).map(n -> {
                            Map<String, Double> map = new HashMap<>();
                            map.put("longitude", n.getLongitude());
                            map.put("latitude", n.getLatitude());
                            map.put("courseAngle", n.getCourseAngle());
                            return map;
                        }).collect(Collectors.toList());
                caseContinuousVo.setMainTrajectory(mainSimuTrajectories);
                caseContinuousVo.setStartPoint(mainSimuTrajectories.get(0));
                caseContinuousVo.setEndPoint(mainSimuTrajectories.get(mainSimuTrajectories.size() - 1));
            } catch (IOException e) {
                log.error(StringUtils.format("读取{}路线文件失败:{}", caseContinuousVo.getCaseNumber(), e));
            } catch (IndexOutOfBoundsException e1) {
                log.error(StringUtils.format("{}主车轨迹信息异常，请检查{}", caseContinuousVo.getCaseNumber(),
                        caseDetail.getRouteFile()));
            }
            // 连接轨迹
            List<Map> trajectoryBos = JSONObject.parseArray(t.getConnectInfo(), Map.class);
            caseContinuousVo.setConnectInfo(trajectoryBos);
            return caseContinuousVo;
        }).sorted(Comparator.comparing(CaseContinuousVo::getSort)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveTask(TaskBo in) throws BusinessException {
        TjTask tjTask = new TjTask();
        if (ObjectUtil.isNotEmpty(in.getId())) {
            tjTask = this.getById(in.getId());
            if (!TaskStatusEnum.NO_SUBMIT.getCode().equals(tjTask.getStatus())) {
                throw new BusinessException("任务已提交，不可修改");
            }
        }
        switch (in.getProcessNode()) {
            case TaskProcessNode.TASK_INFO:
                if (CollectionUtils.isEmpty(in.getAvDeviceIds())) {
                    throw new BusinessException("请选择主车被测设备");
                }
                if (ObjectUtil.isNotEmpty(in.getId())) {
                    tjTask.setClient(in.getClient());
                    tjTask.setConsigner(in.getConsigner());
                    tjTask.setContract(in.getContract());
                    tjTask.setStartTime(in.getStartTime());
                    tjTask.setEndTime(in.getEndTime());
                    this.updateById(tjTask);
                    tjTaskDataConfigService.remove(new QueryWrapper<TjTaskDataConfig>().eq(ColumnName.TASK_ID, tjTask.getId()));
                } else {
                    BeanUtils.copyBeanProp(tjTask, in);
                    tjTask.setTaskCode("task-" + sf.format(new Date()));
                    tjTask.setProcessNode(TaskProcessNode.SELECT_CASE);
                    // todo 排期日期
                    tjTask.setPlanDate(new Date());
                    tjTask.setCreateTime(new Date());
                    tjTask.setStatus(TaskStatusEnum.NO_SUBMIT.getCode());
                    this.save(tjTask);
                }
                List<TjTaskDataConfig> dataConfigs = in.getAvDeviceIds().stream().map(deviceId -> {
                    TjTaskDataConfig config = new TjTaskDataConfig();
                    config.setTaskId(in.getId());
                    config.setType(PartRole.AV);
                    config.setParticipatorId("1");
                    config.setParticipatorName("主车1");
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
                tjTaskCaseService.remove(new QueryWrapper<TjTaskCase>().eq(ColumnName.TASK_ID, in.getId()));

                List<TjTaskCase> taskCases = new ArrayList<>();
                IntStream.range(0, in.getCaseIds().size()).forEach(i -> {
                    TjTaskCase taskCase = new TjTaskCase();
                    taskCase.setTaskId(in.getId());
                    taskCase.setCaseId(in.getCaseIds().get(i));
                    taskCase.setSort(i + 1);
                    taskCase.setCreateTime(new Date());
                    taskCase.setStatus(TaskCaseStatusEnum.WAITING.getCode());
                    taskCases.add(taskCase);
                });
                tjTaskCaseService.saveBatch(taskCases);

                List<TjCase> cases = tjCaseService.listByIds(in.getCaseIds());
                // todo 设备从运营平台查询
                TjDeviceDetailDto deviceDetailDto = new TjDeviceDetailDto();
                deviceDetailDto.setSupportRoles(PartRole.MV_SIMULATION);
                List<DeviceDetailVo> simulationDevices = deviceDetailMapper.selectByCondition(deviceDetailDto);
                if (CollectionUtils.isEmpty(simulationDevices)) {
                    throw new BusinessException("无可用的从车设备");
                }
                List<TjTaskDataConfig> dataConfigList = new ArrayList<>();
                for (TjCase tjCase : cases) {
                    SceneTrajectoryBo sceneTrajectoryBo = JSONObject.parseObject(tjCase.getDetailInfo(), SceneTrajectoryBo.class);
                    // 当前所有从车都使用TESSNG
                    List<TjTaskDataConfig> slaveConfigs = sceneTrajectoryBo.getParticipantTrajectories().stream()
                            .filter(t -> PartType.SLAVE.equals(t.getType())).map(t -> {
                                TjTaskDataConfig config = new TjTaskDataConfig();
                                config.setTaskId(in.getId());
                                config.setCaseId(tjCase.getId());
                                config.setType(PartRole.MV_SIMULATION);
                                config.setParticipatorId(t.getId());
                                config.setParticipatorName(t.getName());
                                config.setDeviceId(simulationDevices.get(0).getDeviceId());
                                return config;
                            }).collect(Collectors.toList());
                    dataConfigList.addAll(slaveConfigs);
                }
                tjTaskDataConfigService.saveBatch(dataConfigList);
                TjTask task = new TjTask();
                task.setId(in.getId());
                task.setProcessNode(TaskProcessNode.CONFIG);
                task.setCaseCount(in.getCaseIds().size());
                updateById(task);
                return in.getId();
            case TaskProcessNode.CONFIG:
                if (ObjectUtil.isEmpty(in.getId())) {
                    throw new BusinessException("请选择任务");
                }
                if (CollectionUtils.isEmpty(in.getCases())) {
                    return in.getId();
                }
                if (CollectionUtils.isNotEmpty(in.getCases()) && StringUtils.isEmpty(in.getRouteFile())) {
                    throw new BusinessException("连续性配置后需要进行路径规划");
                }
                if (in.getCases().subList(0, in.getCases().size() - 1).stream().anyMatch(t -> CollectionUtils.isEmpty((List) t.getConnectInfo()))) {
                    throw new BusinessException("请完善用例连接信息");
                }
                // 修改任务用例信息
                List<TjTaskCase> tjTaskCases = tjTaskCaseService.listByIds(in.getCases().stream()
                        .map(CaseContinuousVo::getId).collect(Collectors.toList()));

                Map<Integer, Object> map = in.getCases().stream().collect(Collectors.toMap(CaseContinuousVo::getId,
                        CaseContinuousVo::getConnectInfo));
                for (int i = 0; i < tjTaskCases.size(); i++) {
                    TjTaskCase taskCase = tjTaskCases.get(i);
                    taskCase.setSort(i + 1);
                    Object obj = map.get(taskCase.getId());
                    taskCase.setConnectInfo(ObjectUtil.isEmpty(obj) ? null : JSONObject.toJSONString(obj));
                    tjTaskCaseMapper.update(taskCase);
                }
                // 修改连续式场景任务完整轨迹信息
                TjTask param = new TjTask();
                param.setId(in.getId());
                param.setProcessNode(TaskProcessNode.VIEW_PLAN);
                param.setContinuous(Boolean.TRUE);
                param.setRouteFile(in.getRouteFile());
                updateById(param);
                return in.getId();
            case TaskProcessNode.VIEW_PLAN:
                tjTask.setProcessNode(TaskProcessNode.WAIT_TEST);
                tjTask.setStatus(TaskStatusEnum.WAITING.getCode());
                updateById(tjTask);
                break;
            default:
                break;
        }
        return 0;
    }

    @Override
    public boolean routingPlan(RoutingPlanDto routingPlanDto) throws BusinessException {
        CaseQueryDto param = new CaseQueryDto();
        param.setSelectedIds(CollectionUtils.emptyIfNull(routingPlanDto.getCases()).stream().map(CaseContinuousVo::getCaseId).collect(Collectors.toList()));

        List<CaseDetailVo> caseDetails = caseMapper.selectCases(param);
        Map<Integer, CaseDetailVo> caseDetailMap = CollectionUtils.emptyIfNull(caseDetails).stream().collect(Collectors.toMap(CaseDetailVo::getId, Function.identity()));

        TjTask task = getById(routingPlanDto.getTaskId());
        routingPlanConsumer.subscribeAndSend(task.getId(), task.getTaskCode());
        List<CaseContinuousVo> caseContinuousInfo = routingPlanDto.getCases();
        for (int i = 0; i < caseContinuousInfo.size(); i++) {
            CaseContinuousVo caseContinuousVo = caseContinuousInfo.get(i);
            caseContinuousVo.setTaskId(routingPlanDto.getTaskId());
            caseContinuousVo.setSort(i + 1);
            CaseDetailVo caseDetail = caseDetailMap.get(caseContinuousVo.getCaseId());
            // 主车轨迹
            try {
                List<List<TrajectoryValueDto>> mainSimulations = routeService.readTrajectoryFromRouteFile(caseDetail.getRouteFile(), "1");
                List<TrajectoryValueDto> valueDtos = mainSimulations.stream()
                        .map(item -> item.get(0)).collect(Collectors.toList());
                caseContinuousVo.setMainTrajectory(valueDtos);
                caseContinuousVo.setStartPoint(valueDtos.get(0));
                caseContinuousVo.setEndPoint(valueDtos.get(valueDtos.size() - 1));
            } catch (IOException e) {
                log.error(StringUtils.format("读取{}路线文件失败:{}", caseContinuousVo.getCaseNumber(), e));
            } catch (IndexOutOfBoundsException e1) {
                log.error(StringUtils.format("{}主车轨迹信息异常，请检查{}", caseContinuousVo.getCaseNumber(),
                        caseDetail.getRouteFile()));
            }
        }
        return restService.startRoutingPlan(caseContinuousInfo);
    }

    private Map<String, Object> updateMap(Map<String, Object> ori) {
        Map<String, Object> startPoint = new HashMap<>();
        startPoint.putAll(ori);
        return startPoint;
    }

    @Override
    public int hasUnSubmitTask() {
        QueryWrapper<TjTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ColumnName.STATUS_COLUMN, TaskStatusEnum.NO_SUBMIT.getCode());
        List<TjTask> list = list(queryWrapper);
        return CollectionUtils.isEmpty(list) ? 0 : list.get(0).getId();
    }

    @Override
    public void export(HttpServletResponse response, Integer taskId) throws IOException {
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

    @Override
    public void saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) {
        String weights = JSON.toJSONString(saveCustomScenarioWeightBo.getWeights());
        tjTaskMapper.saveCustomScenarioWeight(saveCustomScenarioWeightBo.getTask_id(), weights, "0");
    }

    @Override
    public void saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo) {
        String weights = JSON.toJSONString(saveCustomIndexWeightBo.getList());
        tjTaskMapper.saveCustomScenarioWeight(saveCustomIndexWeightBo.getTask_id(), weights, "1");
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

    public synchronized String buildTaskNumber() {
        return StringUtils.format(ContentTemplate.TASK_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getNextNumber(ContentTemplate.TASK_NUMBER_TEMPLATE));
    }
}




