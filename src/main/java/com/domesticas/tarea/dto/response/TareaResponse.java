package com.domesticas.tarea.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TareaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String estado;
    private String prioridad;
    private LocalDate fechaInicio;
    private LocalDate fechaLimite;
    private Long usuarioId;
    private String miembroAsignado;
    private Long hogarId;
}