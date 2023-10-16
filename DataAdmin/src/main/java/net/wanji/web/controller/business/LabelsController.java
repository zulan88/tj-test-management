package net.wanji.web.controller.business;

import net.wanji.business.domain.Labels;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.TreeNode;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        List<Labels> labelsList = labelsService.selectLabelsList(new Labels());
        Map<Long, TreeNode> labelToNodeMap = new HashMap<>();
        List<TreeNode> roots = new ArrayList<>();
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
        for (Labels label : labelsList) {
            TreeNode node = new TreeNode();
            if(set.contains(label.getId())){
                label.setStatus(true);
            }
            node.setData(label);
            labelToNodeMap.put(label.getId(), node);

            if (label.getParentId() == null) {
                roots.add(node);
            }
        }
        for (Labels label : labelsList) {
            TreeNode currentNode = labelToNodeMap.get(label.getId());
            TreeNode parentNode = labelToNodeMap.get(label.getParentId());

            if (parentNode != null) {
                parentNode.getChildren().add(currentNode);
            }
        }

        return AjaxResult.success(roots);
    }

    @PostMapping
    public AjaxResult add(@RequestBody Labels labels){
        return toAjax(labelsService.insertLabels(labels));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Labels labels){
        return toAjax(labelsService.updateLabels(labels));
    }

    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(labelsService.deleteLabelsByIds(ids));
    }

}
