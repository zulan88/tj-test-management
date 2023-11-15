package net.wanji.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import net.wanji.business.domain.vo.CaseRealTestVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.TaskCaseVerificationPageVo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.exception.BusinessException;

import java.io.IOException;
import java.util.List;

/**
* @author guowenhao
* @description 针对表【tj_task_case(测试任务-用例详情表)】的数据库操作Service
* @createDate 2023-08-31 17:39:16
*/
public interface TjTaskCaseService extends IService<TjTaskCase> {



    /**
     * 获取任务用例中参与者状态
     * @param taskCaseId
     * @return
     */
    TaskCaseVerificationPageVo getStatus(Integer taskCaseId) throws BusinessException;

    /**
     * 准备（开始倒计时）
     * @param taskCaseId
     * @return
     */
    CaseRealTestVo prepare(Integer taskCaseId) throws BusinessException;

    /**
     * 开始/结束/回放
     * @param recordId
     * @param action
     * @return
     */
    CaseRealTestVo start(Integer recordId, Integer action) throws BusinessException, IOException;

    void playback(Integer recordId, Integer action) throws BusinessException, IOException;
    /**
     * 获取实车验证结果
     * @param recordId
     * @return
     */
    RealTestResultVo getResult(Integer recordId) throws BusinessException;

    /**
     * 通信时延
     * @param recordId
     * @return
     */
    CommunicationDelayVo communicationDelayVo(Integer recordId);

    List<TaskReportVo> getReport(Integer taskId, Integer taskCaseId);
}
