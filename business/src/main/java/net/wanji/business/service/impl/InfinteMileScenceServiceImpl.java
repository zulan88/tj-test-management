package net.wanji.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import net.wanji.business.common.Constants;
import net.wanji.business.domain.InfinteMileScenceExo;
import net.wanji.business.entity.InfinteMileScence;
import net.wanji.business.mapper.InfinteMileScenceMapper;
import net.wanji.business.service.InfinteMileScenceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.wanji.common.utils.CounterUtil;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wj
 * @since 2024-02-23
 */
@Service
public class InfinteMileScenceServiceImpl extends ServiceImpl<InfinteMileScenceMapper, InfinteMileScence> implements InfinteMileScenceService {

    @Override
    public List<InfinteMileScence> selectInfinteMileScenceList() {
        QueryWrapper<InfinteMileScence> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_date");
        List<InfinteMileScence> list = this.list(queryWrapper);
        return list;
    }

    @Override
    public synchronized String buildSceneNumber() {
        return StringUtils.format(Constants.ContentTemplate.INFINTE_NUMBER_TEMPLATE, DateUtils.getNowDayString(),
                CounterUtil.getRandomChar());
    }

    @Override
    public Boolean saveInfinteMileScence(InfinteMileScenceExo infinteMileScence) {
        Gson gson = new Gson();
        if (infinteMileScence.getInElements()!= null && infinteMileScence.getInElements().size() > 0){
            infinteMileScence.setElement(gson.toJson(infinteMileScence.getInElements()));
        }
        if (infinteMileScence.getTrafficFlowObject()!= null){
            infinteMileScence.setTrafficFlow(gson.toJson(infinteMileScence.getTrafficFlowObject()));
        }
        if (infinteMileScence.getSiteSlices()!= null && infinteMileScence.getSiteSlices().size() > 0){
            infinteMileScence.setSiteSlice(gson.toJson(infinteMileScence.getSiteSlices()));
        }
        if (null == infinteMileScence.getId()){
            infinteMileScence.setCreateDate(LocalDateTime.now());
            return this.save(infinteMileScence);
        }else{
            infinteMileScence.setUpdateDate(LocalDateTime.now());
            return this.updateById(infinteMileScence);
        }
    }
}
