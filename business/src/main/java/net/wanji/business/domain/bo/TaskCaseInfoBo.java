package net.wanji.business.domain.bo;

import lombok.Data;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjTaskCaseRecord;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 13:36
 * @Descriptoin:
 */
@Data
public class TaskCaseInfoBo extends TjCase {

    private Integer taskCaseId;

    private Integer taskId;

    private String sceneName;

    private String trajectoryInfo;

    private String allStageLabel;

    private String filePath;

    private String geoJsonPath;

    private TjTaskCaseRecord taskCaseRecord;

    private List<TaskCaseConfigBo> caseConfigs;
}
