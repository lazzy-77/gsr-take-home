package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lance.gsr_take_home.kafka.MessageProducer;
import lance.gsr_take_home.model.Candle;
import lance.gsr_take_home.model.Order;
import lance.gsr_take_home.model.OrderBook;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Data
@Slf4j
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderBook orderBook = new OrderBook();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageProducer producer;

    private Candle currentCandle;
    private Instant currentMinute;
    private int ticks = 0;

    public void handleTextMessage(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        var type = jsonNode.get("type");
        var data = jsonNode.path("data");
        var channel = jsonNode.path("channel");

        if (data.isNull() || data.isEmpty() || type.isNull() || !type.isTextual() ||
                !channel.asText().equals("book")) return;

        if (type.asText().equals("snapshot")) {
            handleSnapshotMessage(data);
        } else if (type.asText().equals("update")) {
            handleUpdateMessage(data);
        }

    }

    private void handleSnapshotMessage(JsonNode data) {
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

    public void cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk() {
        var highestBidPrice = orderBook.getBids().firstEntry().getKey();
        var lowestAskPrice = orderBook.getAsks().firstEntry().getKey();
        if (highestBidPrice >= lowestAskPrice) {
            log.info("---- ERROR: Highest bid price is greater than or equal to lowest ask price ----");
            log.info("---- Highest Bid Price: {}, Lowest ask Price: {} ----", highestBidPrice, lowestAskPrice);
            log.info("---- CLEANING ORDER BOOK ----");
            var bidsToUpdate = orderBook.getBids().keySet().stream().filter(bid -> bid >= lowestAskPrice).toList();
            bidsToUpdate.forEach(price -> orderBook.updateOrder("bid", price, 0.0));
            log.info("---- CLEAN COMPLETE ----");
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
                log.info("---- Created new candle: {} ----", currentCandle);
                producer.sendMessage(currentCandle.toString());
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
