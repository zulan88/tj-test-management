package net.wanji.business.domain.bo;

import lombok.Data;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.entity.TjTaskCaseRecord;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 13:36
 * @Descriptoin:
 */
@Data
public class TaskCaseInfoBo extends TjTaskCase {

    private String caseNumber;

    private String sceneName;

    private String allStageLabel;

    private Integer recordId;

    private String filePath;

    private String geoJsonPath;

    /**
     * 任务用例测试点位详情
     */
    private String detailInfo;

    /**
     * 任务用例测试轨迹文件
     */
    private String routeFile;

    /**
     * 实车试验点位详情
     */
    private String realDetailInfo;

    /**
     * 实车试验轨迹文件
     */
    private String realRouteFile;


    /**
     * 用例原点位详情
     */
    private String caseDetailInfo;

    /**
     * 用例原轨迹文件
     */
    private String caseRouteFile;

    private Integer recordStatus;

    private List<TaskCaseConfigBo> dataConfigs;
}
