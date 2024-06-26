package lance.gsr_take_home.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Configuration
@ConfigurationProperties(prefix = "ws")
public class WebSocketProperties {
    private String url;
    private List<String> pairs;
    private String subscriptionMessage;
    private String subscriptionBody = """
                {
                    "method":"subscribe",
                    "params":{
                        "channel":"book","symbol":[%s]
                    }
                }
                """;
    private Integer depth; // Add the depth property

    @PostConstruct
    public void init() {
        String formattedPairs = pairs.stream()
                .map(pair -> "\"" + pair + "\"")
                .collect(Collectors.joining(","));

        if (depth != null) {
            this.subscriptionBody = """
                {
                    "method":"subscribe",
                    "params":{
                        "channel":"book","symbol":[%s], "depth": %d
                    }
                }
                """;
            this.subscriptionMessage = String.format(subscriptionBody, formattedPairs, depth);
        } else {
            this.subscriptionMessage = String.format(subscriptionBody, formattedPairs);
        }
    }
}

