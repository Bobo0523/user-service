package org.example.Controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class test {

    @GetMapping("/api/public")
    public ResponseEntity<Map<String, String>> testPublicApi() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Spring Boot on EC2!");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}