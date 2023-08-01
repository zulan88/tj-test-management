package net.wanji.business.domain.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
public class FragmentedScenesDetailVo extends TjFragmentedSceneDetail {

    private String typeName;

    /**
     * 地图名称
     */
    private String resourcesName;

    /**
     * 道路类型名称
     */
    private String roadTypeName;

    private String roadWayName;

    private String roadConditionName;

    private String weatherName;

    private String trafficFlowStatusName;

    /**
     * 场景复杂度名称
     */
    private String sceneComplexityName;

    private String sceneTypeName;

    private Map trajectoryJson;

    private List<String> labelList;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getResourcesName() {
        return resourcesName;
    }

    public void setResourcesName(String resourcesName) {
        this.resourcesName = resourcesName;
    }

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

    public String getRoadConditionName() {
        return roadConditionName;
    }

    public void setRoadConditionName(String roadConditionName) {
        this.roadConditionName = roadConditionName;
    }

    public String getWeatherName() {
        return weatherName;
    }

    public void setWeatherName(String weatherName) {
        this.weatherName = weatherName;
    }

    public String getTrafficFlowStatusName() {
        return trafficFlowStatusName;
    }

    public void setTrafficFlowStatusName(String trafficFlowStatusName) {
        this.trafficFlowStatusName = trafficFlowStatusName;
    }

    public String getSceneComplexityName() {
        return sceneComplexityName;
    }

    public void setSceneComplexityName(String sceneComplexityName) {
        this.sceneComplexityName = sceneComplexityName;
    }

    public String getSceneTypeName() {
        return sceneTypeName;
    }

    public void setSceneTypeName(String sceneTypeName) {
        this.sceneTypeName = sceneTypeName;
    }

    public Map getTrajectoryJson() {
        try{
            if (StringUtils.isNotEmpty(this.getTrajectoryInfo())) {
                return JSONObject.parseObject(this.getTrajectoryInfo(), Map.class);
            }
        }catch (Exception e){
            if(log.isErrorEnabled()){
                log.error("parse error!", e);
            }
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
