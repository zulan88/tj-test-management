package net.wanji.business.trajectory;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 9:33
 * @Descriptoin:
 */
@Component
public class TrajectorySubscribe implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
//        DelayMessage deserialize = serializer.deserialize(message.getBody(), DelayMessage.class);
//        System.out.println(deserialize.getMsgId());
//        System.out.println("接收数据:"+message.toString());
//        System.out.println("订阅频道:"+new String(message.getChannel()));
    }
}
