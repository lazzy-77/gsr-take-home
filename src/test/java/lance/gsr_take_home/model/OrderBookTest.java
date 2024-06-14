package lance.gsr_take_home.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    public void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    public void updateSnapshot() {
        List<Order> bids = new ArrayList<>();
        bids.add(new Order(100.0, 1.0));
        bids.add(new Order(99.0, 2.0));

        List<Order> asks = new ArrayList<>();
        asks.add(new Order(101.0, 1.5));
        asks.add(new Order(102.0, 2.5));

        orderBook.updateSnapshot(bids, asks);

        NavigableMap<Double, Double> bidMap = orderBook.getBids();
        NavigableMap<Double, Double> askMap = orderBook.getAsks();

        assertEquals(2, bidMap.size());
        assertEquals(1.0, bidMap.get(100.0));
        assertEquals(2.0, bidMap.get(99.0));

        assertEquals(2, askMap.size());
        assertEquals(1.5, askMap.get(101.0));
        assertEquals(2.5, askMap.get(102.0));
    }

    @Test
    public void updateOrderAddBid() {
        orderBook.updateOrder("bid", 100.0, 1.0);

        NavigableMap<Double, Double> bidMap = orderBook.getBids();
        assertEquals(1, bidMap.size());
        assertEquals(1.0, bidMap.get(100.0));
    }

    @Test
    public void updateOrderAddAsk() {
        orderBook.updateOrder("ask", 101.0, 1.5);

        NavigableMap<Double, Double> askMap = orderBook.getAsks();
        assertEquals(1, askMap.size());
        assertEquals(1.5, askMap.get(101.0));
    }

    @Test
    public void updateOrderRemoveBid() {
        orderBook.updateOrder("bid", 100.0, 1.0);
        orderBook.updateOrder("bid", 100.0, 0.0);

        NavigableMap<Double, Double> bidMap = orderBook.getBids();
        assertTrue(bidMap.isEmpty());
    }

    @Test
    public void updateOrderRemoveAsk() {
        orderBook.updateOrder("ask", 101.0, 1.5);
        orderBook.updateOrder("ask", 101.0, 0.0);

        NavigableMap<Double, Double> askMap = orderBook.getAsks();
        assertTrue(askMap.isEmpty());
    }
}
