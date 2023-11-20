package net.wanji.business.domain.param;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author: guanyuduo
 * @date: 2023/11/16 10:59
 * @descriptoin: 用例轨迹参数
 */
@Data
public class CaseTrajectoryParam {

    private Integer taskId;

    private Map<String, String> vehicleIdTypeMap;

    private String dataChannel;

    private List<CaseSSInfo> caseTrajectorySSVoList;

}
