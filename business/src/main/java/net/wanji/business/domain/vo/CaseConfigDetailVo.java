package net.wanji.business.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/1 9:48
 * @Descriptoin:
 */
@Data
public class CaseConfigDetailVo {

    /**
     * 测试目标
     */
    private String testTarget;
    /**
     * 测试对象
     */
    private String evaObject;
    /**
     * 测试场景
     */
    private String testScene;
    /**
     * 标签
     */
    private List<String> labelList;
    /**
     * 配置信息
     */
    private Map<String, List<CasePartConfigVo>> configInfo;
}
