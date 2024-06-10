package lance.gsr_take_home;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KrakenExampleApp {

    public static void main(String[] args) throws Exception {

        try {
            System.out.println("|=========================================|");
            System.out.println("| KRAKEN.COM JAVA TEST APP                |");
            System.out.println("|=========================================|");
            System.out.println();

            // PUBLIC WEBSOCKET Examples
            if (1 == 1) {
                String publicWebSocketURL = "wss://ws.kraken.com/v2";
                String publicWebSocketSubscriptionMsg = """
                        {
                            "method":"subscribe",
                            "params":{
                            "channel":"book","symbol":["BTC/USD"]
                            }
                        }
                        """;

                // MORE PUBLIC WEBSOCKET EXAMPLES
                /*
                String publicWebSocketSubscriptionMsg = "{ \"event\": \"subscribe\", \"subscription\": { \"interval\": 1440, \"name\": \"ohlc\"}, \"pair\": [ \"XBT/EUR\" ]}";

                String publicWebSocketSubscriptionMsg = "{ \"event\": \"subscribe\", \"subscription\": { \"name\": \"spread\"}, \"pair\": [ \"XBT/EUR\",\"ETH/USD\" ]}";
                */

                OpenAndStreamWebSocketSubscription(publicWebSocketURL, publicWebSocketSubscriptionMsg);
            }

            System.out.println("|=======================================|");
            System.out.println("| END OF PROGRAM - HAVE A GOOD DAY :)   |");
            System.out.println("|=======================================|");
            System.out.println("\n");

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // WebSocket API
    public static void OpenAndStreamWebSocketSubscription(String connectionURL, String webSocketSubscription) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create(connectionURL), new WebSocketClient(latch)).join();
            ws.sendText(webSocketSubscription, true);
            latch.await();

        } catch (Exception e) {
            System.out.println();
            System.out.println("AN EXCEPTION OCCURRED :(");
            System.out.println(e);
        }
    }

    private static class WebSocketClient implements WebSocket.Listener {

        private final CountDownLatch latch;

        public WebSocketClient(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(dtf.format(now) + ": " + webSocket.getSubprotocol());
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(dtf.format(now) + ": " + data);
            return WebSocket.Listener.super.onText(webSocket, data, false);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("ERROR OCCURRED: " + webSocket.toString());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}