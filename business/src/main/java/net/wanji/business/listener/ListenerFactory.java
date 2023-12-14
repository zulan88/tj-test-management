package net.wanji.business.listener;

import net.wanji.common.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author: guanyuduo
 * @date: 2023/12/8 14:15
 * @descriptoin:
 */
@Component
public class ListenerFactory {

    public static final String KAFKA = "kafka";

    public static final String REDIS = "redis";

    @Resource
    private Properties kafkaConsumerProp;

    private static Map<String, Listener> listeners = new HashMap<>();

    public Listener getInstance(String type, String topic) {
        Listener listener = listeners.get(topic);
        if (listener == null) {
            listener = KAFKA.equals(type) ? createKafkaListener(topic) : createRedisListener(topic);
            listeners.put(topic, listener);
        }
        return listener;
    }

    private Listener createKafkaListener(String topic) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaConsumerProp);
        consumer.subscribe(Collections.singletonList(topic));
        KafkaListener listener = new KafkaListener(consumer, topic);

        return listener;
    }

    private Listener createRedisListener(String topic) {
       RedisListener listener = new RedisListener();

        return listener;
    }
}
