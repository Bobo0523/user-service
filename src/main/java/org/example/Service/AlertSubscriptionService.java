package org.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import java.util.Map;


@Service
public class AlertSubscriptionService {

    private final SnsClient sns = SnsClient.create();

    @Value("${app.alert-topic-arn:}")
    private String topicArn;

    public enum State { ACTIVE, PENDING, DISABLED }

    /** Idempotent: calling it again for an already-subscribed email is a no-op. */
    public State ensureSubscribed(String userId, String email) {
        if (topicArn == null || topicArn.isBlank() || email == null || email.isBlank()) {
            return State.DISABLED;
        }

        State existing = findExisting(email);
        if (existing != null) {
            return existing;
        }

        SubscribeResponse res = sns.subscribe(SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .attributes(Map.of(
                        // Only messages published with this userId attribute
                        // will be delivered to this address.
                        "FilterPolicy", "{\"userId\":[\"" + userId + "\"]}"))
                .returnSubscriptionArn(true)
                .build());

        return "PendingConfirmation".equals(res.subscriptionArn()) ? State.PENDING : State.ACTIVE;
    }

    private State findExisting(String email) {
        String nextToken = null;
        do {
            ListSubscriptionsByTopicResponse page = sns.listSubscriptionsByTopic(
                    ListSubscriptionsByTopicRequest.builder()
                            .topicArn(topicArn).nextToken(nextToken).build());

            for (Subscription sub : page.subscriptions()) {
                if (email.equalsIgnoreCase(sub.endpoint())) {
                    return "PendingConfirmation".equals(sub.subscriptionArn())
                            ? State.PENDING : State.ACTIVE;
                }
            }
            nextToken = page.nextToken();
        } while (nextToken != null);
        return null;
    }
}
