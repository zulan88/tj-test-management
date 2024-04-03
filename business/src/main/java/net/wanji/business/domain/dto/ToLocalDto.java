package net.wanji.business.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.wanji.business.service.record.impl.FileWriteRunnable;

/**
 * @author hcy
 * @version 1.0
 * @className ToLocalDto
 * @description TODO
 * @date 2024/4/2 11:03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToLocalDto {
  private Integer taskId;
  private Integer caseId;
  private String fileName;
  private Integer fileRecordId;
  private FileWriteRunnable toLocalThread;
}
