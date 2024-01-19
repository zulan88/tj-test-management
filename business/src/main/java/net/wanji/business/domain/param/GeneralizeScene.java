package net.wanji.business.domain.param;

import lombok.Data;

@Data
public class GeneralizeScene {

    private Integer id;

    private Integer minSpeed;

    private Integer maxSpeed;

    private Integer step;
}
