package com.golapp.repository;

import com.golapp.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link Jugador}.
 * Proporciona operaciones CRUD y consultas personalizadas sobre jugadores.
 */
@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    /**
     * Busca todos los jugadores que pertenecen a un equipo concreto.
     *
     * @param equipoId ID del equipo
     * @return lista de jugadores del equipo
     */
    List<Jugador> findByEquipoId(Long equipoId);

    /**
     * Cuenta el número de jugadores de un equipo.
     *
     * @param equipoId ID del equipo
     * @return número de jugadores
     */
    long countByEquipoId(Long equipoId);

    /**
     * Busca jugadores por nombre completo (sin distinguir mayúsculas).
     *
     * @param nombreCompleto nombre o parte del nombre
     * @return lista de jugadores que coincidan
     */
    List<Jugador> findByNombreCompletoContainingIgnoreCase(String nombreCompleto);

    /**
     * Busca un jugador por su dorsal dentro de un equipo concreto.
     *
     * @param dorsal número de dorsal
     * @param equipoId ID del equipo
     * @return lista de jugadores (normalmente 0 o 1)
     */
    List<Jugador> findByDorsalAndEquipoId(Integer dorsal, Long equipoId);
}
