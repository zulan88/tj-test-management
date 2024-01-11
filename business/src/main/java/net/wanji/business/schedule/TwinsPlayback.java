package net.wanji.business.schedule;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import net.wanji.business.service.KafkaProducer;
import net.wanji.common.common.ClientSimulationTrajectoryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TwinsPlayback {

    @Autowired
    KafkaProducer kafkaProducer;

    @Async
    public void sendTwinsPlayback(String topic, List<List<ClientSimulationTrajectoryDto>> trajectories) throws InterruptedException {
        Thread.sleep(1000);
        for(List<ClientSimulationTrajectoryDto> trajectory : trajectories) {
            Gson gson = new Gson();
            JsonArray jsonArray = gson.toJsonTree(trajectory).getAsJsonArray();
            kafkaProducer.sendMessage(topic, jsonArray.getAsString());
            Thread.sleep(99);
        }
    }

}
