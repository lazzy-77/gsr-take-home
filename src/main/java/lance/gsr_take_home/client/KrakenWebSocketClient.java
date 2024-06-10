package lance.gsr_take_home.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import lance.gsr_take_home.service.DataProcessingService;
import lance.gsr_take_home.service.OrderBookService;
import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class KrakenWebSocketClient implements WebSocket.Listener {

    private final CountDownLatch latch;
    private final DataProcessingService dataProcessingService;
    private final OrderBookService orderBookService;

    public KrakenWebSocketClient(CountDownLatch latch, DataProcessingService dataProcessingService,
                                 OrderBookService orderBookService) {
        this.latch = latch;
        this.dataProcessingService = dataProcessingService;
        this.orderBookService = orderBookService;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        log.info("{}: {}", dtf.format(now), webSocket.getSubprotocol());
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
//        log.info("{}: {}", dtf.format(now), data);

        // Process the data
        try {
            orderBookService.onReceiveText(data.toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return WebSocket.Listener.super.onText(webSocket, data, false);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("ERROR OCCURRED: {}", webSocket.toString());
        WebSocket.Listener.super.onError(webSocket, error);
        latch.countDown();
    }
}
