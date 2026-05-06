package com.golapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JugadorExportDto {
    private String nombreCompleto;
    private Integer dorsal;
    private String posicion;
}
