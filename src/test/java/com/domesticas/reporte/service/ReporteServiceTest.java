package com.domesticas.reporte.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.model.Rol;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import com.domesticas.reporte.dto.DistribucionResponsabilidadResponse;
import com.domesticas.reporte.dto.HistorialCumplimientoResponse;
import com.domesticas.reporte.dto.ReporteUsuarioResponse;
import com.domesticas.tarea.model.Tarea;
import com.domesticas.tarea.repository.TareaRepository;
import com.domesticas.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private TareaRepository tareaRepository;
    @Mock private MiembroHogarRepository miembroHogarRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Usuario usuario1;
    private Usuario usuario2;
    private Hogar hogar;
    private MiembroHogar miembro1;
    private Tarea tareaPendiente;
    private Tarea tareaEnProceso;
    private Tarea tareaCompletada;

    private final LocalDate HOY    = LocalDate.now();
    private final LocalDate AYER   = HOY.minusDays(1);
    private final LocalDate MANANA = HOY.plusDays(1);

    @BeforeEach
    void setUp() {
        usuario1 = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@test.com").password("hash").build();

        usuario2 = Usuario.builder()
                .id(2L).nombre("Maria").email("maria@test.com").password("hash").build();

        hogar = Hogar.builder()
                .id(10L).nombre("Hogar Test").codigoAcceso("ABC123").build();

        Rol rol = Rol.builder().id(1L).nombre("Padre").build();

        miembro1 = MiembroHogar.builder()
                .id(1L).usuario(usuario1).hogar(hogar)
                .rol(rol).esAdministrador(true).build();

        tareaPendiente = Tarea.builder()
                .id(1L).nombre("Tarea 1").estado("PENDIENTE")
                .usuario(usuario1).hogar(hogar)
                .fechaInicio(HOY).fechaLimite(MANANA).build();

        tareaEnProceso = Tarea.builder()
                .id(2L).nombre("Tarea 2").estado("EN_PROCESO")
                .usuario(usuario1).hogar(hogar)
                .fechaInicio(HOY).fechaLimite(MANANA).build();

        tareaCompletada = Tarea.builder()
                .id(3L).nombre("Tarea 3").estado("COMPLETADA")
                .usuario(usuario2).hogar(hogar)
                .fechaInicio(HOY).fechaLimite(MANANA).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HU20 — Reporte de tareas por usuario
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU20a - Reporte sin filtro de fecha muestra tareas de todos los miembros")
    void reporteTareasPorUsuario_SinFiltroFecha_DebeRetornarReportePorUsuario() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaEnProceso, tareaCompletada));

        List<ReporteUsuarioResponse> reporte = reporteService.reporteTareasPorUsuario(
                10L, null, null, "juan@test.com"
        );

        assertNotNull(reporte);
        // Hay 2 usuarios con tareas (usuario1 tiene 2, usuario2 tiene 1)
        assertEquals(2, reporte.size());

        ReporteUsuarioResponse reporteJuan = reporte.stream()
                .filter(r -> r.getUsuario().equals("Juan"))
                .findFirst().orElseThrow();

        assertEquals(2L, reporteJuan.getAsignadas());
        assertEquals(1L, reporteJuan.getEnProceso());
        assertEquals(0L, reporteJuan.getCompletadas());
    }

    @Test
    @DisplayName("HU20b - Reporte con filtro de fecha filtra correctamente")
    void reporteTareasPorUsuario_ConFiltroFecha_FiltraCorrectamente() {

        // Tarea fuera del rango de fechas
        Tarea tareaFuera = Tarea.builder()
                .id(4L).nombre("Tarea fuera").estado("PENDIENTE")
                .usuario(usuario1).hogar(hogar)
                .fechaInicio(HOY.minusDays(10)).fechaLimite(MANANA).build();

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaFuera));

        // Filtro: solo HOY
        List<ReporteUsuarioResponse> reporte = reporteService.reporteTareasPorUsuario(
                10L, HOY, HOY, "juan@test.com"
        );

        assertNotNull(reporte);
        // Solo la tareaPendiente tiene fechaInicio = HOY
        assertEquals(1, reporte.size());
        assertEquals(1L, reporte.get(0).getAsignadas());
    }

    @Test
    @DisplayName("HU20c - Usuario no miembro del hogar lanza BadRequestException")
    void reporteTareasPorUsuario_UsuarioNoMiembro_DebeLanzarExcepcion() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "extraño@test.com"))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reporteService.reporteTareasPorUsuario(
                        10L, null, null, "extraño@test.com")
        );

        assertEquals("No perteneces a este hogar", ex.getMessage());
        verify(tareaRepository, never()).findByHogarId(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HU21 — Distribución de responsabilidades
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU21a - Distribución calcula porcentajes correctamente")
    void distribucionResponsabilidades_ConTareas_DebeCalcularPorcentajes() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        // usuario1 tiene 2 tareas, usuario2 tiene 1 → total 3
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaEnProceso, tareaCompletada));

        List<DistribucionResponsabilidadResponse> distribucion =
                reporteService.distribucionResponsabilidades(10L, "juan@test.com");

        assertNotNull(distribucion);
        assertEquals(2, distribucion.size());

        DistribucionResponsabilidadResponse distJuan = distribucion.stream()
                .filter(d -> d.getUsuario().equals("Juan"))
                .findFirst().orElseThrow();

        assertEquals(2L, distJuan.getTotalTareas());
        // 2/3 * 100 = 66.67%
        assertEquals(66.67, distJuan.getPorcentaje());
    }

    @Test
    @DisplayName("HU21b - Sin tareas en el hogar lanza BadRequestException")
    void distribucionResponsabilidades_SinTareas_DebeLanzarExcepcion() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        when(tareaRepository.findByHogarId(10L)).thenReturn(List.of());

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reporteService.distribucionResponsabilidades(10L, "juan@test.com")
        );

        assertEquals("No hay tareas registradas para calcular la distribución",
                ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HU22 — Historial de cumplimiento
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU22a - Historial calcula porcentaje de cumplimiento por miembro")
    void historialCumplimiento_SinFiltros_DebeRetornarCumplimientoPorMiembro() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        // usuario1: 2 tareas, 0 completadas → 0%
        // usuario2: 1 tarea, 1 completada → 100%
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaEnProceso, tareaCompletada));

        List<HistorialCumplimientoResponse> historial =
                reporteService.historialCumplimiento(
                        10L, null, null, null, "juan@test.com");

        assertNotNull(historial);
        assertEquals(2, historial.size());

        HistorialCumplimientoResponse historialMaria = historial.stream()
                .filter(h -> h.getUsuario().equals("Maria"))
                .findFirst().orElseThrow();

        assertEquals(1L, historialMaria.getAsignadas());
        assertEquals(1L, historialMaria.getCompletadas());
        assertEquals(100.0, historialMaria.getCumplimiento());
    }

    @Test
    @DisplayName("HU22b - Filtrar por usuarioId muestra solo ese miembro")
    void historialCumplimiento_ConFiltroUsuario_MuestraSoloEseMiembro() {

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaEnProceso, tareaCompletada));

        // Filtrar solo por usuario1 (id=1)
        List<HistorialCumplimientoResponse> historial =
                reporteService.historialCumplimiento(
                        10L, 1L, null, null, "juan@test.com");

        assertNotNull(historial);
        assertEquals(1, historial.size());
        assertEquals("Juan", historial.get(0).getUsuario());
        assertEquals(2L, historial.get(0).getAsignadas());
        assertEquals(0L, historial.get(0).getCompletadas());
        assertEquals(0.0, historial.get(0).getCumplimiento());
    }

    @Test
    @DisplayName("HU22c - Filtrar por rango de fechas retorna solo tareas del período")
    void historialCumplimiento_ConFiltroFecha_FiltraCorrectamente() {

        Tarea tareaFuera = Tarea.builder()
                .id(5L).nombre("Tarea antigua").estado("COMPLETADA")
                .usuario(usuario1).hogar(hogar)
                .fechaInicio(HOY.minusDays(10)).fechaLimite(AYER).build();

        when(miembroHogarRepository.findByHogarIdAndUsuarioEmail(10L, "juan@test.com"))
                .thenReturn(Optional.of(miembro1));
        when(tareaRepository.findByHogarId(10L))
                .thenReturn(List.of(tareaPendiente, tareaFuera));

        List<HistorialCumplimientoResponse> historial =
                reporteService.historialCumplimiento(
                        10L, null, HOY, HOY, "juan@test.com");

        // Solo tareaPendiente tiene fechaInicio = HOY
        assertEquals(1, historial.size());
        assertEquals(1L, historial.get(0).getAsignadas());
    }
}