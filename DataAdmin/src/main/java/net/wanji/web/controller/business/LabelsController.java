package net.wanji.web.controller.business;

import io.swagger.annotations.Api;
import net.wanji.business.domain.Label;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.domain.vo.SceneDetailVo;
import net.wanji.business.domain.vo.TreeVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.schedule.SceneLabelMap;
import net.wanji.business.service.ILabelsService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.core.controller.BaseController;
import net.wanji.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Api(tags = "场景创建-标签管理")
@RestController
@RequestMapping("/labels")
public class LabelsController extends BaseController {

    @Autowired
    private ILabelsService labelsService;

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;

    @Autowired
    private SceneLabelMap sceneLabelMap;


    //输出为树形结构
    @GetMapping("/list")
    public AjaxResult list(Integer id) throws BusinessException {
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, Label> labelToNodeMap = new HashMap<>();
        TreeVo treeVo = new TreeVo();
        treeVo.setTotal(labelList.size());
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
            if(set.contains(label.getId())){
                label.setStatus(true);
            }
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
        treeVo.setTrees(roots);
        return AjaxResult.success(treeVo);
    }

    @PostMapping("/tree")
    public AjaxResult getlabeltree(@RequestBody List<String> labels) throws BusinessException {
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, Label> labelToNodeMap = new HashMap<>();
        TreeVo treeVo = new TreeVo();
        treeVo.setTotal(labelList.size());
        List<Label> roots = new ArrayList<>();
        Set<Long> set = new HashSet<>();

        for (String str : labels) {
            try {
                long intValue = Long.parseLong(str);
                set.add(intValue);
            } catch (NumberFormatException e) {
                // 处理无效的整数字符串
            }
        }

        for (Label label : labelList) {
            if(set.contains(label.getId())){
                label.setStatus(true);
            }
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
        treeVo.setTrees(roots);
        return AjaxResult.success(treeVo);
    }

    public AjaxResult list2(Integer id) throws BusinessException{
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, Label> labelToNodeMap = new HashMap<>();
        TreeVo treeVo = new TreeVo();
        treeVo.setTotal(labelList.size());
        List<Label> roots = new ArrayList<>();
        Set<Long> set = new HashSet<>();

        if (id != null) {
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
            if (set.contains(label.getId())) {
                label.setStatus(true);
            }
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

        for (Label parentNode : labelToNodeMap.values()) {
            if (parentNode.getParentId() == null) {
                List<Label> leafNodes = new ArrayList<>();
                for (Label child : parentNode.getChildren()) {
                    if (child.getChildren().isEmpty()) {
                        leafNodes.add(child);
                    }
                }
                parentNode.getChildren().removeIf(obj -> obj.getChildren().isEmpty());

                if (leafNodes.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (Label leafNode : leafNodes) {
                        sb.append(leafNode.getName()).append(",");
                    }
                    Label label = new Label();
                    label.setName(sb.substring(0, sb.length() - 1));
                    parentNode.getChildren().add(label);
                }
            }
        }

        treeVo.setTrees(roots);
        return AjaxResult.success(treeVo);
    }

    @PostMapping
    public AjaxResult add(@RequestBody Label label){
        labelsService.insertLabels(label);
        sceneLabelMap.reset(2l);
        return AjaxResult.success(label.getId());
    }

    @PutMapping
    public AjaxResult edit(@RequestBody Label label){
        labelsService.updateLabels(label);
        sceneLabelMap.reset(2l);
        return AjaxResult.success();
    }

    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) throws BusinessException {
        for (Long id:ids){
            SceneDetailVo sceneDetailVo = new SceneDetailVo();
            sceneDetailVo.setLabel(String.valueOf(id));
            List<SceneDetailVo> list = tjFragmentedSceneDetailService.selectTjFragmentedSceneDetailList(sceneDetailVo);
            if(list.size()>0){
                AjaxResult.error("该标签被场景或用例选中，请删除场景和用例后再试");
            }
        }
        return toAjax(labelsService.deleteLabelsByIds(ids));
    }

    @GetMapping("/getlabel")
    public AjaxResult getlabel(Integer id) throws BusinessException {
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long,String> sceneMap = new HashMap<>();
        for(Label tlabel : labelList){
            Long parentId = tlabel.getParentId();
            String prelabel = null;
            if(parentId!=null) {
               prelabel = sceneMap.getOrDefault(parentId, null);
            }else {
                continue;
            }
            if(tlabel.getId().equals(2L)){
                continue;
            }
            if(prelabel==null){
                sceneMap.put(tlabel.getId(),tlabel.getName());
            }else {
                sceneMap.put(tlabel.getId(),prelabel+"-"+tlabel.getName());
            }
        }
        List<String> data = new ArrayList<>();
        if(id!=null) {
            FragmentedScenesDetailVo detailVo = tjFragmentedSceneDetailService.getDetailVo(id);
            List<String> labels = detailVo.getLabelList();
            for (String str : labels) {
                try {
                    long intValue = Long.parseLong(str);
                    data.add(sceneMap.get(intValue));
                } catch (NumberFormatException e) {
                    // 处理无效的整数字符串
                }
            }
        }
        return AjaxResult.success(data);
    }

    @GetMapping("/singlelist")
    public AjaxResult singlelist(){
        List<Label> labelList = labelsService.selectLabelsList(new Label());
        Map<Long, Label> labelToNodeMap = new HashMap<>();
        TreeVo treeVo = new TreeVo();
        treeVo.setTotal(labelList.size());
        List<Label> roots = new ArrayList<>();
        for (Label label : labelList) {

            labelToNodeMap.put(label.getId(), label);

            if (label.getParentId()!=null&&label.getParentId().equals(2l)) {
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
        treeVo.setTrees(roots);
        return AjaxResult.success(treeVo);
    }

}
