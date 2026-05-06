package com.golapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipoExportDto {
    private String nombre;
    private String escudoUrl;
    private String nombreDelegado;
    private String telefonoContacto;
    private String emailContacto;
    
    @Builder.Default
    private List<JugadorExportDto> jugadores = new ArrayList<>();
}
