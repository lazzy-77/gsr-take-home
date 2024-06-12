package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lance.gsr_take_home.model.Candle;
import lance.gsr_take_home.model.Order;
import lance.gsr_take_home.model.OrderBook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Getter
@Slf4j
public class OrderBookService {
    private final OrderBook orderBook;
    private final ObjectMapper objectMapper;

    private Candle currentCandle;
    private Instant currentMinute;
    private int ticks = 0;

    public OrderBookService() {
        this.orderBook = new OrderBook();
        this.objectMapper = new ObjectMapper();
    }

    public void handleTextMessage(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        var type = jsonNode.get("type");
        var data = jsonNode.path("data");
        var channel = jsonNode.path("channel");

        if (data.isEmpty() || !channel.asText().equals("book")) return;

        if (type.asText().equals("snapshot")) {
            handleSnapshotMessage(data);
        } else if (type.asText().equals("update")) {
            handleUpdateMessage(data);
        }

    }

    private void handleSnapshotMessage(JsonNode data) {
//        log.info("SNAPSHOT: {}", data.toString());
        List<Order> bids = parseOrders(data.get(0).get("bids"));
        List<Order> asks = parseOrders(data.get(0).get("asks"));
        orderBook.updateSnapshot(bids, asks);
    }

    private void handleUpdateMessage(JsonNode data) {
        String timestampStr = data.get(0).get("timestamp").asText();
        Instant timestamp = Instant.parse(timestampStr);

        updateOrders("bid", data.get(0).get("bids"));
        updateOrders("ask", data.get(0).get("asks"));

        computeCandleData(timestamp);
    }

    private void cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() {
        var highestBidPrice = orderBook.getBids().firstEntry().getKey();
        var lowestAskPrice = orderBook.getAsks().firstEntry().getKey();
        if (highestBidPrice >= lowestAskPrice) {
            orderBook.getBids().keySet().stream().filter(bid -> bid >= lowestAskPrice)
                    .forEach(price -> orderBook.updateOrder("bid", price, 0.0));
        }
    }

    public void computeCandleData(Instant timestamp) {
        //check there's at least one bid AND one ask
        if (orderBook.getBids().isEmpty() || orderBook.getAsks().isEmpty()) {
            return;
        }

        cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk();
        // if currentMinute is null (first minute of run) or passed in timestamp is not the same as the currentMinute (minute has passed)
        if (currentMinute == null || !timestamp.truncatedTo(ChronoUnit.MINUTES).equals(currentMinute)) {
            //if there is a candle log it/process it/etc
            if (currentCandle != null) {
                log.info(currentCandle.toString());
            }

            double midPrice = midPrice(orderBook);

            currentMinute = timestamp.truncatedTo(ChronoUnit.MINUTES);
            currentCandle = new Candle(currentMinute, midPrice, midPrice, midPrice, midPrice, 0);
        }
        // if it is still the same minute

        double midPrice = midPrice(orderBook);

        currentCandle.setHigh(Math.max(currentCandle.getHigh(), midPrice));
        currentCandle.setLow(Math.min(currentCandle.getLow(), midPrice));
        currentCandle.setClose(midPrice);
        currentCandle.setTicks(currentCandle.getTicks() + 1);
    }

    private double midPrice(OrderBook orderBook) {
        double highestBid = orderBook.getBids().firstEntry().getKey();
        double lowestAsk = orderBook.getAsks().firstEntry().getKey();
        return (highestBid + lowestAsk) / 2;
    }

    private List<Order> parseOrders(JsonNode ordersJson) {
        List<Order> orders = new ArrayList<>();
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orders.add(new Order(price, qty));
        });
        return orders;
    }

    private void updateOrders(String side, JsonNode ordersJson) {
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orderBook.updateOrder(side, price, qty);
        });
    }
}
