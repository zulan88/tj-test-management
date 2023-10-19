package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.business.domain.dto.CaseTreeDto;
import net.wanji.business.domain.vo.CaseTreeVo;
import net.wanji.business.entity.TjCaseTree;
import net.wanji.business.service.TjCaseTreeService;
import net.wanji.business.mapper.TjCaseTreeMapper;
import net.wanji.common.utils.bean.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author guanyuduo
 * @description 针对表【tj_case_tree(片段式场景表)】的数据库操作Service实现
 * @createDate 2023-10-19 13:22:41
 */
@Service
public class TjCaseTreeServiceImpl extends ServiceImpl<TjCaseTreeMapper, TjCaseTree>
        implements TjCaseTreeService {

    @Autowired
    private TjCaseTreeMapper caseTreeMapper;

    @Override
    public List<CaseTreeVo> selectTree(String type, String name) {
        List<TjCaseTree> caseTrees = caseTreeMapper.selectByCondition(type, name);
        return CollectionUtils.emptyIfNull(caseTrees).stream().map(CaseTreeVo::new).collect(Collectors.toList());
    }

    @Override
    public boolean saveTree(CaseTreeDto caseTreeDto) {
        TjCaseTree caseTree = new TjCaseTree();
        if (ObjectUtils.isEmpty(caseTreeDto.getId())) {
            BeanUtils.copyBeanProp(caseTree, caseTreeDto);
        } else {
            caseTree = this.getById(caseTreeDto.getId());
        }
        return saveOrUpdate(caseTree);
    }
}
