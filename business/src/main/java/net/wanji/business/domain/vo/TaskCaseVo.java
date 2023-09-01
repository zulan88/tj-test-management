package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.entity.TjTaskCase;

/**
 * @author: guowenhao
 * @date: 2023/8/31 17:50
 * @description: 测试任务-用例页面使用
 */
@Data
public class TaskCaseVo extends TjTaskCase {

    /**
     * 主车名称
     */
    private String mainCarName;

    /**
     * 用例名称
     */
    private String caseName;

}
