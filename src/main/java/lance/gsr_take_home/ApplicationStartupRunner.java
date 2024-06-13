package lance.gsr_take_home;

import jakarta.annotation.PostConstruct;
import lance.gsr_take_home.kafka.MessageConsumer;
import lance.gsr_take_home.service.KrakenWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupRunner {

    @Autowired
    private KrakenWebSocketService webSocketService;
    @Autowired
    private MessageConsumer consumerService;

    @PostConstruct
    public void run() {
        webSocketService.connect();
        consumerService.consumeMessages();
    }
}
