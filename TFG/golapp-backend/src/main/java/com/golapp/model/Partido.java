package com.golapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.golapp.model.enums.EstadoPartido;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Partido.
 * Representa un partido entre dos equipos dentro de un torneo.
 * Incluye goles locales/visitantes y estado para el cálculo automatizado de resultados.
 */
@Entity
@Table(name = "partidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_partido")
    private LocalDateTime fechaPartido;

    @Column(name = "jornada")
    private Integer jornada;

    @Column(name = "fase", length = 50)
    private String fase;

    @Min(value = 0, message = "Los goles no pueden ser negativos")
    @Column(name = "goles_local")
    @Builder.Default
    private Integer golesLocal = 0;

    @Min(value = 0, message = "Los goles no pueden ser negativos")
    @Column(name = "goles_visitante")
    @Builder.Default
    private Integer golesVisitante = 0;

    @NotNull(message = "El estado del partido es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPartido estado = EstadoPartido.PROGRAMADO;

    // ── Relaciones ──────────────────────────────────

    /**
     * Torneo al que pertenece el partido.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Torneo torneo;

    /**
     * Expone el nombre del torneo en la respuesta JSON.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("torneoNombre")
    public String obtenerTorneoNombre() {
        return torneo != null ? torneo.getNombre() : null;
    }

    /**
     * Expone el ID del torneo en la respuesta JSON.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("torneoId")
    public Long obtenerTorneoId() {
        return torneo != null ? torneo.getId() : null;
    }

    /**
     * Equipo local.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_local_id", nullable = false)
    private Equipo equipoLocal;

    /**
     * Equipo visitante.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_visitante_id", nullable = false)
    private Equipo equipoVisitante;

    // ── Métodos de utilidad ─────────────────────────

    /**
     * Determina el equipo ganador del partido.
     * @return el equipo ganador, o null si es empate o el partido no ha finalizado.
     */
    public Equipo getGanador() {
        if (estado != EstadoPartido.FINALIZADO) {
            return null;
        }
        if (golesLocal > golesVisitante) {
            return equipoLocal;
        } else if (golesVisitante > golesLocal) {
            return equipoVisitante;
        }
        return null; // Empate
    }

    /**
     * Comprueba si el partido terminó en empate.
     */
    public boolean isEmpate() {
        return estado == EstadoPartido.FINALIZADO
                && golesLocal.equals(golesVisitante);
    }

    /**
     * Devuelve el resultado en formato "X - Y".
     */
    public String getResultado() {
        return golesLocal + " - " + golesVisitante;
    }
}
