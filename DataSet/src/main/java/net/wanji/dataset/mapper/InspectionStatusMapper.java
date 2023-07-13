package net.wanji.dataset.mapper;

import net.wanji.dataset.entity.InspectionStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * inspection_status Mapper 接口
 * </p>
 *
 * @author wj
 * @since 2022-11-09
 */
public interface InspectionStatusMapper extends BaseMapper<InspectionStatus> {
   public void updateInspectionStatus(InspectionStatus inspectionStatus);
}
