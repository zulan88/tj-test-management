package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.entity.TjCase;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/28 13:11
 * @Descriptoin:
 */
@Data
public class CaseDetailVo extends TjCase {

    private String sceneNumber;

    private String resourceName;



}
