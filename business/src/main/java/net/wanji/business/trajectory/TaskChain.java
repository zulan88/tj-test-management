package net.wanji.business.trajectory;

import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.trajectory.TaskChain.TaskItem;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * @author: guanyuduo
 * @date: 2024/1/19 15:38
 * @descriptoin:
 */

public class TaskChain extends LinkedList<TaskItem> {

    private String taskChainNumber;

    private String createdBy;

    public TaskChain(String taskChainNumber, String createdBy, List<TjTaskCase> taskCases) {
        super(taskCases.stream().map(TaskItem::new).collect(Collectors.toList()));
        this.taskChainNumber = taskChainNumber;
        this.createdBy = createdBy;
    }

    public void confirm() {
        this.getFirst().stage = 1;
        this.getFirst().stageStartTime = System.currentTimeMillis();
    }

    public void prepare() {
        this.getFirst().stage = 2;
        this.getFirst().stageStartTime = System.currentTimeMillis();
    }

    public void start() {
        this.getFirst().stage = 3;
        this.getFirst().stageStartTime = System.currentTimeMillis();
    }

    public void stageComplete() {
        this.getFirst().stageState = true;
    }

    public String getTaskChainNumber() {
        return taskChainNumber;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public static class TaskItem {
        /**
         * 阶段 0：等待中 1：状态确认中 2：准备中 3：执行中
         */
        int stage;

        /**
         * 阶段状态
         */
        boolean stageState;

        /**
         * 任务排序
         */
        int sort;

        /**
         * 阶段开始时间
         */
        long stageStartTime;

        /**
         * 任务内容
         */
        TjTaskCase taskCase;

        TaskItem(TjTaskCase taskCase) {
            this.taskCase = taskCase;
            this.sort = taskCase.getSort();
        }

    }
}
