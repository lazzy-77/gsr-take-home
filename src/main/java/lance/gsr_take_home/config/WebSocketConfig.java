package lance.gsr_take_home.config;

import lance.gsr_take_home.client.KrakenWebSocketClient;
import lance.gsr_take_home.service.OrderBookService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.concurrent.CountDownLatch;

@Configuration
@AllArgsConstructor
public class WebSocketConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public CountDownLatch latch() {
        return new CountDownLatch(1);
    }

    @Bean
    public KrakenWebSocketClient krakenWebSocketClient(CountDownLatch latch, OrderBookService orderBookService) {
        return new KrakenWebSocketClient(latch, orderBookService);
    }
}
