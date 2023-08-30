package net.wanji.business.service;

import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.vo.RealVehicleVerificationPageVo;
import net.wanji.business.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 9:57
 * @Descriptoin:
 */

public interface TestingService {

    /**
     * 获取用例中参与者状态
     * @param caseId
     * @return
     */
    RealVehicleVerificationPageVo getStatus(Integer caseId) throws BusinessException;

    /**
     * 开始测试
     * @param caseId
     * @return
     */
    boolean start(Integer caseId) throws BusinessException;

    /**
     * 与主控交互
     * @param caseId
     * @return
     */
    boolean sendRule(Integer caseId, Integer action) throws BusinessException;


    /**
     * 获取实车验证结果
     * @param caseId
     * @return
     */
    boolean getResult(Integer caseId) throws BusinessException;
}
