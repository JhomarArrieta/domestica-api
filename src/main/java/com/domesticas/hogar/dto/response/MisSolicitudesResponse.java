package com.domesticas.hogar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MisSolicitudesResponse {

    private Long id;
    private String hogar;
    private String estado;
    private LocalDate fechaSolicitud;
}