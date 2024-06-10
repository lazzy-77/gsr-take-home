package lance.gsr_take_home.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TimeUtil {

    public static long getCurrentTimeAsMinuteInEpoch() {
        return convertTimestampToMinuteInEpoch(Instant.now().toString());
    }

    public static long convertTimestampToMinuteInEpoch(JsonNode timestamp) {
        return convertTimestampToMinuteInEpoch(timestamp.asText());
    }

    public static long convertTimestampToMinuteInEpoch(String timestamp) {
        var instant = Instant.parse(timestamp).truncatedTo(ChronoUnit.MINUTES);
        return instant.toEpochMilli();
    }
}
