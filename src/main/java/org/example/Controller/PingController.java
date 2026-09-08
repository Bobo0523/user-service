package org.example.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Two endpoints used to prove the token pipeline end to end:
 *   /api/public  - no token required, confirms the app is up
 *   /api/me      - token required, echoes the verified claims back
 */
@RestController
public class PingController {

    @GetMapping("/api/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "user-service is running",
                "status", "success"));
    }

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", jwt.getSubject());
        body.put("username", jwt.getClaimAsString("cognito:username"));
        body.put("email", jwt.getClaimAsString("email"));
        body.put("tokenUse", jwt.getClaimAsString("token_use"));
        body.put("issuer", jwt.getIssuer().toString());
        body.put("expiresAt", jwt.getExpiresAt());
        return ResponseEntity.ok(body);
    }
}
