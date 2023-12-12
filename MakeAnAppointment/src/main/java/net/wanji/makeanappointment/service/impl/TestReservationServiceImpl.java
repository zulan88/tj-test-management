package net.wanji.makeanappointment.service.impl;

import net.wanji.makeanappointment.domain.vo.TestTypeVo;
import net.wanji.makeanappointment.mapper.TestReservationMapper;
import net.wanji.makeanappointment.service.TestReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public List<TestTypeVo> getTestType() {
        return null;
    }
}
