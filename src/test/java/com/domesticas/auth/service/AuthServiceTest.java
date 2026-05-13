package com.domesticas.auth.service;

import com.domesticas.auth.dto.request.LoginRequest;
import com.domesticas.auth.dto.request.RegisterRequest;
import com.domesticas.auth.dto.response.LoginResponse;
import com.domesticas.exception.BadRequestException;
import com.domesticas.exception.UnauthorizedException;
import com.domesticas.security.JwtService;
import com.domesticas.usuario.dto.response.UsuarioResponse;
import com.domesticas.usuario.model.Usuario;
import com.domesticas.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Usuario usuarioFalso;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequest();
        registerRequest.setNombre("Juan Test");
        registerRequest.setEmail("juan@test.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@test.com");
        loginRequest.setPassword("password123");

        usuarioFalso = Usuario.builder()
                .id(1L)
                .nombre("Juan Test")
                .email("juan@test.com")
                .password("$2a$10$hashFalsoParaElTest")
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // REGISTRO EXITOSO
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP001 - Registro exitoso con datos válidos")
    void registrar_ConDatosValidos_DebeRetornarUsuarioCreado() {

        when(usuarioRepository.existsByEmail("juan@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$hashFalsoParaElTest");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioFalso);

        UsuarioResponse response = authService.registrar(registerRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan Test", response.getNombre());
        assertEquals("juan@test.com", response.getEmail());
        assertEquals("Usuario registrado exitosamente", response.getMensaje());

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertEquals("Juan Test", usuarioGuardado.getNombre());
        assertEquals("juan@test.com", usuarioGuardado.getEmail());

        // Verifica que NO se guarde en texto plano
        assertNotEquals("password123", usuarioGuardado.getPassword());

        // Verifica que sí se guarde el hash esperado
        assertEquals(
                "$2a$10$hashFalsoParaElTest",
                usuarioGuardado.getPassword()
        );

        verify(passwordEncoder, times(1))
                .encode("password123");

        verify(usuarioRepository, times(1))
                .existsByEmail("juan@test.com");

        verifyNoMoreInteractions(jwtService);
    }

    // ─────────────────────────────────────────────────────────
    // CORREO DUPLICADO
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP002 - Registro con correo duplicado lanza excepción")
    void registrar_ConCorreoDuplicado_DebeLanzarBadRequestException() {

        when(usuarioRepository.existsByEmail("juan@test.com"))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.registrar(registerRequest)
        );

        assertEquals(
                "El correo ya está registrado",
                exception.getMessage()
        );

        verify(usuarioRepository, never())
                .save(any());

        verify(passwordEncoder, never())
                .encode(anyString());

        verifyNoInteractions(jwtService);
    }

    // ─────────────────────────────────────────────────────────
    // PASSWORD HASHEADA
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP003 - Password debe almacenarse hasheada")
    void registrar_DebeGuardarPasswordHasheada() {

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$hashFalsoParaElTest");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioFalso);

        authService.registrar(registerRequest);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertNotEquals(
                "password123",
                usuarioGuardado.getPassword()
        );

        assertEquals(
                "$2a$10$hashFalsoParaElTest",
                usuarioGuardado.getPassword()
        );
    }

    // ─────────────────────────────────────────────────────────
    // LOGIN EXITOSO
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP004 - Login exitoso retorna JWT")
    void login_ConCredencialesCorrectas_DebeRetornarTokenJWT() {

        when(usuarioRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.of(usuarioFalso));

        when(passwordEncoder.matches(
                "password123",
                "$2a$10$hashFalsoParaElTest"
        )).thenReturn(true);

        when(jwtService.generateToken("juan@test.com"))
                .thenReturn("token-jwt-generado-123");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);

        assertEquals(1L, response.getId());
        assertEquals("Juan Test", response.getNombre());
        assertEquals("juan@test.com", response.getEmail());
        assertEquals("token-jwt-generado-123", response.getToken());
        assertEquals(
                "Inicio de sesión exitoso",
                response.getMensaje()
        );

        verify(jwtService, times(1))
                .generateToken("juan@test.com");
    }

    // ─────────────────────────────────────────────────────────
    // LOGIN - CORREO NO EXISTE
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP005 - Login con correo inexistente")
    void login_ConCorreoInexistente_DebeLanzarUnauthorizedException() {

        when(usuarioRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(
                "Credenciales inválidas",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(anyString());

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());
    }

    // ─────────────────────────────────────────────────────────
    // LOGIN - PASSWORD INCORRECTA
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP006 - Login con contraseña incorrecta")
    void login_ConPasswordIncorrecta_DebeLanzarUnauthorizedException() {

        when(usuarioRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.of(usuarioFalso));

        when(passwordEncoder.matches(
                "password123",
                "$2a$10$hashFalsoParaElTest"
        )).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(
                "Credenciales inválidas",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(anyString());
    }

    // ─────────────────────────────────────────────────────────
    // SAVE FALLA
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP007 - Error al guardar usuario")
    void registrar_CuandoSaveFalla_DebePropagarExcepcion() {

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("$2a$10$hashFalsoParaElTest");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.registrar(registerRequest)
        );

        assertEquals("DB error", exception.getMessage());
    }

    // ─────────────────────────────────────────────────────────
    // JWT FALLA
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP008 - Error generando JWT")
    void login_CuandoJwtFalla_DebePropagarExcepcion() {

        when(usuarioRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(usuarioFalso));

        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);

        when(jwtService.generateToken(anyString()))
                .thenThrow(new RuntimeException("JWT error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("JWT error", exception.getMessage());
    }

    // ─────────────────────────────────────────────────────────
    // PASSWORD ENCODER FALLA
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP009 - Error al hashear password")
    void registrar_CuandoPasswordEncoderFalla_DebePropagarExcepcion() {

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenThrow(new RuntimeException("Encoder error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.registrar(registerRequest)
        );

        assertEquals("Encoder error", exception.getMessage());

        verify(usuarioRepository, never())
                .save(any());
    }
}