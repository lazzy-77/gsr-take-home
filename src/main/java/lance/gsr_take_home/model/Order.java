package lance.gsr_take_home.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Order {
    private double price;
    private double qty;
}

