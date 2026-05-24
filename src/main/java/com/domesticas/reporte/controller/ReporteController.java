package com.domesticas.reporte.controller;

import com.domesticas.reporte.dto.DistribucionResponsabilidadResponse;
import com.domesticas.reporte.dto.HistorialCumplimientoResponse;
import com.domesticas.reporte.dto.ReporteUsuarioResponse;
import com.domesticas.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.domesticas.reporte.dto.DistribucionResponsabilidadResponse;
import java.time.LocalDate;
import com.domesticas.reporte.dto.HistorialCumplimientoResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/tareas-usuarios/{hogarId}")
    public ResponseEntity<List<ReporteUsuarioResponse>> reporteTareasPorUsuario(
            @PathVariable Long hogarId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                reporteService.reporteTareasPorUsuario(
                        hogarId,
                        fechaInicio,
                        fechaFin,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/distribucion/{hogarId}")
public ResponseEntity<List<DistribucionResponsabilidadResponse>>
distribucionResponsabilidades(
        @PathVariable Long hogarId,
        Authentication authentication
) {

    return ResponseEntity.ok(
            reporteService.distribucionResponsabilidades(
                    hogarId,
                    authentication.getName()
            )
    );
}
@GetMapping("/cumplimiento/{hogarId}")
public ResponseEntity<List<HistorialCumplimientoResponse>>
historialCumplimiento(
        @PathVariable Long hogarId,

        @RequestParam(required = false)
        Long usuarioId,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin,

        Authentication authentication
) {

    return ResponseEntity.ok(
            reporteService.historialCumplimiento(
                    hogarId,
                    usuarioId,
                    fechaInicio,
                    fechaFin,
                    authentication.getName()
            )
    );
}
}