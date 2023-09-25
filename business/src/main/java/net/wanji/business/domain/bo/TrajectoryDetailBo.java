package net.wanji.business.domain.bo;

import lombok.Data;
import net.wanji.common.utils.StringUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 18:05
 * @Descriptoin:
 */
@Data
public class TrajectoryDetailBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String time;
    private Long frameId;
    private String position;
    private String longitude;
    private String latitude;
    private String lane;
    private Double speed;
    private String speedUnit;
    private boolean pass;
    private String reason = "等待校验";

    public void setPosition(String position) {
        if (StringUtils.isNotEmpty(position)) {
            this.position = position;
            String[] pos = position.split(",");
            if (!ObjectUtils.isEmpty(pos)) {
                this.longitude = pos[0];
                this.latitude = pos[1];
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrajectoryDetailBo that = (TrajectoryDetailBo) o;
        return Objects.equals(type, that.type) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, position);
    }
}
