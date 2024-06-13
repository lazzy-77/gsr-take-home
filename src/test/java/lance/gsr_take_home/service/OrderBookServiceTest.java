package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class OrderBookServiceTest {
    private OrderBookService orderBookService;

    @BeforeEach
    public void setUp() {
        orderBookService = new OrderBookService();
    }

    @Test
    public void shouldHandleSnapshotTextMessage() throws JsonProcessingException {
        //given
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        var asks = new TreeMap<>();
        asks.put(3673.55, 0.14890467);
        var bids = new TreeMap<>(Collections.reverseOrder());
        bids.put(3671.60, 0.03455496);
        //when
        orderBookService.handleTextMessage(snapshotMessage);
        //then
        assertEquals(asks.size(), orderBookService.getOrderBook().getAsks().size());
        assertEquals(asks, orderBookService.getOrderBook().getAsks());
        assertEquals(bids.size(), orderBookService.getOrderBook().getBids().size());
        assertEquals(bids, orderBookService.getOrderBook().getBids());
    }

    @Test
    public void shouldHandleUpdateTextMessage() throws JsonProcessingException {
        //given
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3672.09,\"qty\":0.03455496}],\"asks\":[{\"price\":3673.55,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        var updateMessage = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3672.09,\"qty\":27.23240554}],\"asks\":[],\"checksum\":1211470590,\"timestamp\":\"2024-06-10T19:53:05.505924Z\"}]}";
        orderBookService.handleTextMessage(snapshotMessage);
        var bids= new TreeMap<>();
        bids.put(3672.09, 27.23240554);
        //when
        orderBookService.handleTextMessage(updateMessage);
        //then
        assertEquals(bids.size(), orderBookService.getOrderBook().getBids().size());
        assertEquals(bids, orderBookService.getOrderBook().getBids());
    }

    @Test
    public void shouldReturnIfDataIsEmptyTextMessage() {
        //given
        var message = "{\"channel\":\"book\",\"type\":\"update\",\"data\":[]} ";
        //when
        //then
        assertDoesNotThrow(() -> orderBookService.handleTextMessage(message));
    }

    @Test
    public void shouldThrowInvalidJsonTextMessage() {
        //given
        var invalidJson = "{";
        //when
        //then
        assertThrows(JsonProcessingException.class, () -> orderBookService.handleTextMessage(invalidJson));
    }

    @Test
    public void shouldCleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() throws JsonProcessingException {
        //given
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.60,\"qty\":0.03455496}],\"asks\":[{\"price\":3671.60,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        orderBookService.handleTextMessage(snapshotMessage);
        //when
        orderBookService.cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk();
        //then
        assertEquals(0, orderBookService.getOrderBook().getBids().size());
    }

    @Test
    public void shouldNotCleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() throws JsonProcessingException {
        //given
        var snapshotMessage = "{\"channel\":\"book\",\"type\":\"snapshot\",\"data\":[{\"symbol\":\"ETH/USD\",\"bids\":[{\"price\":3671.59,\"qty\":0.03455496}],\"asks\":[{\"price\":3671.60,\"qty\":0.14890467}],\"checksum\":3036778067}]}";
        orderBookService.handleTextMessage(snapshotMessage);
        var entry = new ImmutablePair<>(3671.59, 0.03455496);
        //when
        orderBookService.cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk();
        //then
        assertEquals(1, orderBookService.getOrderBook().getBids().size());
        assertEquals(entry ,orderBookService.getOrderBook().getBids().firstEntry());
    }

}
