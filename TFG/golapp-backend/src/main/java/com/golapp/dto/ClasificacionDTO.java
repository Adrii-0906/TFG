package com.golapp.dto;

import lombok.*;

/**
 * DTO para representar la clasificación de un equipo dentro de un torneo.
 * Se calcula dinámicamente a partir de los partidos FINALIZADOS.
 *
 * Sistema de puntos:
 *   - Victoria:  3 puntos
 *   - Empate:    1 punto
 *   - Derrota:   0 puntos
 *
 * Criterio de ordenación: puntos DESC → diferencia de goles DESC → goles a favor DESC.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClasificacionDTO implements Comparable<ClasificacionDTO> {

    private Long equipoId;
    private String equipoNombre;
    private String escudoUrl;

    @Builder.Default
    private int partidosJugados = 0;

    @Builder.Default
    private int victorias = 0;

    @Builder.Default
    private int empates = 0;

    @Builder.Default
    private int derrotas = 0;

    @Builder.Default
    private int golesAFavor = 0;

    @Builder.Default
    private int golesEnContra = 0;

    @Builder.Default
    private int puntos = 0;

    // ── Métodos de utilidad ─────────────────────────

    /**
     * Calcula la diferencia de goles (GF - GC).
     */
    public int getDiferenciaGoles() {
        return golesAFavor - golesEnContra;
    }

    /**
     * Ordenación natural: por puntos DESC, diferencia de goles DESC, goles a favor DESC.
     */
    @Override
    public int compareTo(ClasificacionDTO otro) {
        // 1. Más puntos primero
        int cmp = Integer.compare(otro.puntos, this.puntos);
        if (cmp != 0) return cmp;

        // 2. Mayor diferencia de goles
        cmp = Integer.compare(otro.getDiferenciaGoles(), this.getDiferenciaGoles());
        if (cmp != 0) return cmp;

        // 3. Más goles a favor
        return Integer.compare(otro.golesAFavor, this.golesAFavor);
    }
}
