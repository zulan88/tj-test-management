package net.wanji.business.domain.dto;

import java.util.List;

import lombok.Data;
import net.wanji.business.entity.TjTaskDataConfig;
import net.wanji.business.entity.TjTaskDc;

/**
 * @author: guowenhao
 * @date: 2023/8/31 19:46
 * @description:
 */
@Data
public class TaskDto {

    private Integer id;

    private String taskName;

    private String caseIds;

    private List<TjTaskDataConfig> dataConfigs;

    private List<TjTaskDc> diadynamicCriterias;
}
