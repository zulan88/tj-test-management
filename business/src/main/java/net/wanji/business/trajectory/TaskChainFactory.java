package net.wanji.business.trajectory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.trajectory.TaskChain.TaskItem;
import net.wanji.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * @author: guanyuduo
 * @date: 2024/1/19 15:59
 * @descriptoin:
 */
@Component
public class TaskChainFactory {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private TjTaskCaseService taskCaseService;

    /**
     * 创建任务链
     * @param taskChainNumber
     * @param taskId
     * @return
     */
    public TaskChainFactory createTaskChain(String taskChainNumber, String createdBy, Integer taskId) {
        TaskChain taskChain = new TaskChain(taskChainNumber, createdBy,
                taskCaseService.list(new LambdaQueryWrapper<TjTaskCase>().eq(TjTaskCase::getTaskId, taskId)));
        TaskChainManager.put(taskChainNumber, taskChain);
        log.info("创建任务链{} -> 任务数{}", taskChainNumber, taskChain.size());
        return this;
    }

    /**
     * 确认状态
     * @param taskChainNumber
     */
    public void confirmState(String taskChainNumber) throws BusinessException {
        if (Stage.WAITING.getCode() != getCurrentNodeStage(taskChainNumber)) {
            log.error("等待中 -> 状态确认中 状态转换失败：{}", outStageWithDuration(taskChainNumber));
            throw new BusinessException("等待中 -> 状态确认中 转换失败");
        }
        getTaskChain(taskChainNumber).confirm();
        outStage(taskChainNumber);
    }

    /**
     * 确认完成
     * @param taskChainNumber
     */
    public void confirmComplete(String taskChainNumber) throws BusinessException {
        if (Stage.CONFIRMING.getCode() != getCurrentNodeStage(taskChainNumber)) {
            log.error("状态确认完成 转换失败：{}", outStageWithDuration(taskChainNumber));
            throw new BusinessException("状态确认完成 转换失败");
        }
        getTaskChain(taskChainNumber).stageComplete();
        outStage(taskChainNumber);
    }

    /**
     * 是否确认完成
     * @param taskChainNumber
     * @return
     */
    public boolean isConfirmed(String taskChainNumber) {
        return Stage.CONFIRMING.getCode() == getCurrentNodeStage(taskChainNumber) && isCompletedStage(taskChainNumber);
    }

    /**
     * 准备
     * @param taskChainNumber
     */
    public void prepare(String taskChainNumber) throws BusinessException {
        if (!isConfirmed(taskChainNumber)) {
            log.error("状态确认完成 -> 测试准备中 状态转换失败：{}", outStageWithDuration(taskChainNumber));
            throw new BusinessException("状态确认完成 -> 测试准备中 状态转换失败");
        }
        getTaskChain(taskChainNumber).prepare();
        outStage(taskChainNumber);
    }

    /**
     * 准备完成
     * @param taskChainNumber
     */
    public void prepareComplete(String taskChainNumber) throws BusinessException {
        if (Stage.PREPARING.getCode() != getCurrentNodeStage(taskChainNumber)) {
            log.error("准备完成 转换失败：{}", outStageWithDuration(taskChainNumber));
            throw new BusinessException("准备完成 转换失败");
        }
        getTaskChain(taskChainNumber).stageComplete();
        outStage(taskChainNumber);
    }

    /**
     * 是否准备完成
     * @param taskChainNumber
     * @return
     */
    public boolean isPrepared(String taskChainNumber) {
        return Stage.PREPARING.getCode() == getCurrentNodeStage(taskChainNumber) && isCompletedStage(taskChainNumber);
    }

    /**
     * 开始
     * @param taskChainNumber
     * @throws BusinessException
     */
    public void start(String taskChainNumber) throws BusinessException {
        if (!this.isPrepared(taskChainNumber)) {
            log.error("开始测试 转换失败：{}", outStageWithDuration(taskChainNumber));
            throw new BusinessException("开始测试 转换失败");
        }
        getTaskChain(taskChainNumber).start();
        outStage(taskChainNumber);
    }

