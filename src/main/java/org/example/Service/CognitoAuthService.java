package org.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognito = CognitoIdentityProviderClient.create();

    @Value("${app.cognito.confidential-client-id:}")
    private String clientId;

    /** Never hard-code this: it arrives from the environment on the instance. */
    @Value("${app.cognito.confidential-client-secret:}")
    private String clientSecret;

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /**
     * Base64( HMAC-SHA256( username + clientId, key = clientSecret ) ).
     * This is the whole difference at the wire level between the two client
     * types, and the single most common reason a confidential-client call
     * fails with "Unable to verify secret hash".
     */
    String secretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(username.getBytes(StandardCharsets.UTF_8));
            byte[] raw = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute SECRET_HASH", e);
        }
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("USERNAME", username);
        params.put("PASSWORD", password);
        params.put("SECRET_HASH", secretHash(username));   // absent for a public client

        InitiateAuthResponse res = cognito.initiateAuth(InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(params)
                .build());

        if (res.authenticationResult() == null) {
            // e.g. NEW_PASSWORD_REQUIRED or MFA. Surfaced rather than swallowed.
            Map<String, Object> pending = new LinkedHashMap<>();
            pending.put("challenge", String.valueOf(res.challengeNameAsString()));
            pending.put("session", res.session());
            return pending;
        }

        AuthenticationResultType auth = res.authenticationResult();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idToken", auth.idToken());
        body.put("accessToken", auth.accessToken());
        body.put("expiresIn", auth.expiresIn());
        // The refresh token stays out of the response on purpose: keeping it
        // server-side is the main security advantage this path has over the
        // browser talking to Cognito directly.
        body.put("clientType", "confidential");
        return body;
    }

    public Map<String, Object> register(String username, String password) {
        SignUpResponse res = cognito.signUp(SignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash(username))
                .username(username)
                .password(password)
                .userAttributes(AttributeType.builder().name("email").value(username).build())
                .build());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userConfirmed", res.userConfirmed());
        body.put("deliveryMedium", res.codeDeliveryDetails() == null
                ? null : res.codeDeliveryDetails().deliveryMediumAsString());
        body.put("destination", res.codeDeliveryDetails() == null
                ? null : res.codeDeliveryDetails().destination());
        return body;
    }

    public Map<String, Object> confirm(String username, String code) {
        cognito.confirmSignUp(ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash(username))
                .username(username)
                .confirmationCode(code)
                .build());
        return Map.of("status", "confirmed");
    }
}
