package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;

import net.wanji.business.domain.bo.TaskBo;
import net.wanji.business.domain.dto.CreateTaskDto;
import net.wanji.business.domain.dto.TaskDto;
import net.wanji.business.domain.vo.TaskVo;
import net.wanji.business.entity.TjTask;
import net.wanji.business.exception.BusinessException;
import net.wanji.common.core.page.TableDataInfo;

import java.util.concurrent.ExecutionException;

/**
* @author guowenhao
* @description 针对表【tj_task(测试任务表)】的数据库操作Service
* @createDate 2023-08-31 17:39:16
*/
public interface TjTaskService extends IService<TjTask> {

    /**
     * 页面列表
     * @param in
     * @return
     */
    public TableDataInfo pageList(TaskBo in);

    /**
     * 创建任务
     * @param in
     * @return
     * @throws BusinessException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public TaskVo createTask(CreateTaskDto in)
        throws BusinessException, ExecutionException, InterruptedException;

    /**
     * 保存创建任务
     * @param in
     * @return
     */
    public int saveTask(TaskDto in);
}
