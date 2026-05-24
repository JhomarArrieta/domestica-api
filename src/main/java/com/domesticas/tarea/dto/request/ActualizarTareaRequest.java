package com.domesticas.tarea.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActualizarTareaRequest {

    private String nombre;
    private String descripcion;
    private String prioridad;
    private LocalDate fechaInicio;
    private LocalDate fechaLimite;
}