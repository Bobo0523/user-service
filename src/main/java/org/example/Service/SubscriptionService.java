package org.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-user alert preferences.
 *
 * Every method takes the userId that was extracted from the verified JWT, and
 * uses it as the partition key. A user can only ever touch their own rows -
 * the isolation is enforced here in application code, not by IAM.
 */
@Service
public class SubscriptionService {

    private final DynamoDbClient db = DynamoDbClient.create();

    @Value("${app.subscriptions-table:weather-subscriptions}")
    private String table;

    public List<Map<String, Object>> listForUser(String userId) {
        QueryResponse res = db.query(QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("userId = :uid")
                .expressionAttributeValues(Map.of(
                        ":uid", AttributeValue.builder().s(userId).build()))
                .build());

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, AttributeValue> item : res.items()) {
            Map<String, Object> row = new HashMap<>();
            row.put("city", str(item.get("city")));
            row.put("tempAbove", num(item.get("tempAbove")));
            row.put("createdAt", str(item.get("createdAt")));
            row.put("email", str(item.get("email")));
            out.add(row);
        }
        return out;
    }

    public void save(String userId, String email, String city, Double tempAbove) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("city", AttributeValue.builder().s(city).build());
        if (email != null && !email.isBlank()) {
            item.put("email", AttributeValue.builder().s(email).build());
        }
        if (tempAbove != null) {
            item.put("tempAbove", AttributeValue.builder().n(tempAbove.toString()).build());
        }
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());

        db.putItem(PutItemRequest.builder().tableName(table).item(item).build());
    }

    private static String str(AttributeValue v) { return v == null ? null : v.s(); }
    private static Double num(AttributeValue v) {
        return (v == null || v.n() == null) ? null : Double.valueOf(v.n());
    }
}