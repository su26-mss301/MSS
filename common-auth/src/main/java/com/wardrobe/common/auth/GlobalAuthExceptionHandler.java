package com.wardrobe.common.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class GlobalAuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case AUTH_CONTEXT_MISSING, AUTH_CONTEXT_INVALID -> HttpStatus.UNAUTHORIZED;
            case AUTH_FORBIDDEN_ROLE, AUTH_FORBIDDEN_SCOPE -> HttpStatus.FORBIDDEN;
        };

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", ex.getErrorCode().name());
        body.put("message", ex.getMessage());

        return ResponseEntity.status(status).body(body);
    }
}