package net.wanji.business.domain.vo;

import com.alibaba.fastjson2.JSONObject;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.common.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/28 13:20
 * @Descriptoin:
 */
public class FragmentedScenesDetailVo extends TjFragmentedSceneDetail {

    private String roadTypeName;

    private String roadWayName;

    private Map trajectoryJson;

    private List<String> labelList;

    public String getRoadTypeName() {
        return roadTypeName;
    }

    public void setRoadTypeName(String roadTypeName) {
        this.roadTypeName = roadTypeName;
    }

    public String getRoadWayName() {
        return roadWayName;
    }

    public void setRoadWayName(String roadWayName) {
        this.roadWayName = roadWayName;
    }

    public Map getTrajectoryJson() {
        if (StringUtils.isNotEmpty(this.getTrajectoryInfo())) {
            return JSONObject.parseObject(this.getTrajectoryInfo(), Map.class);
        }
        return trajectoryJson;
    }

    public void setTrajectoryJson(Map<String, Object> trajectoryJson) {
        this.trajectoryJson = trajectoryJson;
    }


    public List<String> getLabelList() {
        if (StringUtils.isNotEmpty(this.getLabel())) {
            return Arrays.stream(this.getLabel().split(",")).collect(Collectors.toList());
        }
        return labelList;
    }

    public void setLabelList(List<String> labelList) {
        this.labelList = labelList;
    }
}
