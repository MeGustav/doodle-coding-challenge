package com.megustav.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DatabaseHealthController {

    private final DatabaseHealthService databaseHealthService;

    @GetMapping("/api/health/db")
    public ResponseEntity<Boolean> checkDb() {
        return ResponseEntity.ok(databaseHealthService.isDatabaseHealthy());
    }
}
