package com.golapp.repository;

import com.golapp.model.Partido;
import com.golapp.model.enums.EstadoPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link Partido}.
 * Proporciona operaciones CRUD y consultas personalizadas sobre partidos.
 */
@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {

    /**
     * Busca todos los partidos de un torneo concreto.
     *
     * @param torneoId ID del torneo
     * @return lista de partidos del torneo
     */
    List<Partido> findByTorneoId(Long torneoId);

    /**
     * Busca partidos según su estado (PROGRAMADO, EN_CURSO, FINALIZADO, SUSPENDIDO).
     *
     * @param estado estado del partido
     * @return lista de partidos con ese estado
     */
    List<Partido> findByEstado(EstadoPartido estado);

    /**
     * Busca partidos de un torneo concreto filtrados por estado.
     * Útil para obtener, por ejemplo, los partidos finalizados de un torneo.
     *
     * @param torneoId ID del torneo
     * @param estado   estado del partido
     * @return lista de partidos que cumplan ambos criterios
     */
    List<Partido> findByTorneoIdAndEstado(Long torneoId, EstadoPartido estado);

    /**
     * Busca todos los partidos en los que un equipo participa como local.
     *
     * @param equipoLocalId ID del equipo local
     * @return lista de partidos como local
     */
    List<Partido> findByEquipoLocalId(Long equipoLocalId);

    /**
     * Busca todos los partidos en los que un equipo participa como visitante.
     *
     * @param equipoVisitanteId ID del equipo visitante
     * @return lista de partidos como visitante
     */
    List<Partido> findByEquipoVisitanteId(Long equipoVisitanteId);

    /**
     * Busca todos los partidos de una jornada concreta dentro de un torneo.
     *
     * @param torneoId ID del torneo
     * @param jornada  número de jornada
     * @return lista de partidos de esa jornada
     */
    List<Partido> findByTorneoIdAndJornada(Long torneoId, Integer jornada);

    /**
     * Busca todos los partidos de un torneo ordenados por fecha ascendente.
     *
     * @param torneoId ID del torneo
     * @return lista de partidos ordenados por fecha
     */
    List<Partido> findByTorneoIdOrderByFechaPartidoAsc(Long torneoId);

    /**
     * Busca todos los partidos del organizador autenticado (cruzando torneo → organizador).
     *
     * @param email email del organizador
     * @return lista de partidos ordenados por fecha
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT p FROM Partido p WHERE p.torneo.organizador.email = :email ORDER BY p.fechaPartido ASC"
    )
    List<Partido> findByOrganizadorEmail(@org.springframework.data.repository.query.Param("email") String email);
}
