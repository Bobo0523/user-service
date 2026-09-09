package org.example.Controller;

import org.example.Service.AlertSubscriptionService;
import org.example.Service.CurrentWeatherService;
import org.example.Service.HistoryService;
import org.example.Service.SubscriptionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WeatherController {

    private final HistoryService history;
    private final SubscriptionService subscriptions;
    private final CurrentWeatherService current;
    private final AlertSubscriptionService alerts;

    public WeatherController(HistoryService history,
                             SubscriptionService subscriptions,
                             CurrentWeatherService current,
                             AlertSubscriptionService alerts) {
        this.history = history;
        this.subscriptions = subscriptions;
        this.current = current;
        this.alerts = alerts;
    }

    /**
     * PUBLIC. Latest reading for every supported city. Doubles as the list the
     * frontend uses to populate its city selector, so the options can never
     * drift from what the ingestion job actually covers.
     */
    @GetMapping("/api/public/current")
    public List<Map<String, Object>> currentWeather() {
        return current.listAll();
    }

    /**
     * PUBLIC. Same data for every visitor, so no token is needed.
     * Sits under /api/public/** which SecurityConfig already permits.
     */
    @GetMapping(value = "/api/public/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getHistory(
            @RequestParam(defaultValue = "NZM00093110") String station) {
        String json = history.getStationHistory(station);
        return json == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(json);
    }

    /** PRIVATE. userId comes from the verified token, never from the request. */
    @GetMapping("/api/subscriptions")
    public List<Map<String, Object>> list(@AuthenticationPrincipal Jwt jwt) {
        return subscriptions.listForUser(jwt.getSubject());
    }

    @PostMapping("/api/subscriptions")
    public ResponseEntity<Map<String, String>> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {

        String city = (String) body.get("city");
        if (city == null || city.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "city is required"));
        }
        // Never trust the client's list: a city with no ingested weather would
        // save fine and then silently never trigger an alert.
        if (!current.isSupported(city)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "unsupported city: " + city));
        }
        Double tempAbove = body.get("tempAbove") == null
                ? null : Double.valueOf(body.get("tempAbove").toString());

        String email = jwt.getClaimAsString("email");
        subscriptions.save(jwt.getSubject(), email, city, tempAbove);

        // Sign the address up for alerts on first save. AWS sends a
        // confirmation email that the user has to click once.
        AlertSubscriptionService.State state = alerts.ensureSubscribed(jwt.getSubject(), email);

        return ResponseEntity.ok(Map.of(
                "status", "saved",
                "alertDelivery", state.name(),
                "message", switch (state) {
                    case PENDING -> "Check " + email + " and confirm the subscription to receive alerts.";
                    case ACTIVE -> "Alerts will be emailed to " + email + ".";
                    case DISABLED -> "Saved, but alert delivery is not configured.";
                }));
    }
}