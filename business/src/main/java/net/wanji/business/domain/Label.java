package net.wanji.business.domain;


import lombok.Data;
import net.wanji.business.domain.vo.TreeNode;

import java.util.ArrayList;
import java.util.List;


/**
 * labels对象 labels
 * 
 * @author wanji
 * @date 2023-10-16
 */
@Data
public class Label
{

    /** id */
    private Long id;

    /** 名称 */
    private String name;

    /** 父id */
    private Long parentId;

    private boolean status;

    private List<Label> children;

    public List<Label> getChildren() {
        if(children==null){
            children = new ArrayList<>();
        }
        return children;

    }

}
