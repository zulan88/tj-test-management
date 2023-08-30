package net.wanji.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.wanji.business.domain.bo.CaseInfoBo;
import net.wanji.business.domain.dto.TjCaseDto;
import net.wanji.business.domain.vo.CaseVo;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjFragmentedSceneDetail;
import net.wanji.business.entity.TjFragmentedScenes;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wj
 * @since 2023-06-29
 */
public interface TjCaseMapper extends BaseMapper<TjCase> {

    List<CaseVo> selectCases(TjCaseDto tjCaseDto);

    int updateCaseStatus(@Param("ids") List<Integer> ids, @Param("status") String status);

    int selectCountBySceneDetailIds(@Param("sceneDetailIds") List<Integer> sceneDetailIds);

    int selectCountBySceneIds(@Param("sceneIds") List<Integer> sceneIds);

    List<TjFragmentedScenes> selectSceneIdInCase(@Param("testType") String testType,
                                                 @Param("type") String type);

    List<TjFragmentedSceneDetail> selectSubscenesInCase(@Param("testType") String testType,
                                                        @Param("fragmentedSceneId") Integer fragmentedSceneId);

    CaseInfoBo selectCaseInfo(Integer caseId);
}
