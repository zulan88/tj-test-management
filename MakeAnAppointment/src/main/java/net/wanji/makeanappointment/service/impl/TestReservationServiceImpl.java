package net.wanji.makeanappointment.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.common.core.domain.entity.SysDictData;
import net.wanji.makeanappointment.domain.vo.TestTypeVo;
import net.wanji.makeanappointment.mapper.TestReservationMapper;
import net.wanji.makeanappointment.service.TestReservationService;
import net.wanji.system.mapper.SysDictDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName TestReservationServiceImpl
 * @Description
 * @Author liruitao
 * @Date 2023-12-06
 * @Version 1.0
 **/
@Service
public class TestReservationServiceImpl implements TestReservationService {

    @Autowired
    private TestReservationMapper testReservationMapper;

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Override
    public List<TestTypeVo> getTestType() {
        Wrapper<TestTypeVo> queryWrapper = new QueryWrapper<>();
        List<TestTypeVo> testTypeVos = testReservationMapper.selectList(queryWrapper);

        List<SysDictData> sysDictDatas = sysDictDataMapper.selectDictDataByType("test_type");

        for (TestTypeVo testTypeVo : testTypeVos) {
            Optional<SysDictData> result = sysDictDatas.stream().filter(
                    sysDictData -> sysDictData.getDictLabel().equals(testTypeVo.getTestTypeName())).findFirst();
            result.ifPresent(value->{testTypeVo.setDictName(value.getDictValue());});
        }
        return testTypeVos;
    }
}
