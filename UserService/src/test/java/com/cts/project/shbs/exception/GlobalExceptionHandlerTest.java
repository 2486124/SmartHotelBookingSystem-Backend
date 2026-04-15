package com.cts.project.shbs.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // helper — pulls the response body as a Map
    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(ResponseEntity<Object> response) {
        return (Map<String, Object>) response.getBody();
    }

    // ── UserNotFoundException → 404 ───────────────────────────────────────────

    @Nested
    @DisplayName("UserNotFoundException")
    class UserNotFound {

        @Test
        @DisplayName("returns 404 with correct status and message")
        void returns404() {
            UserNotFoundException ex = new UserNotFoundException(1L);
            ResponseEntity<Object> response = handler.handleUserNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response).get("status")).isEqualTo(404);
            assertThat(bodyOf(response).get("error")).isEqualTo("Not Found");
            assertThat(bodyOf(response).get("message")).isEqualTo(ex.getMessage());
        }
    }

    // ── AccountNotFoundException → 404 ────────────────────────────────────────

    @Nested
    @DisplayName("AccountNotFoundException")
    class AccountNotFound {

        @Test
        @DisplayName("returns 404 with correct status and message")
        void returns404() {
            AccountNotFoundException ex = new AccountNotFoundException("alice@example.com");
            ResponseEntity<Object> response = handler.handleAccountNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response).get("status")).isEqualTo(404);
            assertThat(bodyOf(response).get("message")).isEqualTo(ex.getMessage());
        }
    }

    // ── EmailAlreadyExistsException → 409 ────────────────────────────────────

    @Nested
    @DisplayName("EmailAlreadyExistsException")
    class EmailAlreadyExists {

        @Test
        @DisplayName("returns 409 Conflict")
        void returns409() {
            EmailAlreadyExistsException ex = new EmailAlreadyExistsException("alice@example.com");
            ResponseEntity<Object> response = handler.handleEmailExists(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response).get("status")).isEqualTo(409);
            assertThat(bodyOf(response).get("error")).isEqualTo("Conflict");
            assertThat(bodyOf(response).get("message")).isEqualTo(ex.getMessage());
        }
    }

    // ── InvalidOrExpiredTokenException → 401 ─────────────────────────────────

    @Nested
    @DisplayName("InvalidOrExpiredTokenException")
    class InvalidOrExpiredToken {

        @Test
        @DisplayName("returns 401 Unauthorized")
        void returns401() {
            InvalidOrExpiredTokenException ex = new InvalidOrExpiredTokenException();
            ResponseEntity<Object> response = handler.handleInvalidToken(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(bodyOf(response).get("status")).isEqualTo(401);
            assertThat(bodyOf(response).get("error")).isEqualTo("Unauthorized");
        }
    }

    // ── PasswordResetTokenAlreadyUsedException → 410 ─────────────────────────

    @Nested
    @DisplayName("PasswordResetTokenAlreadyUsedException")
    class TokenAlreadyUsed {

        @Test
        @DisplayName("returns 410 Gone")
        void returns410() {
            PasswordResetTokenAlreadyUsedException ex = new PasswordResetTokenAlreadyUsedException();
            ResponseEntity<Object> response = handler.handleTokenAlreadyUsed(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
            assertThat(bodyOf(response).get("status")).isEqualTo(410);
            assertThat(bodyOf(response).get("error")).isEqualTo("Gone");
        }
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────────

    @Nested
    @DisplayName("Generic Exception (fallback)")
    class GenericException {

        @Test
        @DisplayName("returns 500 with generic message")
        void returns500() {
            ResponseEntity<Object> response = handler.handleGenericException(new RuntimeException("boom"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(bodyOf(response).get("status")).isEqualTo(500);
            assertThat(bodyOf(response).get("message")).isEqualTo("An unexpected error occurred.");
        }
    }

    // ── MethodArgumentNotValidException → 400 ────────────────────────────────

    @Nested
    @DisplayName("MethodArgumentNotValidException (validation errors)")
    class ValidationErrors {

        @Test
        @DisplayName("returns 400 with field-level error map")
        void returns400WithFieldErrors() {
            // Mock the binding result with two field errors
            FieldError nameError = new FieldError("registerRequest", "name", "Name is required");
            FieldError emailError = new FieldError("registerRequest", "email", "Email must be valid");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(nameError, emailError));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Object> response = handler.handleValidationErrors(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            Map<String, Object> body = bodyOf(response);
            assertThat(body.get("status")).isEqualTo(400);
            assertThat(body.get("error")).isEqualTo("Validation Failed");

            @SuppressWarnings("unchecked")
            Map<String, String> messages = (Map<String, String>) body.get("messages");
            assertThat(messages).containsEntry("name", "Name is required");
            assertThat(messages).containsEntry("email", "Email must be valid");
        }

        @Test
        @DisplayName("keeps first message when same field has multiple errors")
        void duplicateField_keepsFirst() {
            FieldError first = new FieldError("req", "email", "Email is required");
            FieldError second = new FieldError("req", "email", "Email must be valid");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(first, second));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Object> response = handler.handleValidationErrors(ex);

            @SuppressWarnings("unchecked")
            Map<String, String> messages = (Map<String, String>) bodyOf(response).get("messages");

            // Exactly one entry for "email", and it's the first one
            assertThat(messages.get("email")).isEqualTo("Email is required");
        }

        @Test
        @DisplayName("returns empty messages map when no field errors")
        void noFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Object> response = handler.handleValidationErrors(ex);

            @SuppressWarnings("unchecked")
            Map<String, String> messages = (Map<String, String>) bodyOf(response).get("messages");
            assertThat(messages).isEmpty();
        }
    }
}