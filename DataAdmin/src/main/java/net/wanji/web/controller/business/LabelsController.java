package net.wanji.web.controller.business;

import net.wanji.business.domain.Label;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.TreeNode;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.*;

@RestController
@RequestMapping("/labels")
public class LabelsController extends BaseController {

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;


    //输出为树形结构
    @GetMapping("/list")
    public AjaxResult list(Integer id) throws BusinessException {
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, Label> labelToNodeMap = new HashMap<>();
        List<Label> roots = new ArrayList<>();
        Set<Long> set = new HashSet<>();
        if(id!=null) {
            FragmentedScenesDetailVo detailVo = tjFragmentedSceneDetailService.getDetailVo(id);
            List<String> labels = detailVo.getLabelList();
            for (String str : labels) {
                try {
                    long intValue = Long.parseLong(str);
                    set.add(intValue);
                } catch (NumberFormatException e) {
                    // 处理无效的整数字符串
                }
            }
        }
        for (Label label : labelList) {
            TreeNode node = new TreeNode();
            if(set.contains(label.getId())){
                label.setStatus(true);
            }
            node.setData(label);
            labelToNodeMap.put(label.getId(), label);

            if (label.getParentId() == null) {
                roots.add(label);
            }
        }
        for (Label label : labelList) {
            Label currentNode = labelToNodeMap.get(label.getId());
            Label parentNode = labelToNodeMap.get(label.getParentId());

            if (parentNode != null) {
                parentNode.getChildren().add(currentNode);
            }
        }

        return AjaxResult.success(roots);
    }

    @PostMapping
    public AjaxResult add(@RequestBody Label label){
        return toAjax(labelsService.insertLabels(label));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Label label){
        return toAjax(labelsService.updateLabels(label));
    }

    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(labelsService.deleteLabelsByIds(ids));
    }

}
