package lance.gsr_take_home.util;

import lance.gsr_take_home.model.OrderBookEntry;
import lance.gsr_take_home.model.Tick;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CandleCalculator {

    public static double midPrice(OrderBookEntry entry) {
        var highestBid = entry.getBids().stream().map(Tick::price).mapToDouble(Double::doubleValue).max();
        var lowestAsk = entry.getAsks().stream().map(Tick::price).mapToDouble(Double::doubleValue).min();

        if (highestBid.isEmpty() || lowestAsk.isEmpty() || highestBid.getAsDouble() >= lowestAsk.getAsDouble()) {
            throw new IllegalStateException("Highest bid price is higher than lowest ask price");
        }

        log.info("In Entry: highest ask: {}, lowest ask: {}", highestBid, lowestAsk);

        return (highestBid.getAsDouble() + lowestAsk.getAsDouble()) / 2;
    }


}
