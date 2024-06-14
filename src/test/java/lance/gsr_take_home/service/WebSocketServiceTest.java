package lance.gsr_take_home.service;

import lance.gsr_take_home.client.KrakenWebSocketClient;
import lance.gsr_take_home.config.WebSocketProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

public class WebSocketServiceTest {
    private KrakenWebSocketService webSocketService;
    HttpClient httpClient;
    KrakenWebSocketClient webSocketClient;
    CountDownLatch countDownLatch;
    WebSocketProperties properties;

    @BeforeEach
    public void setUp() {
        httpClient = mock(HttpClient.class);
        webSocketClient = mock(KrakenWebSocketClient.class);
        properties = new WebSocketProperties();
        countDownLatch = new CountDownLatch(1);
        webSocketService = new KrakenWebSocketService(httpClient, webSocketClient, countDownLatch, properties);
    }

    //throws when no pair
    @Test
    public void throwsWhenNoPairs() {
        properties.setUrl("some.url");
        assertThrows(IllegalArgumentException.class, () -> webSocketService.connect());
    }

    //throws when no url
    @Test
    public void throwsWhenNoUrl() {
        properties.setPairs(List.of("ETH/USD"));
        assertThrows(IllegalArgumentException.class, () -> webSocketService.connect());
    }
}
