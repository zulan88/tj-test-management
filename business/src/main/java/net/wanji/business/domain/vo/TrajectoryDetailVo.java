package net.wanji.business.domain.vo;

import lombok.Data;

@Data
public class TrajectoryDetailVo {

    public TrajectoryDetailVo(Long frameId, boolean pass){
        this.frameId = frameId;
        this.pass = pass;
    }

    private Long frameId;

    private boolean pass;

}
