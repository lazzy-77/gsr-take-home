package lance.gsr_take_home.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class Candle {
        private Instant timestamp;
        private double open;
        private double high;
        private double low;
        private double close;
        private int ticks;
}
