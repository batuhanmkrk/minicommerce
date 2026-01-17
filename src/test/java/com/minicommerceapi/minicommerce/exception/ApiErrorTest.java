package com.minicommerceapi.minicommerce.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorTest {

    @Test
    void constructor_shouldInitializeTimestamp() {
        ApiError apiError = new ApiError();

        assertNotNull(apiError.getTimestamp());
        assertTrue(apiError.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(apiError.getTimestamp().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    void status_shouldSetAndReturnSelf() {
        ApiError apiError = new ApiError();

        ApiError result = apiError.status(404);

        assertSame(apiError, result);
        assertEquals(404, apiError.getStatus());
    }

    @Test
    void error_shouldSetAndReturnSelf() {
        ApiError apiError = new ApiError();

        ApiError result = apiError.error("Not Found");

        assertSame(apiError, result);
        assertEquals("Not Found", apiError.getError());
    }

    @Test
    void message_shouldSetAndReturnSelf() {
        ApiError apiError = new ApiError();

        ApiError result = apiError.message("Resource not found");

        assertSame(apiError, result);
        assertEquals("Resource not found", apiError.getMessage());
    }

    @Test
    void path_shouldSetAndReturnSelf() {
        ApiError apiError = new ApiError();

        ApiError result = apiError.path("/api/test");

        assertSame(apiError, result);
        assertEquals("/api/test", apiError.getPath());
    }

    @Test
    void violations_shouldSetAndReturnSelf() {
        ApiError apiError = new ApiError();
        List<ApiError.FieldViolation> violations = List.of(
                new ApiError.FieldViolation("email", "must be valid")
        );

        ApiError result = apiError.violations(violations);

        assertSame(apiError, result);
        assertEquals(violations, apiError.getViolations());
    }

    @Test
    void builderPattern_shouldAllowMethodChaining() {
        List<ApiError.FieldViolation> violations = List.of(
                new ApiError.FieldViolation("name", "must not be blank"),
                new ApiError.FieldViolation("email", "invalid format")
        );

        ApiError apiError = new ApiError()
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path("/api/users")
                .violations(violations);

        assertEquals(400, apiError.getStatus());
        assertEquals("Bad Request", apiError.getError());
        assertEquals("Validation failed", apiError.getMessage());
        assertEquals("/api/users", apiError.getPath());
        assertEquals(violations, apiError.getViolations());
        assertNotNull(apiError.getTimestamp());
    }

    @Test
    void builderPattern_shouldHandleNullViolations() {
        ApiError apiError = new ApiError()
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/products/123")
                .violations(null);

        assertEquals(404, apiError.getStatus());
        assertEquals("Not Found", apiError.getError());
        assertEquals("Resource not found", apiError.getMessage());
        assertEquals("/api/products/123", apiError.getPath());
        assertNull(apiError.getViolations());
    }

    @Test
    void getStatus_shouldReturnZero_whenNotSet() {
        ApiError apiError = new ApiError();

        assertEquals(0, apiError.getStatus());
    }

    @Test
    void getError_shouldReturnNull_whenNotSet() {
        ApiError apiError = new ApiError();

        assertNull(apiError.getError());
    }

    @Test
    void getMessage_shouldReturnNull_whenNotSet() {
        ApiError apiError = new ApiError();

        assertNull(apiError.getMessage());
    }

    @Test
    void getPath_shouldReturnNull_whenNotSet() {
        ApiError apiError = new ApiError();

        assertNull(apiError.getPath());
    }

    @Test
    void getViolations_shouldReturnNull_whenNotSet() {
        ApiError apiError = new ApiError();

        assertNull(apiError.getViolations());
    }

    @Test
    void fieldViolation_shouldStoreFieldAndMessage() {
        ApiError.FieldViolation violation = new ApiError.FieldViolation("email", "must be valid");

        assertEquals("email", violation.field());
        assertEquals("must be valid", violation.message());
    }

    @Test
    void fieldViolation_shouldHandleNullValues() {
        ApiError.FieldViolation violation = new ApiError.FieldViolation(null, null);

        assertNull(violation.field());
        assertNull(violation.message());
    }

    @Test
    void fieldViolation_shouldBeImmutable() {
        ApiError.FieldViolation violation = new ApiError.FieldViolation("username", "too short");

        // Records are immutable, verify getters work correctly
        assertEquals("username", violation.field());
        assertEquals("too short", violation.message());
    }

    @Test
    void multipleViolations_shouldBeStoredInList() {
        List<ApiError.FieldViolation> violations = List.of(
                new ApiError.FieldViolation("name", "required"),
                new ApiError.FieldViolation("email", "invalid"),
                new ApiError.FieldViolation("age", "must be positive")
        );

        ApiError apiError = new ApiError().violations(violations);

        assertEquals(3, apiError.getViolations().size());
        assertEquals("name", apiError.getViolations().get(0).field());
        assertEquals("required", apiError.getViolations().get(0).message());
        assertEquals("email", apiError.getViolations().get(1).field());
        assertEquals("invalid", apiError.getViolations().get(1).message());
        assertEquals("age", apiError.getViolations().get(2).field());
        assertEquals("must be positive", apiError.getViolations().get(2).message());
    }

    @Test
    void emptyViolationsList_shouldBeHandled() {
        List<ApiError.FieldViolation> emptyViolations = List.of();

        ApiError apiError = new ApiError().violations(emptyViolations);

        assertNotNull(apiError.getViolations());
        assertTrue(apiError.getViolations().isEmpty());
    }

    @Test
    void timestamp_shouldBeSetOnCreation() throws InterruptedException {
        Instant before = Instant.now();
        Thread.sleep(1); // Ensure some time passes

        ApiError apiError = new ApiError();

        Thread.sleep(1); // Ensure some time passes
        Instant after = Instant.now();

        assertNotNull(apiError.getTimestamp());
        assertFalse(apiError.getTimestamp().isBefore(before));
        assertFalse(apiError.getTimestamp().isAfter(after));
    }

    @Test
    void completeErrorResponse_shouldHaveAllFields() {
        List<ApiError.FieldViolation> violations = List.of(
                new ApiError.FieldViolation("field1", "error1")
        );

        ApiError apiError = new ApiError()
                .status(422)
                .error("Unprocessable Entity")
                .message("Request validation failed")
                .path("/api/orders")
                .violations(violations);

        // Verify all fields are set
        assertNotNull(apiError.getTimestamp());
        assertEquals(422, apiError.getStatus());
        assertEquals("Unprocessable Entity", apiError.getError());
        assertEquals("Request validation failed", apiError.getMessage());
        assertEquals("/api/orders", apiError.getPath());
        assertNotNull(apiError.getViolations());
        assertEquals(1, apiError.getViolations().size());
    }
}