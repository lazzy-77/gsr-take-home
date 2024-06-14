package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Data
@Slf4j
@RequiredArgsConstructor
public class OrderBookService {
    private final Map<String, OrderBook> orderBookMap = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Candle> currentCandleMap = new HashMap<>();
    private final Map<String, Instant> currentMinuteMap = new HashMap<>();

    @Autowired
    private MessageProducer producer;

    public void handleTextMessage(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        var type = jsonNode.get("type");
        var data = jsonNode.path("data");
        var channel = jsonNode.path("channel");

        if (data.isNull() || data.isEmpty() || type.isNull() || !type.isTextual() ||
                !channel.asText().equals("book")) return;

        List<JsonNode> dataList = objectMapper.treeToValue(data, new TypeReference<>() {});
        if (dataList == null || dataList.isEmpty()) return;

        dataList.forEach(dataItem -> {
//            log.info("DataItem: {}", dataItem.toString());
            if (dataItem == null || dataItem.isEmpty()) return;

            var symbol = dataItem.get("symbol").asText();

            if (!orderBookMap.containsKey(symbol)) {
                orderBookMap.put(symbol, new OrderBook());
            }
            var orderBook = orderBookMap.get(symbol);

            if (type.asText().equals("snapshot")) {
                handleSnapshotMessage(dataItem, orderBook);
            } else if (type.asText().equals("update")) {
                handleUpdateMessage(dataItem, orderBook, symbol);
            }
        });

//        orderBookMap.forEach((symbol, orderBook) -> {
//            log.info("OrderBook: {}", symbol);
//            log.info("Bids: {}", orderBook.getBids().toString());
//            log.info("Asks: {}", orderBook.getAsks().toString());
//        });
    }

    void handleSnapshotMessage(JsonNode data, OrderBook orderBook) {
        List<Order> bids = parseOrders(data.get("bids"));
        List<Order> asks = parseOrders(data.get("asks"));
        orderBook.updateSnapshot(bids, asks);
    }

    void handleUpdateMessage(JsonNode data, OrderBook orderBook, String symbol) {
        String timestampStr = data.get("timestamp").asText();
        Instant timestamp = Instant.parse(timestampStr);

        updateOrders("bid", data.get("bids"), orderBook);
        updateOrders("ask", data.get("asks"), orderBook);

        computeCandleData(timestamp, orderBook, symbol);
    }

    void cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk(OrderBook orderBook) {
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

    void computeCandleData(Instant timestamp, OrderBook orderBook, String symbol) {
        //check there's at least one bid AND one ask
        if (orderBook.getBids().isEmpty() || orderBook.getAsks().isEmpty()) {
            return;
        }

        cleanOrderBookIfBidsGreaterThanOrEqualLowestAsk(orderBook);
        // if currentMinute is null (first minute of run) or passed in timestamp is not the same as the currentMinute (minute has passed)
        if (!currentMinuteMap.containsKey(symbol)|| !timestamp.truncatedTo(ChronoUnit.MINUTES).equals(currentMinuteMap.get(symbol))) {
            //if there is a candle log it/process it/etc
            if (currentCandleMap.containsKey(symbol)) {
                log.info("---- Created new candle: {} {} ----", symbol, currentCandleMap.get(symbol));
                producer.sendMessage(currentCandleMap.get(symbol).toString());
            }

            double midPrice = midPrice(orderBook);

            currentMinuteMap.put(symbol, timestamp.truncatedTo(ChronoUnit.MINUTES));
            currentCandleMap.put(symbol, new Candle(symbol, currentMinuteMap.get(symbol), midPrice, midPrice, midPrice, midPrice, 0));
        }
        // if it is still the same minute

        double midPrice = midPrice(orderBook);

        var currentCandle = currentCandleMap.get(symbol);

        currentCandle.setHigh(Math.max(currentCandle.getHigh(), midPrice));
        currentCandle.setLow(Math.min(currentCandle.getLow(), midPrice));
        currentCandle.setClose(midPrice);
        currentCandle.setTicks(currentCandle.getTicks() + 1);
    }

    double midPrice(OrderBook orderBook) {
        double highestBid = orderBook.getBids().firstEntry().getKey();
        double lowestAsk = orderBook.getAsks().firstEntry().getKey();
        return (highestBid + lowestAsk) / 2;
    }

    List<Order> parseOrders(JsonNode ordersJson) {
        List<Order> orders = new ArrayList<>();
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orders.add(new Order(price, qty));
        });
        return orders;
    }

    void updateOrders(String side, JsonNode ordersJson, OrderBook orderBook) {
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orderBook.updateOrder(side, price, qty);
        });
    }
}
