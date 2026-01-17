package com.minicommerceapi.minicommerce.exception;

import java.time.Instant;
import java.util.List;

public class ApiError {
    private Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldViolation> violations;

    public record FieldViolation(String field, String message) {}

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public List<FieldViolation> getViolations() { return violations; }

    public ApiError status(int status) { this.status = status; return this; }
    public ApiError error(String error) { this.error = error; return this; }
    public ApiError message(String message) { this.message = message; return this; }
    public ApiError path(String path) { this.path = path; return this; }
    public ApiError violations(List<FieldViolation> violations) { this.violations = violations; return this; }
}
