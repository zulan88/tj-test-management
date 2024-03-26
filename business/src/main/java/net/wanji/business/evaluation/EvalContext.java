package net.wanji.business.evaluation;

import lombok.Data;
import lombok.ToString;

/**
 * @author hcy
 * @version 1.0
 * @className ContextDto
 * @description TODO
 * @date 2024/3/26 15:23
 **/
@Data
@ToString
public class EvalContext {
  private Integer taskId;
  private Integer caseId;
}
