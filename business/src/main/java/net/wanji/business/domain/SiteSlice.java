package net.wanji.business.domain;

import lombok.Data;
import java.util.List;

@Data
public class SiteSlice {

    String sliceName;

    List<SitePoint> route;

}
