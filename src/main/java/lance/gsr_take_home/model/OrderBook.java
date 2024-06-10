package lance.gsr_take_home.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

@Getter
public class OrderBook {
    private NavigableMap<Double, Double> bids = new TreeMap<>(Collections.reverseOrder());
    private NavigableMap<Double, Double> asks = new TreeMap<>();

    public synchronized void updateSnapshot(List<Order> newBids, List<Order> newAsks) {
        bids.clear();
        asks.clear();
        newBids.forEach(bid -> bids.put(bid.getPrice(), bid.getQty()));
        newAsks.forEach(ask -> asks.put(ask.getPrice(), ask.getQty()));
    }

    public synchronized void updateOrder(String side, double price, double qty) {
        NavigableMap<Double, Double> map = side.equals("bid") ? bids : asks;
        if (qty == 0) {
            map.remove(price);
        } else {
            map.put(price, qty);
        }
    }
}

