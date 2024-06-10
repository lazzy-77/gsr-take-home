package lance.gsr_take_home.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Candle {
    private long timestamp;
    private double open;
    private double close;
    private double high;
    private double low;
    private int ticks;
}
