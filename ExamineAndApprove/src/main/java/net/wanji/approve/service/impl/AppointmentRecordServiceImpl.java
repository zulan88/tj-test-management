package net.wanji.approve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.approve.entity.AppointmentRecord;
import net.wanji.approve.entity.TjTesteeObjectInfo;
import net.wanji.approve.entity.TjWorkers;
import net.wanji.approve.entity.dto.AppointmentRecordDto;
import net.wanji.approve.entity.vo.AppointmentRecordVo;
import net.wanji.approve.mapper.AppointmentRecordMapper;
import net.wanji.approve.service.AppointmentRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.approve.service.TjTesteeObjectInfoService;
import net.wanji.approve.service.TjWorkersService;
import net.wanji.business.domain.vo.CasePageVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjCaseService;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 预约记录表 服务实现类
 * </p>
 *
 * @author wj
 * @since 2023-12-05
 */
@Service
public class AppointmentRecordServiceImpl extends ServiceImpl<AppointmentRecordMapper, AppointmentRecord> implements AppointmentRecordService {

    @Autowired
    TjTesteeObjectInfoService tjTesteeObjectInfoService;
    ;

    @Autowired
    TjCaseService tjCaseService;


    @Override
    public List<AppointmentRecord> listByEntity(AppointmentRecordDto appointmentRecord) {
        if (appointmentRecord == null) {
            return this.list();
        } else {
            QueryWrapper<AppointmentRecord> queryWrapper = new QueryWrapper<>();
            if (appointmentRecord.getUnitName() != null && !appointmentRecord.getUnitName().isEmpty()) {
                queryWrapper.eq("unit_name", appointmentRecord.getUnitName());
            }
            if (appointmentRecord.getContactPerson() != null && !appointmentRecord.getContactPerson().isEmpty()) {
                queryWrapper.eq("contact_person", appointmentRecord.getContactPerson());
            }
            if (appointmentRecord.getMeasurandType() != null && !appointmentRecord.getMeasurandType().isEmpty()) {
                queryWrapper.eq("measurand_type", appointmentRecord.getMeasurandType());
            }
            if (appointmentRecord.dateExists()) {
                queryWrapper.between("commit_date", appointmentRecord.getStartDate(), appointmentRecord.getEndDate());
            }
            if (appointmentRecord.getCreateBy() != null && !appointmentRecord.getCreateBy().isEmpty()) {
                queryWrapper.eq("create_by", appointmentRecord.getCreateBy());
            }
            if (appointmentRecord.getStatus()!=null){
                queryWrapper.eq("status", appointmentRecord.getStatus());
            }
            return this.list(queryWrapper);
        }
    }

    @Override
    public AppointmentRecordVo getInfoById(Integer id) throws BusinessException {
        AppointmentRecordVo appointmentRecordVo = new AppointmentRecordVo();
        if (id != null) {
            AppointmentRecord appointmentRecord = this.getById(id);
            BeanUtils.copyBeanProp(appointmentRecordVo, appointmentRecord);
        } else {
            throw new BusinessException("没传ID");
        }
        if (appointmentRecordVo.getMeasurandId() != null) {
            appointmentRecordVo.setTjTesteeObjectInfo(tjTesteeObjectInfoService.getById(appointmentRecordVo.getMeasurandId()));
        } else {
            throw new BusinessException("没传测量对象ID");
        }

        List<Integer> ids = Arrays.stream(appointmentRecordVo.getCaseIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        Long expense = tjCaseService.takeExpense(ids);
        if (expense != null) {
            appointmentRecordVo.setExpense(Math.toIntExact(expense));
        } else {
            appointmentRecordVo.setExpense(0);
        }
        return appointmentRecordVo;
    }

    @Override
    public Long getExpense(Integer id) {
        AppointmentRecord appointmentRecord = this.getById(id);
        if (appointmentRecord == null) {
            return 0L;
        }

        List<Integer> ids = Arrays.stream(appointmentRecord.getCaseIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return tjCaseService.takeExpense(ids);
    }

    @Override
    public List<AppointmentRecord> getByids(List<Integer> ids) {
        QueryWrapper<AppointmentRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        return this.list(queryWrapper);
    }

    @Override
    public List<CasePageVo> pageList(Integer id, Integer treeId) {
        AppointmentRecord appointmentRecord = this.getById(id);
        List<Integer> ids = Arrays.stream(appointmentRecord.getCaseIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        List<CasePageVo> casePageVos = tjCaseService.pageListByIds(ids, treeId);
        return casePageVos;
    }

    @Override
    public AppointmentRecord addApprove(AppointmentRecord appointmentRecord) {
        AppointmentRecord byId = getById(appointmentRecord.getId());

        // 新增
        if (byId == null) {
            String recordId = StringUtils.generateRandomString(12);
            appointmentRecord.setRecordId(recordId);
            save(appointmentRecord);

            QueryWrapper<AppointmentRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("record_id", recordId);
            return getOne(queryWrapper);
        } else {
            // 修改
            updateById(appointmentRecord);
            return getById(appointmentRecord.getRecordId());
        }
    }

    @Override
    public Long getExpenseByCaseIds(String caseIds) {
        List<Integer> ids = Arrays.stream(caseIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        return tjCaseService.takeExpense(ids);
    }


}
