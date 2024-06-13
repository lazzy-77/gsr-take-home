package lance.gsr_take_home.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class MessageProducer {
    @Value("${kafka.topic}")
    private String topic;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        log.info("PRODUCER TOPIC: {}", topic);
        this.producer = new KafkaProducer<>(props);
    }

    @Async
    public void sendMessage(String value) {
        var res = producer.send(new ProducerRecord<>(topic, value));
        try {
            Thread.sleep(1000);
            log.info("---- PRODUCER -> MESSAGE SENT: {} ----", res.isDone());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        producer.close();
    }
}

