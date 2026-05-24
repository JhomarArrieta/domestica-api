package com.domesticas.tarea.controller;

import com.domesticas.tarea.dto.request.CambiarEstadoRequest;
import com.domesticas.tarea.dto.request.CrearTareaRequest;
import com.domesticas.tarea.service.TareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.domesticas.tarea.dto.response.TareaResponse;
import java.util.List;
import com.domesticas.tarea.dto.request.ActualizarTareaRequest;
import com.domesticas.tarea.dto.response.TareaCompletadaResponse;

@RestController
@RequestMapping("/api/v1/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;

    @PostMapping
    public ResponseEntity<String> crearTarea(
            Authentication authentication,
            @RequestBody CrearTareaRequest request
    ) {
        String email = authentication.getName();
        tareaService.crearTarea(email, request);
        return ResponseEntity.ok("Tarea creada correctamente");
    }

    @PutMapping("/{tareaId}/estado")
public ResponseEntity<String> cambiarEstado(
        @PathVariable Long tareaId,
        Authentication authentication,
        @RequestBody CambiarEstadoRequest request
) {
    String email = authentication.getName();

    tareaService.cambiarEstado(
            tareaId,
            email,
            request.getEstado()
    );

    return ResponseEntity.ok("Estado actualizado correctamente");
}

@GetMapping("/hogar/{hogarId}")
public ResponseEntity<List<TareaResponse>> listarTareas(
        @PathVariable Long hogarId,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) String estado,
        Authentication authentication
) {
    String email = authentication.getName();

    return ResponseEntity.ok(
            tareaService.listarTareas(hogarId, usuarioId, estado, email)
    );
}

@PutMapping("/{tareaId}")
public ResponseEntity<String> actualizarTarea(
        @PathVariable Long tareaId,
        @RequestBody ActualizarTareaRequest request,
        Authentication authentication
) {

    tareaService.actualizarTarea(
            tareaId,
            request,
            authentication.getName()
    );

    return ResponseEntity.ok("Tarea actualizada correctamente");
}


@DeleteMapping("/{tareaId}")
public ResponseEntity<String> eliminarTarea(
        @PathVariable Long tareaId,
        Authentication authentication
) {

    tareaService.eliminarTarea(
            tareaId,
            authentication.getName()
    );

    return ResponseEntity.ok("Tarea eliminada correctamente");
}

@GetMapping("/hogar/{hogarId}/completadas")
public ResponseEntity<List<TareaCompletadaResponse>>
obtenerTareasCompletadas(
        @PathVariable Long hogarId,
        Authentication authentication
) {

    return ResponseEntity.ok(
            tareaService.obtenerTareasCompletadas(
                    hogarId,
                    authentication.getName()
            )
    );
}

}
