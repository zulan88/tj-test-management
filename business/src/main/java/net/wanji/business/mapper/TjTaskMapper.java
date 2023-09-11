package net.wanji.business.mapper;

import net.wanji.business.domain.vo.TaskReportVo;
import net.wanji.business.entity.TjTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author guowenhao
* @description 针对表【tj_task(测试任务表)】的数据库操作Mapper
* @createDate 2023-08-31 17:39:15
* @Entity net.wanji.business.entity.TjTask
*/
public interface TjTaskMapper extends BaseMapper<TjTask> {

    List<TaskReportVo> getExportList(Integer taskId);
}




