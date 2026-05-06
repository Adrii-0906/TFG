package com.golapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad Jugador.
 * Representa un jugador asociado a un equipo.
 * Incluye datos personales, posición y estadísticas básicas (goles, tarjetas).
 */
@Entity
@Table(name = "jugadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del jugador es obligatorio")
    @Size(max = 150)
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "dorsal")
    private Integer dorsal;

    @Column(length = 50)
    private String posicion;

    // ── Estadísticas ────────────────────────────────

    @Builder.Default
    @Column(nullable = false)
    private int goles = 0;

    @Builder.Default
    @Column(name = "tarjetas_amarillas", nullable = false)
    private int tarjetasAmarillas = 0;

    @Builder.Default
    @Column(name = "tarjetas_rojas", nullable = false)
    private int tarjetasRojas = 0;

    // ── Relaciones ──────────────────────────────────

    /**
     * Equipo al que pertenece el jugador (muchos jugadores → un equipo).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Equipo equipo;
}
