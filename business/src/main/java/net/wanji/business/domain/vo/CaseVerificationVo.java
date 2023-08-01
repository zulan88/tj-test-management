package net.wanji.business.domain.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import net.wanji.business.domain.bo.CaseTrajectoryDetailBo;
import net.wanji.business.entity.TjCase;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/3 17:56
 * @Descriptoin:
 */
@Data
public class CaseVerificationVo {

    private Integer id;

    private CaseTrajectoryDetailBo detailInfo;

    public CaseVerificationVo(TjCase tjCase) {
        this.id = tjCase.getId();
        this.detailInfo = JSONObject.parseObject(tjCase.getDetailInfo(), CaseTrajectoryDetailBo.class);
    }

}
