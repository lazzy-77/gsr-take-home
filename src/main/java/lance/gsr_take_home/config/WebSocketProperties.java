package lance.gsr_take_home.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class WebSocketProperties {
    private String url = "wss://ws.kraken.com/v2";
    private String subscriptionMessage = """
                {
                    "method":"subscribe",
                    "params":{
                        "channel":"book","symbol":["ETH/USD"]
                    }
                }
                """;
}
