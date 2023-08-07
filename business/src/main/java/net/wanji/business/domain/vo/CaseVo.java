package net.wanji.business.domain.vo;

import net.wanji.business.entity.TjCase;
import net.wanji.common.annotation.Excel;
import net.wanji.common.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/29 17:23
 * @Descriptoin:
 */

public class CaseVo extends TjCase {

    @Excel(name = "资源名称")
    private String resourcesName;

    private Integer hazardIndex;

    private List<String> labelList;

    public String getResourcesName() {
        return resourcesName;
    }

    public void setResourcesName(String resourcesName) {
        this.resourcesName = resourcesName;
    }

    public Integer getHazardIndex() {
        return hazardIndex;
    }

    public void setHazardIndex(Integer hazardIndex) {
        this.hazardIndex = hazardIndex;
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
