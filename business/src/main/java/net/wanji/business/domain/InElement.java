package net.wanji.business.domain;

import lombok.Data;

import java.util.List;

@Data
public class InElement {

    String name;

    Integer expectedSpeed;

    // 0-被测对象，1-其他对象
    Integer type;

    List<SitePoint> route;

}
