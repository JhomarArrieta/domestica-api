package com.domesticas.tarea.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.model.Rol;
import com.domesticas.hogar.repository.HogarRepository;
import com.domesticas.tarea.dto.request.ActualizarTareaRequest;
import com.domesticas.tarea.dto.request.CrearTareaRequest;
import com.domesticas.tarea.model.Tarea;
import com.domesticas.tarea.repository.TareaRepository;
import com.domesticas.usuario.model.Usuario;
import com.domesticas.usuario.repository.UsuarioRepository;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import com.domesticas.tarea.dto.response.TareaCompletadaResponse;
import com.domesticas.tarea.dto.response.TareaResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock private TareaRepository tareaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HogarRepository hogarRepository;
    @Mock private MiembroHogarRepository miembroHogarRepository;

    @InjectMocks
    private TareaService tareaService;

    // ── Objetos reutilizables ────────────────────────────────────────────────
    private Usuario usuario;
    private Hogar hogar;
    private CrearTareaRequest requestValido;

    // Fechas fijas para los tests
    private final LocalDate hoy = LocalDate.now();
    private final LocalDate manana = hoy.plusDays(1);
    private final LocalDate ayer = hoy.minusDays(1);

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).nombre("Juan Test")
                .email("juan@test.com").password("hash").build();

        hogar = Hogar.builder()
                .id(10L).nombre("Hogar Test")
                .codigoAcceso("ABC123").build();

        // Request con todos los campos válidos (base para cada test)
        requestValido = new CrearTareaRequest();
        requestValido.setNombre("Limpiar cocina");
        requestValido.setDescripcion("Limpiar encimeras y suelo");
        requestValido.setFechaInicio(hoy);
        requestValido.setFechaLimite(manana);
        requestValido.setPrioridad("ALTA");
        requestValido.setHogarId(10L);
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP029 — Creación exitosa de tarea
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP029 - Crear tarea con datos válidos la guarda en estado PENDIENTE")
    void crearTarea_ConDatosValidos_DebeGuardarTareaEnEstadoPendiente() {

        when(usuarioRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.of(usuario));
        when(hogarRepository.findById(10L))
                .thenReturn(Optional.of(hogar));

        tareaService.crearTarea("juan@test.com", requestValido);

        // La tarea guardada debe tener estado PENDIENTE y estar asignada al usuario
        verify(tareaRepository, times(1)).save(argThat(t ->
                "PENDIENTE".equals(t.getEstado())
                        && t.getUsuario().equals(usuario)
                        && t.getHogar().equals(hogar)
                        && "Limpiar cocina".equals(t.getNombre())
        ));
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP030 — Campos obligatorios vacíos impiden la creación
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP030a - Nombre vacío lanza BadRequestException")
    void crearTarea_SinNombre_DebeLanzarExcepcion() {

        requestValido.setNombre("");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals("El nombre es obligatorio", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP030b - Nombre null lanza BadRequestException")
    void crearTarea_ConNombreNull_DebeLanzarExcepcion() {

        requestValido.setNombre(null);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals("El nombre es obligatorio", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP030c - Fecha límite null lanza BadRequestException")
    void crearTarea_SinFechaLimite_DebeLanzarExcepcion() {

        requestValido.setFechaLimite(null);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals("La fecha límite es obligatoria", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP030d - Prioridad vacía lanza BadRequestException")
    void crearTarea_SinPrioridad_DebeLanzarExcepcion() {

        requestValido.setPrioridad("");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals("La prioridad es obligatoria", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP030e - Prioridad null lanza BadRequestException")
    void crearTarea_ConPrioridadNull_DebeLanzarExcepcion() {

        requestValido.setPrioridad(null);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals("La prioridad es obligatoria", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP031 — Fecha límite anterior a fecha de inicio
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP031 - Fecha límite anterior a inicio lanza BadRequestException")
    void crearTarea_ConFechaLimiteAnteriorAInicio_DebeLanzarExcepcion() {

        // fechaInicio = mañana, fechaLimite = ayer → inválido
        requestValido.setFechaInicio(manana);
        requestValido.setFechaLimite(ayer);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        assertEquals(
                "La fecha límite no puede ser anterior a la fecha de inicio",
                ex.getMessage()
        );
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP031b - Fecha límite igual a fecha de inicio es válida")
    void crearTarea_ConFechaLimiteIgualAInicio_DebeCrearTarea() {

        // fechaInicio = fechaLimite = hoy → válido
        requestValido.setFechaInicio(hoy);
        requestValido.setFechaLimite(hoy);

        when(usuarioRepository.findByEmail("juan@test.com"))
                .thenReturn(Optional.of(usuario));
        when(hogarRepository.findById(10L))
                .thenReturn(Optional.of(hogar));

        // No debe lanzar ninguna excepción
        assertDoesNotThrow(
                () -> tareaService.crearTarea("juan@test.com", requestValido)
        );

        verify(tareaRepository, times(1)).save(any(Tarea.class));
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP032 — Tarea recurrente (NO PASA — no implementado en el código)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP032 - Tarea recurrente NO implementada: el modelo no tiene campo recurrencia")
    void crearTarea_Recurrente_NoEstaImplementada() {

        // Este test documenta que CP032 FALLA porque Tarea no tiene
        // ningún campo de recurrencia (diaria/semanal/mensual) ni lógica
        // para generar instancias automáticas.
        // Cuando el equipo de desarrollo lo implemente, este test debe
        // actualizarse para verificar el comportamiento esperado.

        // Por ahora solo validamos que el campo no existe en el modelo
        // Verificamos via reflexión que no hay campo "recurrencia" en Tarea
        boolean tieneRecurrencia = java.util.Arrays.stream(
                Tarea.class.getDeclaredFields()
        ).anyMatch(f -> f.getName().equalsIgnoreCase("recurrencia")
                || f.getName().equalsIgnoreCase("frecuencia"));

        assertFalse(tieneRecurrencia,
                "CP032 FALLA: El modelo Tarea no tiene campo de recurrencia implementado");
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP033 — Cambio de estado PENDIENTE → EN_PROCESO
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP033 - Miembro asignado cambia estado de PENDIENTE a EN_PROCESO")
    void cambiarEstado_DePendienteAEnProceso_DebeActualizarEstado() {

        Tarea tareaExistente = Tarea.builder()
                .id(1L)
                .nombre("Limpiar cocina")
                .estado("PENDIENTE")
                .usuario(usuario)
                .hogar(hogar)
                .build();

        when(tareaRepository.findById(1L))
                .thenReturn(Optional.of(tareaExistente));

        tareaService.cambiarEstado(1L, "juan@test.com", "EN_PROCESO");

        // El estado debe haberse actualizado en el objeto
        assertEquals("EN_PROCESO", tareaExistente.getEstado());

        // Y guardado en la BD
        verify(tareaRepository, times(1)).save(argThat(t ->
                "EN_PROCESO".equals(t.getEstado())
        ));
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP034 — Cambio de estado EN_PROCESO → COMPLETADA (PASA PARCIALMENTE)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP034 - Cambio EN_PROCESO a COMPLETADA registra fechaFin")
    void cambiarEstado_DeEnProcesoACompletada_DebeRegistrarFechaFin() {

        Tarea tareaEnProceso = Tarea.builder()
                .id(1L)
                .nombre("Limpiar cocina")
                .estado("EN_PROCESO")
                .usuario(usuario)
                .hogar(hogar)
                .build();

        when(tareaRepository.findById(1L))
                .thenReturn(Optional.of(tareaEnProceso));

        tareaService.cambiarEstado(1L, "juan@test.com", "COMPLETADA");

        // El estado debe ser COMPLETADA
        assertEquals("COMPLETADA", tareaEnProceso.getEstado());

        // La fechaFin debe haberse registrado (no puede ser null)
        assertNotNull(tareaEnProceso.getFechaFin(),
                "La fechaFin debe registrarse al completar la tarea");

        verify(tareaRepository, times(1)).save(argThat(t ->
                "COMPLETADA".equals(t.getEstado()) && t.getFechaFin() != null
        ));

        // NOTA CP034 PASA PARCIALMENTE:
        // La fechaFin se registra correctamente, pero el criterio también
        // pide actualizar el "historial de cumplimiento del usuario".
        // No existe ninguna entidad HistorialTarea en el código actual.
    }

    // ────────────────────────────────────────────────────────────────────────
    // CP035 — Miembro no asignado no puede cambiar el estado
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP035 - Miembro no asignado intenta cambiar estado y recibe error")
    void cambiarEstado_UsuarioNoAsignado_DebeLanzarExcepcion() {

        // La tarea pertenece a "juan@test.com"
        Tarea tareaDeOtro = Tarea.builder()
                .id(1L)
                .nombre("Limpiar cocina")
                .estado("PENDIENTE")
                .usuario(usuario) // dueño: juan@test.com
                .hogar(hogar)
                .build();

        when(tareaRepository.findById(1L))
                .thenReturn(Optional.of(tareaDeOtro));

        // Quien intenta cambiar el estado es "otro@test.com"
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.cambiarEstado(1L, "otro@test.com", "EN_PROCESO")
        );

        assertEquals(
                "Solo el usuario asignado puede cambiar el estado",
                ex.getMessage()
        );

        // El estado no debe cambiar ni guardarse
        assertEquals("PENDIENTE", tareaDeOtro.getEstado());
        verify(tareaRepository, never()).save(any());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Extra — Transición de estado inválida (ej: PENDIENTE → COMPLETADA directo)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Extra - Transición inválida PENDIENTE→COMPLETADA lanza excepción")
    void cambiarEstado_TransicionInvalida_DebeLanzarExcepcion() {

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Limpiar cocina")
                .estado("PENDIENTE").usuario(usuario).hogar(hogar).build();

        when(tareaRepository.findById(1L))
                .thenReturn(Optional.of(tarea));

        // Intentar saltarse EN_PROCESO e ir directo a COMPLETADA
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.cambiarEstado(1L, "juan@test.com", "COMPLETADA")
        );

        assertEquals("Transición de estado inválida", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
// HU17 — Listar tareas con filtros
// ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU17a - Listar todas las tareas del hogar sin filtros")
    void listarTareas_SinFiltros_DebeRetornarTodasLasTareas() {

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Limpiar").estado("PENDIENTE")
                .prioridad("ALTA").usuario(usuario).hogar(hogar)
                .fechaInicio(hoy).fechaLimite(manana).build();

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(true).build()
                ));
        when(tareaRepository.findByHogarId(10L)).thenReturn(List.of(tarea));

        List<TareaResponse> resultado = tareaService.listarTareas(
                10L, null, null, "juan@test.com");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Limpiar", resultado.get(0).getNombre());
    }

    @Test
    @DisplayName("HU17b - Filtrar tareas por estado")
    void listarTareas_ConFiltroEstado_DebeRetornarSoloEseEstado() {

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Limpiar").estado("PENDIENTE")
                .prioridad("ALTA").usuario(usuario).hogar(hogar)
                .fechaInicio(hoy).fechaLimite(manana).build();

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(false).build()
                ));
        when(tareaRepository.findByHogarIdAndEstado(10L, "PENDIENTE"))
                .thenReturn(List.of(tarea));

        List<TareaResponse> resultado = tareaService.listarTareas(
                10L, null, "PENDIENTE", "juan@test.com");

        assertEquals(1, resultado.size());
        assertEquals("PENDIENTE", resultado.get(0).getEstado());
    }

    @Test
    @DisplayName("HU17c - Usuario no miembro no puede listar tareas")
    void listarTareas_UsuarioNoMiembro_DebeLanzarExcepcion() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> tareaService.listarTareas(10L, null, null, "juan@test.com")
        );
    }

// ─────────────────────────────────────────────────────────────────────────
// HU15 — Edición de tareas
// ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU15a - Miembro asignado puede editar su tarea pendiente")
    void actualizarTarea_MiembroAsignado_DebeActualizarCampos() {

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Vieja").estado("PENDIENTE")
                .usuario(usuario).hogar(hogar).build();

        ActualizarTareaRequest request = new ActualizarTareaRequest();
        request.setNombre("Nueva");
        request.setDescripcion("Nueva desc");
        request.setPrioridad("MEDIA");
        request.setFechaInicio(hoy);
        request.setFechaLimite(manana);

        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(false).build()
                ));

        tareaService.actualizarTarea(1L, request, "juan@test.com");

        verify(tareaRepository, times(1)).save(argThat(t ->
                "Nueva".equals(t.getNombre()) && "MEDIA".equals(t.getPrioridad())
        ));
    }

    @Test
    @DisplayName("HU15b - Tarea completada no puede editarse")
    void actualizarTarea_TareaCompletada_DebeLanzarExcepcion() {

        Tarea tareaCompletada = Tarea.builder()
                .id(1L).nombre("Hecha").estado("COMPLETADA")
                .usuario(usuario).hogar(hogar).build();

        ActualizarTareaRequest request = new ActualizarTareaRequest();
        request.setNombre("Intento editar");
        request.setFechaInicio(hoy);
        request.setFechaLimite(manana);

        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tareaCompletada));
        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(false).build()
                ));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.actualizarTarea(1L, request, "juan@test.com")
        );

        assertEquals("Las tareas completadas no pueden editarse", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU15c - Miembro no asignado no puede editar la tarea")
    void actualizarTarea_UsuarioNoAsignado_DebeLanzarExcepcion() {

        Usuario otroUsuario = Usuario.builder()
                .id(99L).nombre("Otro").email("otro@test.com").password("hash").build();

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Tarea").estado("PENDIENTE")
                .usuario(otroUsuario).hogar(hogar).build();

        ActualizarTareaRequest request = new ActualizarTareaRequest();
        request.setNombre("Intento");
        request.setFechaInicio(hoy);
        request.setFechaLimite(manana);

        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Hijo").build())
                                .esAdministrador(false).build()
                ));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.actualizarTarea(1L, request, "juan@test.com")
        );

        assertEquals("No tienes permisos para editar esta tarea", ex.getMessage());
    }

// ─────────────────────────────────────────────────────────────────────────
// HU16 — Eliminación de tareas
// ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU16a - Miembro asignado puede eliminar su tarea")
    void eliminarTarea_MiembroAsignado_DebeEliminarTarea() {

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Limpiar").estado("PENDIENTE")
                .usuario(usuario).hogar(hogar).build();

        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(false).build()
                ));

        tareaService.eliminarTarea(1L, "juan@test.com");

        verify(tareaRepository, times(1)).delete(tarea);
    }

    @Test
    @DisplayName("HU16b - Miembro no asignado no puede eliminar la tarea")
    void eliminarTarea_UsuarioNoAsignado_DebeLanzarExcepcion() {

        Usuario otroUsuario = Usuario.builder()
                .id(99L).nombre("Otro").email("otro@test.com").password("hash").build();

        Tarea tarea = Tarea.builder()
                .id(1L).nombre("Limpiar").estado("PENDIENTE")
                .usuario(otroUsuario).hogar(hogar).build();

        when(tareaRepository.findById(1L)).thenReturn(Optional.of(tarea));
        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Hijo").build())
                                .esAdministrador(false).build()
                ));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tareaService.eliminarTarea(1L, "juan@test.com")
        );

        assertEquals("No tienes permisos para eliminar esta tarea", ex.getMessage());
        verify(tareaRepository, never()).delete(any());
    }

// ─────────────────────────────────────────────────────────────────────────
// HU19 — Tareas completadas
// ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU19a - Obtener tareas completadas del hogar")
    void obtenerTareasCompletadas_ConTareasCompletadas_DebeRetornarListado() {

        Tarea tareaCompletada = Tarea.builder()
                .id(1L).nombre("Limpiar").estado("COMPLETADA")
                .usuario(usuario).hogar(hogar).fechaLimite(manana).build();

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(
                        MiembroHogar.builder().usuario(usuario).hogar(hogar)
                                .rol(Rol.builder().nombre("Padre").build())
                                .esAdministrador(false).build()
                ));
        when(tareaRepository.findByHogarIdAndEstado(10L, "COMPLETADA"))
                .thenReturn(List.of(tareaCompletada));

        List<TareaCompletadaResponse> resultado =
                tareaService.obtenerTareasCompletadas(10L, "juan@test.com");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Limpiar", resultado.get(0).getNombreTarea());
        assertEquals("Juan Test", resultado.get(0).getMiembro());
    }


}