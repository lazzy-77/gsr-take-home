package lance.gsr_take_home.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Slf4j
@Service
public class MessageConsumer {
    @Value("${kafka.topic}")
    private String topic;

    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kraken");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        log.info("CONSUMER TOPIC: {}", topic);
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
    }

    @Async
    public void consumeMessages() {
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10000));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("---- CONSUMER -> CONSUMED RECORD: {} ----", record.value());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            consumer.close();
        }
    }
}

