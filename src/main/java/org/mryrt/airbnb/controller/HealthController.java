package org.mryrt.airbnb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Эндпоинт проверки доступности приложения (для мониторинга и CI/CD).
 */
@RestController
public class HealthController {

    /** Возвращает статус приложения (UP). GET /health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
