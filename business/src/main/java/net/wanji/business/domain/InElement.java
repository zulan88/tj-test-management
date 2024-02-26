package net.wanji.business.domain;

import lombok.Data;

import java.util.List;

@Data
public class InElement {

    String name;

    Integer expectedSpeed;

    List<SitePoint> route;

}
