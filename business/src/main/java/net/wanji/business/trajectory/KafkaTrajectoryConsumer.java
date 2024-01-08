package net.wanji.business.trajectory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Maps;
import net.wanji.business.common.Constants.ChannelBuilder;
import net.wanji.business.common.Constants.RedisMessageType;
import net.wanji.business.common.Constants.TestingStatusEnum;
import net.wanji.business.domain.RealWebsocketMessage;
import net.wanji.business.entity.TjCase;
import net.wanji.business.entity.TjCaseRealRecord;
import net.wanji.business.entity.TjTaskCaseRecord;
import net.wanji.business.listener.KafkaCollector;
import net.wanji.business.mapper.TjCaseMapper;
import net.wanji.business.mapper.TjCaseRealRecordMapper;
import net.wanji.business.mapper.TjTaskCaseRecordMapper;
import net.wanji.business.socket.WebSocketManage;
import net.wanji.business.util.RedisLock;
import net.wanji.common.common.ClientSimulationTrajectoryDto;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger("kafka");

    @Resource
    private TjCaseMapper caseMapper;

    @Resource
    private TjTaskCaseRecordMapper taskCaseRecordMapper;

    @Resource
    private TjCaseRealRecordMapper caseRealRecordMapper;

    @Resource
    private KafkaCollector kafkaCollector;

    @Autowired
    private RedisLock redisLock;

    @KafkaListener(id = "singleTrajectory", topics = {"tj_master_fusion_data"}, groupId = "trajectory4")
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
        List<ClientSimulationTrajectoryDto> data = participantTrajectories.stream()
                .map(t -> JSONObject.parseObject(t.toString(), ClientSimulationTrajectoryDto.class))
                .collect(Collectors.toList());
        outLog(data);
        if(taskId > 0){
            data.forEach(t -> redisLock.renewLock("task_"+t.getSource()));
        } else {
            redisLock.renewLock("case_"+caseId);
        }
        kafkaCollector.collector(key, caseId, data);
        // 发送ws数据
        String duration = DateUtils.secondsToDuration(
                (int) Math.floor((double) (kafkaCollector.getSize(key, caseId)) / 10));
        RealWebsocketMessage msg = new RealWebsocketMessage(RedisMessageType.TRAJECTORY, Maps.newHashMap(), participantTrajectories,
                duration);
        WebSocketManage.sendInfo(key, JSONObject.toJSONString(msg));
    }

    private String selectUserOfTask(Integer taskId, Integer caseId) {
        // todo 可以使用缓存：taskId_caseId -> userName
        String userName = null;
        if (0 < taskId) {
            TjTaskCaseRecord taskCaseRecord = taskCaseRecordMapper.selectOne(new LambdaQueryWrapper<TjTaskCaseRecord>()
                    .eq(TjTaskCaseRecord::getTaskId, taskId)
                    .eq(TjTaskCaseRecord::getCaseId, caseId)
                    .eq(TjTaskCaseRecord::getStatus, TestingStatusEnum.NO_PASS)
                    .isNull(TjTaskCaseRecord::getEndTime));
            if (!ObjectUtils.isEmpty(taskCaseRecord)) {
                userName = taskCaseRecord.getCreatedBy();
            }
        } else {
            TjCaseRealRecord caseRealRecord = caseRealRecordMapper.selectOne(new LambdaQueryWrapper<TjCaseRealRecord>()
                    .eq(TjCaseRealRecord::getCaseId, caseId)
                    .eq(TjCaseRealRecord::getStatus, TestingStatusEnum.NO_PASS)
                    .isNull(TjCaseRealRecord::getEndTime));
            if (!ObjectUtils.isEmpty(caseRealRecord)) {
                userName = caseRealRecord.getCreatedBy();
            }
        }
        TjCase tjCase = caseMapper.selectById(caseId);
        if (!ObjectUtils.isEmpty(tjCase)) {
            return tjCase.getCreatedBy();
        }
        return userName;
    }

    private void outLog(List<ClientSimulationTrajectoryDto> data) {
        StringBuilder sb = new StringBuilder();
        long now = System.currentTimeMillis();
        for (ClientSimulationTrajectoryDto trajectoryDto : data) {
            sb.append(StringUtils.format("{}：{}ms；",trajectoryDto.getSource(), now - Long.parseLong(trajectoryDto.getTimestamp())));
        }
        log.info(sb.toString());
    }
}
