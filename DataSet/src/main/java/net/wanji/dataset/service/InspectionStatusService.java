package net.wanji.dataset.service;

import net.wanji.dataset.entity.InspectionStatus;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * inspection_status 服务类
 * </p>
 *
 * @author wj
 * @since 2022-11-09
 */
public interface InspectionStatusService extends IService<InspectionStatus> {
    public void updateInspectionStatus(InspectionStatus inspectionStatus);

}
