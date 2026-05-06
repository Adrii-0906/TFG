package com.golapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad EventoPartido.
 * Representa un evento ocurrido durante un partido (Gol, Tarjeta Amarilla, Tarjeta Roja)
 * asociado a un jugador específico.
 */
@Entity
@Table(name = "eventos_partido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EventoPartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_evento", nullable = false, length = 20)
    private String tipoEvento; // "GOL", "AMARILLA", "ROJA"

    @Column(name = "minuto")
    private Integer minuto;

    // ── Relaciones ──────────────────────────────────

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;
}
