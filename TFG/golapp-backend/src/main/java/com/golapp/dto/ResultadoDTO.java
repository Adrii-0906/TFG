package com.golapp.dto;

import lombok.*;

/**
 * DTO para recibir los datos de un resultado de partido desde el frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoDTO {

    private int golesLocal;
    private int golesVisitante;
    private java.util.List<EventoDto> eventos;
}
