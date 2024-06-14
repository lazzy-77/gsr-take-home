package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lance.gsr_take_home.kafka.MessageProducer;
import lance.gsr_take_home.model.Candle;
import lance.gsr_take_home.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderBookServiceTest {
    private OrderBookService orderBookService;
    private MessageProducer producer;

    @BeforeEach
    public void setUp() {
        producer = mock(MessageProducer.class);
        orderBookService = new OrderBookService();
        orderBookService.setProducer(producer);
    }

    @Test
    public void handleSnapshotTextMessage() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        var asks = new TreeMap<>();
        asks.put(3673.55, 0.14890467);
        var bids = new TreeMap<>(Collections.reverseOrder());
        bids.put(3671.60, 0.03455496);

        // when
        orderBookService.handleTextMessage(snapshotMessage);

        // then
        assertEquals(asks.size(), orderBookService.getOrderBookMap().get(symbol).getAsks().size());
        assertEquals(asks, orderBookService.getOrderBookMap().get(symbol).getAsks());
        assertEquals(bids.size(), orderBookService.getOrderBookMap().get(symbol).getBids().size());
        assertEquals(bids, orderBookService.getOrderBookMap().get(symbol).getBids());
    }

    @Test
    public void handleUpdateTextMessage() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3672.09,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        var updateMessage = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3672.09,\"qty\":27.23240554}],\"asks\":[],\"checksum\":1211470590,\"timestamp\":\"2024-06-10T19:53:05.505924Z\"}]}";
        orderBookService.handleTextMessage(snapshotMessage);
        var bids = new TreeMap<>();
        bids.put(3672.09, 27.23240554);

        // when
        orderBookService.handleTextMessage(updateMessage);

        // then
        assertEquals(bids.size(), orderBookService.getOrderBookMap().get(symbol).getBids().size());
        assertEquals(bids, orderBookService.getOrderBookMap().get(symbol).getBids());
    }

    @Test
    public void returnIfDataIsEmptyTextMessage() {
        // given
        var message = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[]} ";

        // when / then
        assertDoesNotThrow(() -> orderBookService.handleTextMessage(message));
    }

    @Test
    public void throwsWhenInvalidJsonTextMessage() {
        // given
        var invalidJson = "{";

        // when / then
        assertThrows(JsonProcessingException.class, () -> orderBookService.handleTextMessage(invalidJson));
    }

    @Test
    public void cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3671.60,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        orderBookService.handleTextMessage(snapshotMessage);

        // when
        orderBookService.cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk(orderBookService.getOrderBookMap().get(symbol));

        // then
        assertEquals(0, orderBookService.getOrderBookMap().get(symbol).getBids().size());
    }

    @Test
    public void shouldNotCleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.59,\"qty\":0.03455496}],\"asks\":[{\"price\":3671.60,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        orderBookService.handleTextMessage(snapshotMessage);
        var entry = new Order(3671.59, 0.03455496);

        // when
        orderBookService.cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk(orderBookService.getOrderBookMap().get(symbol));

        // then
        assertEquals(1, orderBookService.getOrderBookMap().get(symbol).getBids().size());
        assertEquals(entry.getPrice(), orderBookService.getOrderBookMap().get(symbol).getBids().firstEntry().getKey());
        assertEquals(entry.getQty(), orderBookService.getOrderBookMap().get(symbol).getBids().firstEntry().getValue());
    }

    @Test
    public void computeCandleDataFirstMinute() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        var midPrice = 3672.575;
        orderBookService.handleTextMessage(snapshotMessage);
        Instant timestamp = Instant.parse("2024-06-10T19:53:05.505924Z");

        // when
        orderBookService.computeCandleData(timestamp, orderBookService.getOrderBookMap().get(symbol), symbol);

        // then
        Candle candle = orderBookService.getCurrentCandleMap().get(symbol);
        assertNotNull(candle);
        assertEquals(midPrice, candle.getOpen());
        assertEquals(midPrice, candle.getHigh());
        assertEquals(midPrice, candle.getLow());
        assertEquals(midPrice, candle.getClose());
    }

    @Test
    public void computeCandleDataSameMinute() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var updateMessage = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":1211470590,\"timestamp\":\"2024-06-10T19:53:05.505924Z\"}]}";
        var midPrice = 3672.575;
        orderBookService.handleTextMessage(updateMessage);
        Instant timestamp = Instant.parse("2024-06-10T19:53:05.505924Z");
        orderBookService.computeCandleData(timestamp, orderBookService.getOrderBookMap().get(symbol), symbol);

        // when
        Instant sameMinuteTimestamp = Instant.parse("2024-06-10T19:53:25.505924Z");
        orderBookService.computeCandleData(sameMinuteTimestamp, orderBookService.getOrderBookMap().get(symbol), symbol);

        // then
        Candle candle = orderBookService.getCurrentCandleMap().get(symbol);
        assertNotNull(candle);
        assertEquals(midPrice, candle.getOpen());
        assertEquals(midPrice, candle.getHigh());
        assertEquals(midPrice, candle.getLow());
        assertEquals(midPrice, candle.getClose());
        assertEquals(3, candle.getTicks());
    }

    @Test
    public void computeCandleDataNewMinute() throws JsonProcessingException {
        // given
        var symbol = "ETH/USD";
        var updateMessage = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":1211470590,\"timestamp\":\"2024-06-10T19:53:05.505924Z\"}]}";
        orderBookService.handleTextMessage(updateMessage);
        Instant firstMinuteTimestamp = Instant.parse("2024-06-10T19:53:05.505924Z");
        orderBookService.computeCandleData(firstMinuteTimestamp, orderBookService.getOrderBookMap().get(symbol), symbol);

        // when
        Instant newMinuteTimestamp = Instant.parse("2024-06-10T19:54:05.505924Z");
        orderBookService.computeCandleData(newMinuteTimestamp, orderBookService.getOrderBookMap().get(symbol), symbol);

        // then
        verify(producer, times(1)).sendMessage(anyString());
        Candle candle = orderBookService.getCurrentCandleMap().get(symbol);
        assertNotNull(candle);
        assertEquals(3672.575, candle.getOpen());
    }
}
