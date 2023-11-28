package net.wanji.business.service;

import net.wanji.business.domain.vo.CaseTestPrepareVo;
import net.wanji.business.domain.vo.CaseTestStartVo;
import net.wanji.business.domain.vo.CommunicationDelayVo;
import net.wanji.business.domain.vo.RealTestResultVo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.exception.BusinessException;

import java.io.IOException;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 9:57
 * @Descriptoin:
 */

public interface TestingService {

    void resetStatus(Integer caseId) throws BusinessException;

    /**
     * 获取用例中参与者状态
     *
     * @param caseId
     * @return
     */
    RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException;

    /**
     * 准备（开始倒计时）
     *
     * @param caseId
     * @return
     */
    CaseTestPrepareVo prepare(Integer caseId) throws BusinessException;

    /**
     * 开始/结束/回放
     *
     * @param caseId
     * @param action
     * @return
     */
    CaseTestStartVo start(Integer caseId, Integer action,String key,String username) throws BusinessException, IOException;

    void end(Integer caseId, String channel, int action);

    CaseTestStartVo controlTask(Integer caseId) throws BusinessException, IOException;

    CaseTestStartVo hjktest(Integer caseId) throws BusinessException;

    void playback(Integer recordId, Integer action) throws BusinessException, IOException;

    /**
     * 获取实车验证结果
     *
     * @param recordId
     * @return
     */
    RealTestResultVo getResult(Integer recordId) throws BusinessException;

    /**
     * 通信时延
     *
     * @param recordId
     * @return
     */
    CommunicationDelayVo communicationDelayVo(Integer recordId);
}
