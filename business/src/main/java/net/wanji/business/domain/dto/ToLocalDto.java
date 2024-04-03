package net.wanji.business.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.wanji.business.service.record.impl.FileWriteRunnable;

import java.util.Objects;

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
  private Integer fileId;
  private FileWriteRunnable toLocalThread;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ToLocalDto that = (ToLocalDto) o;
    return Objects.equals(taskId, that.taskId) && Objects.equals(caseId,
        that.caseId) && Objects.equals(fileId, that.fileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, caseId, fileId);
  }
}
