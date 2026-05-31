package com.domesticas.reporte.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistorialCumplimientoResponse {

    private String usuario;

    private Long asignadas;

    private Long completadas;

    private Double cumplimiento;
}