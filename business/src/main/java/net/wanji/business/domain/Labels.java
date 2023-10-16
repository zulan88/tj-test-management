package net.wanji.business.domain;


import lombok.Data;


/**
 * labels对象 labels
 * 
 * @author wanji
 * @date 2023-10-16
 */
@Data
public class Labels
{

    /** id */
    private Long id;

    /** 名称 */
    private String name;

    /** 父id */
    private Long parentId;

    private boolean status;

}
