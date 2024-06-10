package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lance.gsr_take_home.model.Candle;
import lance.gsr_take_home.model.OrderBookEntry;
import lance.gsr_take_home.model.Tick;
import lance.gsr_take_home.util.CandleCalculator;
import lance.gsr_take_home.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static lance.gsr_take_home.util.TimeUtil.convertTimestampToMinuteInEpoch;
import static lance.gsr_take_home.util.TimeUtil.getCurrentTimeAsMinuteInEpoch;

@Service
@Slf4j
public class OrderBookService {
    private final HashMap<Long, OrderBookEntry> orderBook;
    private long currentMinute;
    private Candle currentCandle;
    private final List<Candle> candles;
    private final ObjectMapper objectMapper;

    @Autowired
    private CandleService candleService;

    public OrderBookService() {
        this.objectMapper = new ObjectMapper();
        this.orderBook = new HashMap<>();
        this.currentCandle = new Candle();
        this.candles = new ArrayList<>();
        this.currentMinute = LocalDateTime.now().getMinute();
    }

    public void onReceiveText(String message) throws JsonProcessingException {
        var jsonNode = objectMapper.readTree(message);
        var type = jsonNode.get("type");
        var data = jsonNode.path("data");

        if (data.isEmpty()) return;

        if (type.asText().equals("snapshot")) {
            initialiseOrderBookOnSnapshot(data);
        } else if (type.asText().equals("update")) {
            updateOrderBook(data);
        }
    }

    public void initialiseOrderBookOnSnapshot(JsonNode snapshot) {
        long currentMinute = getCurrentTimeAsMinuteInEpoch();

        var asks = snapshot.get(0).get("asks");
        var bids = snapshot.get(0).get("bids");
        createOrderBookEntry(currentMinute, asks, bids);
    }

    public void updateOrderBook(JsonNode data) {
        var asks = data.get(0).get("asks");
        var bids = data.get(0).get("bids");
        var timestamp = data.get(0).get("timestamp");

        if (Stream.of(asks, bids).allMatch(Objects::isNull) || timestamp == null) return;

        log.info("Data: {}", data);
        createOrderBookEntry(convertTimestampToMinuteInEpoch(timestamp), asks, bids);
    }

    private OrderBookEntry createOrderBookEntry(long timestamp, JsonNode asks, JsonNode bids) {
        log.info("asks: {}, bids: {}, timestamp: {}", asks, bids, timestamp);
        var asksList = objectMapper.convertValue(asks, new TypeReference<List<Tick>>() {});
        var bidsList = objectMapper.convertValue(bids, new TypeReference<List<Tick>>() {});

        var highestBid = bidsList.stream().map(Tick::price).mapToDouble(Double::doubleValue).max().getAsDouble();
        var lowestAsk = asksList.stream().map(Tick::price).mapToDouble(Double::doubleValue).min().getAsDouble();

        log.info("To be added: highest ask: {}, lowest ask: {}", highestBid, lowestAsk);

        orderBook.computeIfAbsent(timestamp,
                k -> new OrderBookEntry(new ArrayList<>(), new ArrayList<>())).getAsks().addAll(asksList);
        orderBook.computeIfAbsent(timestamp,
                k -> new OrderBookEntry(new ArrayList<>(), new ArrayList<>())).getBids().addAll(bidsList);


        var currentMidPrice = CandleCalculator.midPrice(orderBook.get(timestamp));
        log.info("{}: Asks size: {}, Bids size: {}", timestamp, orderBook.get(timestamp).getAsks().size(),
                orderBook.get(timestamp).getBids().size());
        log.info("Current mid price: {}", currentMidPrice);
        return orderBook.get(timestamp);
    }
}
