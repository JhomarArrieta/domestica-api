package com.domesticas.reporte.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import com.domesticas.reporte.dto.DistribucionResponsabilidadResponse;
import com.domesticas.reporte.dto.HistorialCumplimientoResponse;
import com.domesticas.reporte.dto.ReporteUsuarioResponse;
import com.domesticas.tarea.model.Tarea;
import com.domesticas.tarea.repository.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.domesticas.reporte.dto.DistribucionResponsabilidadResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final TareaRepository tareaRepository;
    private final MiembroHogarRepository miembroHogarRepository;

    public List<ReporteUsuarioResponse> reporteTareasPorUsuario(
            Long hogarId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String email
    ) {
        miembroHogarRepository.findByHogarIdAndUsuarioEmail(hogarId, email)
                .orElseThrow(() -> new BadRequestException("No perteneces a este hogar"));

        List<Tarea> tareas = tareaRepository.findByHogarId(hogarId);

        if (fechaInicio != null && fechaFin != null) {
            tareas = tareas.stream()
                    .filter(t -> t.getFechaInicio() != null)
                    .filter(t -> !t.getFechaInicio().isBefore(fechaInicio)
                            && !t.getFechaInicio().isAfter(fechaFin))
                    .toList();
        }

        Map<Long, List<Tarea>> tareasPorUsuario = tareas.stream()
                .collect(Collectors.groupingBy(t -> t.getUsuario().getId()));

        return tareasPorUsuario.values().stream()
                .map(lista -> {
                    Tarea primera = lista.get(0);

                    return ReporteUsuarioResponse.builder()
                            .usuario(primera.getUsuario().getNombre())
                            .asignadas((long) lista.size())
                            .enProceso(lista.stream()
                                    .filter(t -> "EN_PROCESO".equalsIgnoreCase(t.getEstado()))
                                    .count())
                            .completadas(lista.stream()
                                    .filter(t -> "COMPLETADA".equalsIgnoreCase(t.getEstado()))
                                    .count())
                            .build();
                })
                .toList();
    }
    public List<DistribucionResponsabilidadResponse> distribucionResponsabilidades(
        Long hogarId,
        String email
) {

    miembroHogarRepository.findByHogarIdAndUsuarioEmail(hogarId, email)
            .orElseThrow(() ->
                    new BadRequestException("No perteneces a este hogar"));

    List<Tarea> tareas = tareaRepository.findByHogarId(hogarId);

    if (tareas.isEmpty()) {
        throw new BadRequestException(
                "No hay tareas registradas para calcular la distribución"
        );
    }

    Long totalTareasGrupo = (long) tareas.size();

    Map<Long, List<Tarea>> tareasPorUsuario = tareas.stream()
            .collect(Collectors.groupingBy(
                    t -> t.getUsuario().getId()
            ));

    return tareasPorUsuario.values().stream()
            .map(lista -> {

                Tarea primera = lista.get(0);

                long totalUsuario = lista.size();

                double porcentaje =
                        ((double) totalUsuario / totalTareasGrupo) * 100;

                return DistribucionResponsabilidadResponse.builder()
                        .usuario(primera.getUsuario().getNombre())
                        .totalTareas(totalUsuario)
                        .porcentaje(
                                Math.round(porcentaje * 100.0) / 100.0
                        )
                        .build();
            })
            .toList();
}
public List<HistorialCumplimientoResponse> historialCumplimiento(
        Long hogarId,
        Long usuarioId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String email
) {

    miembroHogarRepository.findByHogarIdAndUsuarioEmail(hogarId, email)
            .orElseThrow(() ->
                    new BadRequestException("No perteneces a este hogar"));

    List<Tarea> tareas = tareaRepository.findByHogarId(hogarId);

    if (usuarioId != null) {
        tareas = tareas.stream()
                .filter(t -> t.getUsuario().getId().equals(usuarioId))
                .toList();
    }

    if (fechaInicio != null && fechaFin != null) {
        tareas = tareas.stream()
                .filter(t -> t.getFechaInicio() != null)
                .filter(t ->
                        !t.getFechaInicio().isBefore(fechaInicio)
                                && !t.getFechaInicio().isAfter(fechaFin))
                .toList();
    }

    Map<Long, List<Tarea>> tareasPorUsuario = tareas.stream()
            .collect(Collectors.groupingBy(
                    t -> t.getUsuario().getId()
            ));

    return tareasPorUsuario.values().stream()
            .map(lista -> {

                Tarea primera = lista.get(0);

                long asignadas = lista.size();

                long completadas = lista.stream()
                        .filter(t ->
                                "COMPLETADA".equalsIgnoreCase(
                                        t.getEstado()
                                ))
                        .count();

                double cumplimiento =
                        asignadas == 0
                                ? 0
                                : ((double) completadas / asignadas) * 100;

                return HistorialCumplimientoResponse.builder()
                        .usuario(primera.getUsuario().getNombre())
                        .asignadas(asignadas)
                        .completadas(completadas)
                        .cumplimiento(
                                Math.round(cumplimiento * 100.0) / 100.0
                        )
                        .build();
            })
            .toList();
}
}