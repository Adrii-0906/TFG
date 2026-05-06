package com.golapp.dto;

import lombok.*;

/**
 * DTO para representar un evento de partido (Goles, Tarjetas) asociado a un jugador.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoDto {
    private Long jugadorId;
    private String tipoEvento; // "GOL", "AMARILLA", "ROJA"
    private Integer minuto;
}
