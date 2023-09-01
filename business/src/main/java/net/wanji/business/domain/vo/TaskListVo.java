package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.entity.TjTask;

import java.util.List;

/**
 * @author: guowenhao
 * @date: 2023/8/31 17:44
 * @description: 测试任务列表
 */
@Data
public class TaskListVo extends TjTask {

    /**
     * 主车名称
     */
    private String mainCarName;

    /**
     * 测试用例待测试/总数
     */
    private String status;

    /**
     * 测试用例列表
     */
    private List<TaskCaseVo> taskCaseVos;
}
