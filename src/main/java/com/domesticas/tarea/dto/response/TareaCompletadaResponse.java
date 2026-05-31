package com.domesticas.tarea.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TareaCompletadaResponse {

    private Long id;
    private String nombreTarea;
    private String miembro;
    private LocalDate fechaFinalizacion;
}