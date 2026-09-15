package org.example.Controller;

import org.example.Service.CognitoAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.util.Map;

/**
 * Server-side authentication using a confidential Cognito app client.
 *
 * Exists alongside the page's direct-to-Cognito calls, not instead of them:
 * the two paths issue interchangeable tokens, so every other endpoint works
 * the same either way.
 *
 * Mapped under /api/public/** because a login endpoint cannot itself require
 * a token. That makes it an unauthenticated, credential-accepting surface -
 * in production it would need rate limiting, which is one of the costs of
 * proxying authentication rather than letting the browser talk to Cognito.
 */
@RestController
@RequestMapping("/api/public/auth")
public class AuthController {

    private final CognitoAuthService auth;

    public AuthController(CognitoAuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return call(() -> auth.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return call(() -> auth.register(body.get("username"), body.get("password")));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody Map<String, String> body) {
        return call(() -> auth.confirm(body.get("username"), body.get("code")));
    }

    private ResponseEntity<Map<String, Object>> call(Supplier action) {
        if (!auth.isConfigured()) {
            return ResponseEntity.status(501).body(Map.of(
                    "error", "confidential client not configured",
                    "hint", "set APP_COGNITO_CONFIDENTIAL_CLIENT_ID and _SECRET"));
        }
        try {
            return ResponseEntity.ok(action.get());
        } catch (CognitoIdentityProviderException e) {
            // Cognito's own message is safe to pass through here; the password
            // is never echoed and must never be logged.
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.awsErrorDetails().errorCode(),
                    "message", e.awsErrorDetails().errorMessage()));
        }
    }

    @FunctionalInterface
    private interface Supplier {
        Map<String, Object> get();
    }
}