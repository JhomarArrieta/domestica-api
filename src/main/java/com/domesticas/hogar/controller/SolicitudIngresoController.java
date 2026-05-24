package com.domesticas.hogar.controller;
import java.util.List;
import com.domesticas.hogar.dto.request.ResponderSolicitudRequest;
import com.domesticas.hogar.dto.request.SolicitudIngresoRequest;
import com.domesticas.hogar.dto.response.MisSolicitudesResponse;
import com.domesticas.hogar.service.SolicitudIngresoService;
import com.domesticas.usuario.model.Usuario;
import com.domesticas.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/solicitudes")
@RequiredArgsConstructor
public class SolicitudIngresoController {

    private final SolicitudIngresoService solicitudIngresoService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<String> solicitarIngreso(
            Authentication authentication,
            @RequestBody SolicitudIngresoRequest request
    ) {
        String email = authentication.getName();
        solicitudIngresoService.solicitarIngreso(email, request.getCodigoAcceso());
        return ResponseEntity.ok("Solicitud enviada correctamente");
    }
    
        @PutMapping("/{solicitudId}")
    public ResponseEntity<String> responderSolicitud(
            @PathVariable Long solicitudId,
            Authentication authentication,
            @RequestBody ResponderSolicitudRequest request
    ) {
        String emailAdmin = authentication.getName();

        solicitudIngresoService.responderSolicitud(
                solicitudId,
                emailAdmin,
                request.getAccion(),
                request.getRol()
        );

        return ResponseEntity.ok("Solicitud procesada correctamente");
    }

    @GetMapping("/mis-solicitudes")
public ResponseEntity<List<MisSolicitudesResponse>> misSolicitudes(
        Authentication authentication
) {

    Usuario usuario = usuarioRepository
            .findByEmail(authentication.getName())
            .orElseThrow();

    return ResponseEntity.ok(
            solicitudIngresoService.obtenerMisSolicitudes(usuario.getId())
    );
}
      
}