package net.wanji.dataset.service.impl;

import net.wanji.dataset.entity.InspectionStatus;
import net.wanji.dataset.mapper.InspectionStatusMapper;
import net.wanji.dataset.service.InspectionStatusService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * inspection_status 服务实现类
 * </p>
 *
 * @author wj
 * @since 2022-11-09
 */
@Service
public class InspectionStatusServiceImpl extends ServiceImpl<InspectionStatusMapper, InspectionStatus> implements InspectionStatusService {
    @Autowired
    InspectionStatusMapper inspectionStatusMapper;
    @Override
    public void updateInspectionStatus(InspectionStatus inspectionStatus) {
        inspectionStatusMapper.updateInspectionStatus(inspectionStatus);
    }
}
