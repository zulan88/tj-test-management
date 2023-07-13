package net.wanji.business.util;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import net.wanji.business.proto.E1FrameProto;
import net.wanji.common.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/5 14:00
 * @Descriptoin:
 */
@Component
public class TrajectoryConsumer {

    private KafkaConsumerConfig kafkaConsumerConfig;

    public TrajectoryConsumer(KafkaConsumerConfig kafkaConsumerConfig) {
        this.kafkaConsumerConfig = kafkaConsumerConfig;
    }

    public List<byte[]> consumeMessages(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerConfig.getServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerConfig.getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConsumerConfig.getAutoCommit());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerConfig.getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerConfig.getValueDeserializer());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
        TopicPartition partition0 = new TopicPartition(topic, 0);
        consumer.assign(Collections.singletonList(partition0));
        long startOffset = 0;
        long endOffset = consumer.endOffsets(Collections.singletonList(partition0)).get(partition0);

        System.out.println(startOffset + "  " + endOffset);
        consumer.seek(partition0, 0);

        List<byte[]> list = new ArrayList<>();
        boolean read = true;
        while (read) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, byte[]> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s, timestamp = %s%n", record.offset(), record.key(),
                        record.value(), System.currentTimeMillis());
                list.add(record.value());
                if (record.offset() == endOffset - 1) {
                    consumer.close();
                    read = false;
                }
            }
        }
        return list;

    }

    public void verifyTrajectory() {

    }

    public void extractingTrajectories() {


    }
}
