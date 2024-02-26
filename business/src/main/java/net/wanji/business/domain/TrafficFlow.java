package net.wanji.business.domain;

import lombok.Data;

import java.util.List;

@Data
public class TrafficFlow {

    //安全时距
    Integer  safeInterval;

    //安全间距
    Integer  safeDistance;

    List<DeparturePoint> departurePoints;

}
