package com.domesticas.exception;

import com.domesticas.auth.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─────────────────────────────────────────────────────────
    // BAD REQUEST
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP001 - Maneja BadRequestException correctamente")
    void handleBadRequest_DebeRetornar400() {

        BadRequestException exception =
                new BadRequestException("Correo inválido");

        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(exception);

        assertEquals(400, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().getStatus()
        );

        assertEquals(
                "Correo inválido",
                response.getBody().getMessage()
        );
    }

    // ─────────────────────────────────────────────────────────
    // UNAUTHORIZED
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP002 - Maneja UnauthorizedException correctamente")
    void handleUnauthorized_DebeRetornar401() {

        UnauthorizedException exception =
                new UnauthorizedException("No autorizado");

        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorized(exception);

        assertEquals(401, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                401,
                response.getBody().getStatus()
        );

        assertEquals(
                "No autorizado",
                response.getBody().getMessage()
        );
    }

    // ─────────────────────────────────────────────────────────
    // VALIDATION ERROR
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP003 - Maneja errores de validación correctamente")
    void handleValidation_DebeRetornar400() {

        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError(
                "usuario",
                "email",
                "El email es obligatorio"
        );

        when(bindingResult.getFieldError())
                .thenReturn(fieldError);

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(exception.getBindingResult())
                .thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleValidation(exception);

        assertEquals(400, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().getStatus()
        );

        assertEquals(
                "El email es obligatorio",
                response.getBody().getMessage()
        );
    }

    // ─────────────────────────────────────────────────────────
    // GENERAL EXCEPTION
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP004 - Maneja excepciones generales correctamente")
    void handleGeneral_DebeRetornar500() {

        Exception exception =
                new Exception("Error inesperado");

        ResponseEntity<ErrorResponse> response =
                handler.handleGeneral(exception);

        assertEquals(500, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                500,
                response.getBody().getStatus()
        );

        assertEquals(
                "Ocurrió un error interno en el servidor",
                response.getBody().getMessage()
        );
    }
}