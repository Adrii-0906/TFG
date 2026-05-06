package com.golapp.dto;

import lombok.Data;

/**
 * DTO para la creación manual de un partido.
 * Recibe los IDs de los equipos y la fecha/hora del partido.
 */
@Data
public class PartidoDTO {

    private Long equipoLocalId;
    private Long equipoVisitanteId;
    private String fechaPartido;
}
