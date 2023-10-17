package net.wanji.business.domain.vo;

import lombok.Data;
import net.wanji.business.domain.Labels;

import java.util.ArrayList;
import java.util.List;

@Data
public class TreeNode {

    private Labels data;
    private List<TreeNode> children;

    public List<TreeNode> getChildren() {
        if(children==null){
            children = new ArrayList<>();
        }
        return children;

    }
}
