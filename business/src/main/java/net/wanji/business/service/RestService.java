package net.wanji.business.service;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 15:25
 * @Descriptoin:
 */

public interface RestService {

    /**
     * 开始仿真
     */
    boolean start(Integer caseId, String channel);
}
