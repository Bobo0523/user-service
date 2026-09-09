package org.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Current conditions, written by the ingestion Lambda.
 *
 * This table also defines which cities the product supports: a city exists
 * for the user exactly when the Lambda is fetching it. Scanning is fine here
 * because the table holds one row per supported city, not per user.
 */
@Service
public class CurrentWeatherService {

    private final DynamoDbClient db = DynamoDbClient.create();

    @Value("${app.current-table:weather-current}")
    private String table;

    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, AttributeValue> item :
                db.scan(ScanRequest.builder().tableName(table).build()).items()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("city", s(item.get("city")));
            row.put("temperature", n(item.get("temperature")));
            row.put("humidity", n(item.get("humidity")));
            row.put("precipitation", n(item.get("precipitation")));
            row.put("windSpeed", n(item.get("windSpeed")));
            row.put("observedAt", s(item.get("observedAt")));
            out.add(row);
        }
        out.sort((a, b) -> String.valueOf(a.get("city")).compareTo(String.valueOf(b.get("city"))));
        return out;
    }

    /** True when the ingestion job actually covers this city. */
    public boolean isSupported(String city) {
        return listAll().stream().anyMatch(r -> city.equals(r.get("city")));
    }

    private static String s(AttributeValue v) { return v == null ? null : v.s(); }
    private static Double n(AttributeValue v) {
        return (v == null || v.n() == null) ? null : Double.valueOf(v.n());
    }
}