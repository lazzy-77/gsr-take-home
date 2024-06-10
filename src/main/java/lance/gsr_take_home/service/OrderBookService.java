package lance.gsr_take_home.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lance.gsr_take_home.client.KrakenWebSocketClient;
import lance.gsr_take_home.model.Order;
import lance.gsr_take_home.model.OrderBook;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class OrderBookService {
    private final OrderBook orderBook = new OrderBook();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTextMessage(String message) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(message);
        if (jsonNode.has("type")) {
            String type = jsonNode.get("type").asText();
            if ("snapshot".equals(type)) {
                handleSnapshotMessage(jsonNode);
            } else if ("update".equals(type)) {
                handleUpdateMessage(jsonNode);
            }
        }
    }

    private void handleSnapshotMessage(JsonNode jsonNode) {
        JsonNode data = jsonNode.get("data").get(0);
        List<Order> bids = parseOrders(data.get("bids"));
        List<Order> asks = parseOrders(data.get("asks"));
        orderBook.updateSnapshot(bids, asks);
    }

    private void handleUpdateMessage(JsonNode jsonNode) {
        JsonNode data = jsonNode.get("data").get(0);
        updateOrders("bid", data.get("bids"));
        updateOrders("ask", data.get("asks"));
    }

    private List<Order> parseOrders(JsonNode ordersJson) {
        List<Order> orders = new ArrayList<>();
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orders.add(new Order(price, qty));
        });
        return orders;
    }

    private void updateOrders(String side, JsonNode ordersJson) {
        ordersJson.forEach(orderJson -> {
            double price = orderJson.get("price").asDouble();
            double qty = orderJson.get("qty").asDouble();
            orderBook.updateOrder(side, price, qty);
        });
    }
}
