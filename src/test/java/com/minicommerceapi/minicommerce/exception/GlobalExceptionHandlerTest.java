package com.minicommerceapi.minicommerce.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleNotFound_shouldReturn404_whenNotFoundExceptionThrown() {
        NotFoundException ex = new NotFoundException("Resource not found");

        ResponseEntity<ApiError> response = handler.handleNotFound(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Resource not found", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNull(response.getBody().getViolations());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleConflict_shouldReturn409_whenConflictExceptionThrown() {
        ConflictException ex = new ConflictException("Resource already exists");

        ResponseEntity<ApiError> response = handler.handleConflict(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Resource already exists", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNull(response.getBody().getViolations());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleBadRequest_shouldReturn400_whenBadRequestExceptionThrown() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ApiError> response = handler.handleBadRequest(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Invalid input", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNull(response.getBody().getViolations());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleValidation_shouldReturn400WithViolations_whenValidationFails() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        FieldError fieldError1 = new FieldError("user", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("user", "name", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getViolations());
        assertEquals(2, response.getBody().getViolations().size());

        ApiError.FieldViolation violation1 = response.getBody().getViolations().get(0);
        assertEquals("email", violation1.field());
        assertEquals("must be a valid email", violation1.message());

        ApiError.FieldViolation violation2 = response.getBody().getViolations().get(1);
        assertEquals("name", violation2.field());
        assertEquals("must not be blank", violation2.message());

        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleValidation_shouldReturn400WithEmptyViolations_whenNoFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getViolations());
        assertTrue(response.getBody().getViolations().isEmpty());
    }

    @Test
    void handleOther_shouldReturn500_whenUnexpectedExceptionThrown() {
        Exception ex = new RuntimeException("Unexpected error occurred");

        ResponseEntity<ApiError> response = handler.handleOther(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Unexpected error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNull(response.getBody().getViolations());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleOther_shouldReturn500_whenNullPointerExceptionThrown() {
        Exception ex = new NullPointerException("Null reference");

        ResponseEntity<ApiError> response = handler.handleOther(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error", response.getBody().getMessage());
    }

    @Test
    void handleNotFound_shouldUseCorrectPath_whenDifferentURIProvided() {
        when(request.getRequestURI()).thenReturn("/api/products/123");
        NotFoundException ex = new NotFoundException("Product not found");

        ResponseEntity<ApiError> response = handler.handleNotFound(ex, request);

        assertEquals("/api/products/123", response.getBody().getPath());
    }

    @Test
    void handleConflict_shouldHandleEmptyMessage_whenExceptionHasNoMessage() {
        ConflictException ex = new ConflictException("");

        ResponseEntity<ApiError> response = handler.handleConflict(ex, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("", response.getBody().getMessage());
    }
}