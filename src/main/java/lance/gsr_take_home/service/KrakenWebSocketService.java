package lance.gsr_take_home.service;

import lance.gsr_take_home.config.WebSocketProperties;
import lance.gsr_take_home.client.KrakenWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.springframework.util.CollectionUtils.isEmpty;

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

    @Async
    public void connect() {
        if (isNullOrEmpty(properties.getUrl()) || isEmpty(properties.getPairs())) {
            throw new IllegalArgumentException("A property(s) is missing, check configuration.");
        }

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
