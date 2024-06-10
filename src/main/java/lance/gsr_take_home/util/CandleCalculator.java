package lance.gsr_take_home.util;

import lance.gsr_take_home.model.Tick;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CandleCalculator {

    public static double midPrice(List<Tick> asks, List<Tick> bids) {
        var lowestAsk = asks.stream().map(Tick::price).mapToDouble(Double::doubleValue).min();
        var highestBid = bids.stream().map(Tick::price).mapToDouble(Double::doubleValue).max();

        if (highestBid.isEmpty() || lowestAsk.isEmpty() || highestBid.getAsDouble() >= lowestAsk.getAsDouble()) {
            log.error("Highest bid price is higher than lowest ask price, bid: {}, ask: {}", highestBid, lowestAsk);
            throw new IllegalStateException("Highest bid price is higher than lowest ask price");
        }

        log.info("In Entry: highest ask: {}, lowest ask: {}", highestBid, lowestAsk);

        return (highestBid.getAsDouble() + lowestAsk.getAsDouble()) / 2;
    }


}
