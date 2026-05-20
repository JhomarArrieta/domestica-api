package com.domesticas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        // Inyectamos valores manualmente
        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "mi_clave_super_secreta_para_tests_123456789"
        );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                1000L * 60 * 60 // 1 hora
        );
    }

    @Test
    @DisplayName("CP001 - Generar token correctamente")
    void generateToken_DebeGenerarTokenValido() {

        String token = jwtService.generateToken("juan@test.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("CP002 - Extraer username del token")
    void extractUsername_DebeRetornarEmailCorrecto() {

        String token = jwtService.generateToken("juan@test.com");

        String username = jwtService.extractUsername(token);

        assertEquals("juan@test.com", username);
    }

    @Test
    @DisplayName("CP003 - Token válido con email correcto")
    void isTokenValid_ConEmailCorrecto_DebeRetornarTrue() {

        String token = jwtService.generateToken("juan@test.com");

        boolean valido = jwtService.isTokenValid(
                token,
                "juan@test.com"
        );

        assertTrue(valido);
    }

    @Test
    @DisplayName("CP004 - Token inválido con email incorrecto")
    void isTokenValid_ConEmailIncorrecto_DebeRetornarFalse() {

        String token = jwtService.generateToken("juan@test.com");

        boolean valido = jwtService.isTokenValid(
                token,
                "otro@test.com"
        );

        assertFalse(valido);
    }

    @Test
    @DisplayName("CP005 - Token expirado lanza ExpiredJwtException")
    void isTokenValid_ConTokenExpirado_DebeLanzarExpiredJwtException() {

        JwtService jwtExpirado = new JwtService();

        ReflectionTestUtils.setField(
                jwtExpirado,
                "secretKey",
                "mi_clave_super_secreta_para_tests_123456789"
        );

        // Expiración negativa: el token nace ya expirado
        ReflectionTestUtils.setField(
                jwtExpirado,
                "jwtExpiration",
                -1000L
        );

        String token = jwtExpirado.generateToken("juan@test.com");
        assertThrows(
                io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtExpirado.isTokenValid(token, "juan@test.com"),
                "Debe lanzar ExpiredJwtException con un token expirado"
        );
    }
}