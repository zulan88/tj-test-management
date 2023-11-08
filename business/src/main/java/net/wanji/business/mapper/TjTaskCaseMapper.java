package net.wanji.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.wanji.business.domain.bo.TaskCaseInfoBo;
import net.wanji.business.domain.vo.TaskCaseVo;
import net.wanji.business.entity.TjTaskCase;

import java.util.List;

/**
* @author guowenhao
* @description 针对表【tj_task_case(测试任务-用例详情表)】的数据库操作Mapper
* @createDate 2023-08-31 17:39:16
* @Entity net.wanji.business.entity.TjTaskCase
*/
public interface TjTaskCaseMapper extends BaseMapper<TjTaskCase> {

    public List<TaskCaseVo> selectByCondition(TjTaskCase taskCase);

    TaskCaseInfoBo selectTaskCaseInfo(Integer taskCaseId);
}




