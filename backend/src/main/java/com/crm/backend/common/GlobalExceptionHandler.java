package com.crm.backend.common;

import com.crm.backend.auth.TooManyLoginAttemptsException;
import com.crm.backend.subscription.billing.BillingProviderException;
import com.crm.backend.subscription.billing.BillingUnavailableException;
import com.crm.backend.subscription.billing.InvalidBillingWebhookException;
import com.crm.backend.subscription.usage.SubscriptionLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidBillingWebhookException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBillingWebhook(
            InvalidBillingWebhookException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Invalid billing webhook"
        ));
    }

    @ExceptionHandler(BillingUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleBillingUnavailable(
            BillingUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 503,
                "error", "Service Unavailable",
                "message", exception.getMessage()
        ));
    }
    @ExceptionHandler(SubscriptionLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionLimitExceeded(
            SubscriptionLimitExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 402,
                "error", "Payment Required",
                "code", "SUBSCRIPTION_LIMIT_REACHED",
                "message", exception.getMessage(),
                "feature", exception.getFeature().name(),
                "used", exception.getUsed(),
                "limit", exception.getLimit(),
                "upgradeRequired", true
        ));
    }

    @ExceptionHandler(BillingProviderException.class)
    public ResponseEntity<Map<String, Object>> handleBillingProviderFailure(
            BillingProviderException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 502,
                "error", "Bad Gateway",
                "message", "Billing provider request failed"
        ));
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyLoginAttempts(TooManyLoginAttemptsException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 429,
                "error", "Too Many Requests",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 404,
                "error", "Not Found",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (first, second) -> first
                ));


        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Validation Failed",
                "message", "Request validation failed",
                "fieldErrors", fieldErrors
        ));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 403,
                "error", "Forbidden",
                "message", "Access denied"
        ));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Request body contains an invalid value"
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Invalid value for parameter: " + exception.getName()
        ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestHeader(
            MissingRequestHeaderException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 400,
                "error", "Bad Request",
                "message", "Missing required request header: "
                        + exception.getHeaderName()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 401,
                "error", "Unauthorized",
                "message", "Invalid email or password"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 500,
                "error", "Internal Server Error",
                "message", "Unexpected server error"
        ));
    }
}
