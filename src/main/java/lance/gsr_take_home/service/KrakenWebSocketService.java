package lance.gsr_take_home.service;

import jakarta.annotation.PostConstruct;
import lance.gsr_take_home.config.WebSocketProperties;
import lance.gsr_take_home.client.KrakenWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class KrakenWebSocketService {

    private final HttpClient httpClient;
    private final KrakenWebSocketClient krakenWebSocketClient;
    private final CountDownLatch latch;
    private final WebSocketProperties properties;

    @Autowired
    public KrakenWebSocketService(HttpClient httpClient, KrakenWebSocketClient krakenWebSocketClient,
                                  CountDownLatch latch, WebSocketProperties properties) {
        this.httpClient = httpClient;
        this.krakenWebSocketClient = krakenWebSocketClient;
        this.latch = latch;
        this.properties = properties;
    }

    @PostConstruct
    public void connect() {
        try {
            WebSocket ws = httpClient.newWebSocketBuilder().buildAsync(URI.create(properties.getUrl()), krakenWebSocketClient)
                    .join();
            ws.sendText(properties.getSubscriptionMessage(), true);
            latch.await();
        } catch (Exception e) {
            log.error("An exception occurred: ", e);
        }
    }

}