    /**
     * 下一步
     * @param taskChainNumber
     * @return
     * @throws BusinessException
     */
    public boolean next(String taskChainNumber) throws BusinessException {
        TaskChain taskChain = TaskChainManager.get(taskChainNumber);
        try {
            TaskItem item = taskChain.removeFirst();
            taskCaseService.getStatus(item.taskCase, taskChain.getCreatedBy(), true);
            // 查询状态 todo 循环 直到状态为准备好
            while (!this.isConfirmed(taskChainNumber)) {
                try {
                    Thread.sleep(500);
                    taskCaseService.getStatus(item.taskCase, taskChain.getCreatedBy(), false);
                } catch (InterruptedException v1) {
                    log.error("任务链{} -> 下一步失败：{}", taskChainNumber, v1.getMessage());
                } catch (BusinessException v2) {
                    log.error("任务链{} -> 下一步失败：{}", taskChainNumber, v2.getMessage());
                }
            }
            // 准备
            taskCaseService.prepare(item.taskCase, taskChain.getCreatedBy());
            // 开始
            taskCaseService.controlTask(item.taskCase.getTaskId(), item.taskCase.getTaskId(), 1,
                    taskChain.getCreatedBy(), taskChainNumber);
        } catch (NoSuchElementException e) {
            log.error("任务链{} -> 下一步失败：{}", taskChainNumber, e.getMessage());
            destroy(taskChainNumber);
            return false;
        }
        return true;
    }

    /**
     * 销毁任务链
     * @param taskChainNumber
     * @return
     */
    public boolean destroy(String taskChainNumber) {
        return TaskChainManager.remove(taskChainNumber);
    }

    /**
     * 获取当前节点所处阶段是否完成
     * @param taskChainNumber
     * @return
     */
    public boolean isCompletedStage(String taskChainNumber) {
        return hasChain(taskChainNumber) && getCurrentNode(taskChainNumber).stageState;
    }


    public String outStage(String taskChainNumber) {
        return StringUtils.format("任务链{} 第{}节点 当前阶段：{}", taskChainNumber, getCurrentNodeSort(taskChainNumber),
                Stage.getName(getCurrentNodeStage(taskChainNumber)));
    }

    public String outStageWithDuration(String taskChainNumber) {
        return StringUtils.format("任务链{} 第{}节点 当前阶段：{} 持续时长：{}ms", taskChainNumber, getCurrentNodeSort(taskChainNumber),
                Stage.getName(getCurrentNodeStage(taskChainNumber)), getStageDuration(taskChainNumber));
    }

    /**
     * 计算当前阶段持续时长
     * @param taskChainNumber
     * @return
     */
    public long getStageDuration(String taskChainNumber) {
        return System.currentTimeMillis() - getCurrentNode(taskChainNumber).stageStartTime;
    }

    /**
     * 任务链是否存在
     * @param taskChainNumber
     * @return
     */
    public boolean hasChain(String taskChainNumber) {
        return TaskChainManager.containsKey(taskChainNumber);
    }

    /**
     * 获取任务链
     * @param taskChainNumber
     * @return
     */
    public TaskChain getTaskChain(String taskChainNumber) {
        return TaskChainManager.get(taskChainNumber);
    }

    /**
     * 获取当前节点
     * @param taskChainNumber
     * @return
     */
    public TaskItem getCurrentNode(String taskChainNumber) {
        return hasChain(taskChainNumber)
                ? getTaskChain(taskChainNumber).getFirst()
                : null;
    }

    /**
     * 获取当前节点排序
     * @param taskChainNumber
     * @return
     */
    public Integer getCurrentNodeSort(String taskChainNumber) {
        return hasChain(taskChainNumber)
                ? getCurrentNode(taskChainNumber).sort
                : null;
    }

    /**
     * 获取当前节点用例信息
     * @param taskChainNumber
     * @return
     */
    public TjTaskCase getCurrentNodeCase(String taskChainNumber) {
        return hasChain(taskChainNumber)
                ? getCurrentNode(taskChainNumber).taskCase
                : null;
    }

    /**
     * 获取当前节点ID
     * @param taskChainNumber
     * @return
     */
    public Integer getCurrentNodeId(String taskChainNumber) {
        return hasChain(taskChainNumber)
                ? getCurrentNode(taskChainNumber).taskCase.getId()
                : null;
    }

    /**
     * 获取当前节点所处阶段
     * @param taskChainNumber
     * @return
     */
    public Integer getCurrentNodeStage(String taskChainNumber) {
        return hasChain(taskChainNumber)
                ? getCurrentNode(taskChainNumber).stage
                : -1;
    }



    public enum Stage {
        WAITING(0, "测试等待中"),
        CONFIRMING(1, "状态确认中"),
        PREPARING(1, "测试准备中"),
        RUNNING(3, "测试执行中"),
        END(4, "结束");

        private int code;
        private String name;

        Stage(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public static String getName(int code) {
            for (Stage stage : Stage.values()) {
                if (stage.code == code) {
                    return stage.getName();
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }
}
