package com.golapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.golapp.model.enums.EstadoTorneo;
import com.golapp.model.enums.TipoTorneo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Torneo.
 * Representa un torneo de fútbol que puede ser de tipo LIGA o ELIMINATORIA.
 */
@Entity
@Table(name = "torneos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del torneo es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fondo_url", length = 500)
    private String fondoUrl;

    @NotNull(message = "El tipo de torneo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_torneo", nullable = false, length = 20)
    private TipoTorneo tipoTorneo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoTorneo estado = EstadoTorneo.BORRADOR;

    // ── Relaciones ──────────────────────────────────

    /**
     * Organizador del torneo (muchos torneos → un organizador).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    /**
     * Equipos que participan en este torneo.
     */
    @ManyToMany
    @JoinTable(
            name = "torneo_equipos",
            joinColumns = @JoinColumn(name = "torneo_id"),
            inverseJoinColumns = @JoinColumn(name = "equipo_id")
    )
    @Builder.Default
    private List<Equipo> equipos = new ArrayList<>();

    /**
     * Partidos que pertenecen a este torneo.
     */
    @OneToMany(mappedBy = "torneo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Partido> partidos = new ArrayList<>();

    // ── Callbacks ───────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
