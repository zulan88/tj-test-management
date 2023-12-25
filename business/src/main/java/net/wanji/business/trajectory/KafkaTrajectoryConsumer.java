package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.common.Constants.ColumnName;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjTask;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjTaskMapper;
import net.wanji.business.service.TjCaseRealRecordService;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.common.common.SimulationTrajectoryDto;
import net.wanji.common.utils.DateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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

    @KafkaListener(id = "singleTrajectory", topics = {"tj_master_fusion_data"}, groupId = "trajectory3")
    public void listen(ConsumerRecord<String, String> record) {
        JSONObject jsonObject = JSONObject.parseObject(record.value());
        Integer taskId = jsonObject.getInteger("taskId");
        Integer caseId = jsonObject.getInteger("caseId");
        String userName = selectUserOfTask(taskId, caseId);
        String key = taskId > 0
                ? ChannelBuilder.buildTaskDataChannel(userName, taskId)
                : ChannelBuilder.buildTestingDataChannel(userName, caseId);
        JSONArray participantTrajectories = jsonObject.getJSONArray("participantTrajectories");
        // 收集数据
        List<SimulationTrajectoryDto> data = participantTrajectories.stream()
                .map(t -> JSONObject.parseObject(t.toString(), SimulationTrajectoryDto.class))
                .collect(Collectors.toList());
        kafkaCollector.collector(key, caseId, data);
        // 发送ws数据
        String duration = DateUtils.secondsToDuration(
                (int) Math.floor((double) (kafkaCollector.getSize(key, caseId)) / 10));
        RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, null, participantTrajectories,
                duration);
        WebSocketManage.sendInfo(key, JSONObject.toJSONString(msg));
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
