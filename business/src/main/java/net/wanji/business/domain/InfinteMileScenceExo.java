package net.wanji.business.domain;

import lombok.Data;
import net.wanji.business.entity.InfinteMileScence;

import java.util.List;

@Data
public class InfinteMileScenceExo extends InfinteMileScence {

    List<InElement> inElements;

    List<TrafficFlow> trafficFlows;

    List<SiteSlice> siteSlices;

    List<TrafficFlowConfig> trafficFlowConfigs;

    private Integer action;

}
