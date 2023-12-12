package net.wanji.makeanappointment.service.impl;

import net.wanji.makeanappointment.domain.vo.TestObjectVo;
import net.wanji.makeanappointment.mapper.TestObjectMapper;
import net.wanji.makeanappointment.service.TestObjectService;
import net.wanji.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName TesteeObjectServiceImpl
 * @Description
 * @Author liruitao
 * @Date 2023-12-05
 * @Version 1.0
 **/
@Service("testObjectService")
public class TestObjectServiceImpl implements TestObjectService {

    private static Logger logger = LoggerFactory.getLogger(TestObjectServiceImpl.class);

    @Autowired
    private TestObjectMapper testeeObjectMapper;

    @Override
    public void addTesteeObject(TestObjectVo testeeObjectVo) {
        try {
            testeeObjectVo.setCreateBy(SecurityUtils.getUsername());
        }catch (Exception e){
            logger.error("获取用户名异常", e);
            testeeObjectVo.setCreateBy("admin");
        }
        testeeObjectMapper.addTesteeObject(testeeObjectVo);
    }

    @Override
    public void deleteTesteeObject(Integer id) {
        testeeObjectMapper.deleteTesteeObject(id);
    }

    @Override
    public void updateTesteeObject(TestObjectVo testeeObjectVo) {
        try {
            testeeObjectVo.setUpdateBy(SecurityUtils.getUsername());
        }catch (Exception e){
            logger.error("获取用户名异常", e);
            testeeObjectVo.setUpdateBy("admin");
        }
        testeeObjectMapper.updateTesteeObject(testeeObjectVo);
    }

    @Override
    public TestObjectVo queryTesteeObjectById(Integer id) {
        return testeeObjectMapper.queryTesteeObjectById(id);
    }

    @Override
    public List<TestObjectVo> queryTesteeObjectList(TestObjectVo testeeObjectVo) {
        return testeeObjectMapper.queryTesteeObjectList(testeeObjectVo);
    }

}
