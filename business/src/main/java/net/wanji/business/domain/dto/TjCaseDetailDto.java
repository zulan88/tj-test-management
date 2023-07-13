package net.wanji.business.domain.dto;

import lombok.Data;
import net.wanji.business.common.Constants.UpdateGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 15:23
 * @Descriptoin:
 */
@Data
public class TjCaseDetailDto {

    @NotNull(message = "请选择一条数据", groups = UpdateGroup.class)
    private Integer id;

    @NotBlank(message = "请输入topic", groups = UpdateGroup.class)
    private String topic;

    @NotBlank(message = "请上传文件", groups = UpdateGroup.class)
    private String localFile;
}
