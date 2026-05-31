package com.domesticas.tarea.service;

import com.domesticas.exception.BadRequestException;
import com.domesticas.hogar.model.Hogar;
import com.domesticas.hogar.repository.HogarRepository;
import com.domesticas.tarea.dto.request.ActualizarTareaRequest;
import com.domesticas.tarea.dto.request.CrearTareaRequest;
import com.domesticas.tarea.model.Tarea;
import com.domesticas.tarea.repository.TareaRepository;
import com.domesticas.usuario.model.Usuario;
import com.domesticas.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import com.domesticas.tarea.dto.response.TareaResponse;
import com.domesticas.hogar.model.MiembroHogar;
import com.domesticas.hogar.repository.MiembroHogarRepository;
import com.domesticas.tarea.dto.response.TareaCompletadaResponse;
import java.util.List;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HogarRepository hogarRepository;
    private final MiembroHogarRepository miembroHogarRepository;

    public void crearTarea(String email, CrearTareaRequest request) {

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BadRequestException("El nombre es obligatorio");
        }

        if (request.getFechaLimite() == null) {
            throw new BadRequestException("La fecha límite es obligatoria");
        }

        if (request.getPrioridad() == null || request.getPrioridad().isBlank()) {
            throw new BadRequestException("La prioridad es obligatoria");
        }

        if (request.getFechaInicio() != null &&
            request.getFechaLimite().isBefore(request.getFechaInicio())) {
            throw new BadRequestException("La fecha límite no puede ser anterior a la fecha de inicio");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        Hogar hogar = hogarRepository.findById(request.getHogarId())
                .orElseThrow(() -> new BadRequestException("Grupo no encontrado"));

        Tarea tarea = Tarea.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .fechaInicio(request.getFechaInicio())
                .fechaLimite(request.getFechaLimite())
                .prioridad(request.getPrioridad())
                .estado("PENDIENTE")
                .usuario(usuario)
                .hogar(hogar)
                .build();

        tareaRepository.save(tarea);
    }

    public void cambiarEstado(Long tareaId, String email, String nuevoEstado) {

    Tarea tarea = tareaRepository.findById(tareaId)
            .orElseThrow(() -> new BadRequestException("Tarea no encontrada"));

    // validar que es el dueño
    if (!tarea.getUsuario().getEmail().equals(email)) {
        throw new BadRequestException("Solo el usuario asignado puede cambiar el estado");
    }

    String estadoActual = tarea.getEstado();

    // validar flujo
    if ("PENDIENTE".equals(estadoActual) && "EN_PROCESO".equals(nuevoEstado)) {
        tarea.setEstado("EN_PROCESO");

    } else if ("EN_PROCESO".equals(estadoActual) && "COMPLETADA".equals(nuevoEstado)) {
        tarea.setEstado("COMPLETADA");
        tarea.setFechaFin(LocalDateTime.now());

    } else {
        throw new BadRequestException("Transición de estado inválida");
    }

    tareaRepository.save(tarea);
    
    }

    public List<TareaResponse> listarTareas(Long hogarId, Long usuarioId, String estado, String email) {

    miembroHogarRepository.findByHogarIdAndUsuarioEmail(hogarId, email)
            .orElseThrow(() -> new BadRequestException("No perteneces a este hogar"));

    List<Tarea> tareas;

    if (usuarioId != null && estado != null && !estado.isBlank()) {
        tareas = tareaRepository.findByHogarIdAndUsuarioIdAndEstado(hogarId, usuarioId, estado.toUpperCase());
    } else if (usuarioId != null) {
        tareas = tareaRepository.findByHogarIdAndUsuarioId(hogarId, usuarioId);
    } else if (estado != null && !estado.isBlank()) {
        tareas = tareaRepository.findByHogarIdAndEstado(hogarId, estado.toUpperCase());
    } else {
        tareas = tareaRepository.findByHogarId(hogarId);
    }

    return tareas.stream()
            .map(t -> TareaResponse.builder()
                    .id(t.getId())
                    .nombre(t.getNombre())
                    .descripcion(t.getDescripcion())
                    .estado(t.getEstado())
                    .prioridad(t.getPrioridad())
                    .fechaInicio(t.getFechaInicio())
                    .fechaLimite(t.getFechaLimite())
                    .usuarioId(t.getUsuario().getId())
                    .miembroAsignado(t.getUsuario().getNombre())
                    .hogarId(t.getHogar().getId())
                    .build())
            .toList();
}

    public void actualizarTarea(
        Long tareaId,
        ActualizarTareaRequest request,
        String email
) {

    Tarea tarea = tareaRepository.findById(tareaId)
            .orElseThrow(() -> new BadRequestException("Tarea no encontrada"));

    MiembroHogar miembro = miembroHogarRepository
            .findByHogarIdAndUsuarioEmail(
                    tarea.getHogar().getId(),
                    email
            )
            .orElseThrow(() -> new BadRequestException("No perteneces al hogar"));

    boolean esAdmin = miembro.getRol().getNombre().equalsIgnoreCase("ADMIN");

    boolean esAsignado = tarea.getUsuario().getEmail().equals(email);

    if (!esAdmin && !esAsignado) {
        throw new BadRequestException("No tienes permisos para editar esta tarea");
    }

    if (tarea.getEstado().equalsIgnoreCase("COMPLETADA")) {
        throw new BadRequestException("Las tareas completadas no pueden editarse");
    }

    if (request.getFechaInicio() != null &&
            request.getFechaLimite() != null &&
            request.getFechaLimite().isBefore(request.getFechaInicio())) {

        throw new BadRequestException("La fecha límite no puede ser menor");
    }

    tarea.setNombre(request.getNombre());
    tarea.setDescripcion(request.getDescripcion());
    tarea.setPrioridad(request.getPrioridad());
    tarea.setFechaInicio(request.getFechaInicio());
    tarea.setFechaLimite(request.getFechaLimite());

    tareaRepository.save(tarea);
}
    public void eliminarTarea(Long tareaId, String email) {

    Tarea tarea = tareaRepository.findById(tareaId)
            .orElseThrow(() -> new BadRequestException("Tarea no encontrada"));

    MiembroHogar miembro = miembroHogarRepository
            .findByHogarIdAndUsuarioEmail(
                    tarea.getHogar().getId(),
                    email
            )
            .orElseThrow(() -> new BadRequestException("No perteneces al hogar"));

    boolean esAdmin = miembro.getRol().getNombre().equalsIgnoreCase("ADMIN");

    boolean esAsignado = tarea.getUsuario().getEmail().equals(email);

    if (!esAdmin && !esAsignado) {
        throw new BadRequestException(
                "No tienes permisos para eliminar esta tarea"
        );
    }

    tareaRepository.delete(tarea);
}

public List<TareaCompletadaResponse> obtenerTareasCompletadas(
        Long hogarId,
        String email
) {

    miembroHogarRepository
            .findByHogarIdAndUsuarioEmail(hogarId, email)
            .orElseThrow(() ->
                    new BadRequestException("No perteneces al hogar"));

    List<Tarea> tareas = tareaRepository
            .findByHogarIdAndEstado(hogarId, "COMPLETADA");

    return tareas.stream()
            .map(t -> TareaCompletadaResponse.builder()
                    .id(t.getId())
                    .nombreTarea(t.getNombre())
                    .miembro(t.getUsuario().getNombre())
                    .fechaFinalizacion(t.getFechaLimite())
                    .build())
            .toList();
}

}