package com.domesticas.reporte.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReporteUsuarioResponse {

    private String usuario;

    private Long asignadas;

    private Long enProceso;

    private Long completadas;
}