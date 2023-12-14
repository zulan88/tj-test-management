package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjTask;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.service.TjCaseRealRecordService;
import net.wanji.business.service.TjTaskCaseService;
import net.wanji.business.service.TjTaskService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.SimulationTrajectoryDto;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 14:00
 * @Descriptoin:
 */
@Component
public class KafkaTrajectoryConsumer {

    @Resource
    private TjTaskMapper taskMapper;

    @Resource
    private TjCaseMapper caseMapper;

    @Resource
    private TjCaseRealRecordService caseRealRecordService;

    @Resource
    private KafkaCollector kafkaCollector;

    @KafkaListener(id = "singleTrajectory", topics = {"tj_master_fusion_data"}, groupId = "${kafka.groupId}")
    public void listen(ConsumerRecord<?, ?> record) {
        System.out.println("topic = " + record.topic() + ", offset = " + record.offset() + ", value = " + record.value());

        JSONObject jsonObject = JSONObject.parseObject(record.value().toString());
        Integer taskId = jsonObject.getInteger("taskId");
        Integer caseId = jsonObject.getInteger("caseId");
        String userName = selectUserOfTask(taskId, caseId);
        String key = taskId > 0
                ? ChannelBuilder.buildTestingDataChannel(userName, caseId)
                : WebSocketManage.buildKey(userName, String.valueOf(taskId), WebSocketManage.TASK, null);

        kafkaCollector.collector(key, jsonObject.getObject("participantTrajectories", SimulationTrajectoryDto.class));
    }

    private String selectUserOfTask(Integer taskId, Integer caseId) {
        if (0 < taskId) {
            TjTask task = taskMapper.selectById(taskId);
            if (!ObjectUtils.isEmpty(task)) {
                return task.getCreatedBy();
            }
        }
        TjCase tjCase = caseMapper.selectById(caseId);
        if (!ObjectUtils.isEmpty(tjCase)) {
            return tjCase.getCreatedBy();
        }
        return null;
    }

    private Integer getCaseRealRecordId(Integer caseId) {
        List<TjCaseRealRecord> records = caseRealRecordService.list(new QueryWrapper<TjCaseRealRecord>()
                .eq(ColumnName.CASE_ID_COLUMN, caseId).eq(ColumnName.STATUS_COLUMN, TestingStatusEnum.NO_PASS));
        return CollectionUtils.isEmpty(records) ? 0 : records.get(0).getId();
    }
}
