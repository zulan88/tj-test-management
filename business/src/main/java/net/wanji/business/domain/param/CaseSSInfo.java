package net.wanji.business.domain.param;

import lombok.Data;

/**
 * @author: guanyuduo
 * @date: 2023/11/15 18:10
 * @descriptoin: 用例起止点信息
 */
@Data
public class CaseSSInfo {
    private Integer caseId;
    private Integer taskId;
    private Double endLatitude;
    private Double endLongitude;
    private Double startLatitude;
    private Double startLongitude;
}
