package net.wanji.business.domain.bo;

import lombok.Data;
import net.wanji.business.entity.TjCase;

import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/24 13:36
 * @Descriptoin:
 */
@Data
public class CaseInfoBo extends TjCase {

    private String sceneName;

    private String filePath;

    private String geoJsonPath;

    private List<CaseConfigBo> caseConfigs;
}
