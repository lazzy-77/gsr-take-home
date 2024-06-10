package lance.gsr_take_home.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderBookEntry {
    private List<Tick> asks;
    private List<Tick> bids;
}
