package com.golapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Equipo.
 * Representa un equipo de fútbol creado por un organizador.
 * Cada equipo pertenece a un usuario (organizador) y puede participar en varios torneos.
 */
@Entity
@Table(name = "equipos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "escudo_url", length = 500)
    private String escudoUrl;

    @Column(name = "nombre_delegado", length = 100)
    private String nombreDelegado;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(name = "codigo_acceso", unique = true, length = 8)
    private String codigoAcceso;

    // ── Relaciones ──────────────────────────────────

    /**
     * Organizador que creó este equipo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"torneos", "password", "fechaRegistro"})
    private Usuario usuario;

    /**
     * Jugadores que pertenecen a este equipo.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Jugador> jugadores = new ArrayList<>();

    /**
     * Torneos en los que participa este equipo.
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "equipos")
    @Builder.Default
    private List<Torneo> torneos = new ArrayList<>();
}
