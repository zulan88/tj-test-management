package net.wanji.business.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/12 13:32
 * @Descriptoin:
 */
@AllArgsConstructor
@Data
public class PlaybackMessage {

    private String countDown;

    private List<Map> data;
}
