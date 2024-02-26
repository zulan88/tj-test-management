package net.wanji.business.domain;

import lombok.Data;

@Data
public class DeparturePoint {

    String name;

    String carType;

    String timePeriod;

    Integer carNumber;

    String latitude;
    String longitude;

}
