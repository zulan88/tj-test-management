package net.wanji.business.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/10 17:07
 * @Descriptoin:
 */
@Data
public class RouteVo {

    /**
     * 倒计时 00:00:00
     */
    private String countdown;

    /**
     * 路线
     */
    private List<List<Map>> route;
}
