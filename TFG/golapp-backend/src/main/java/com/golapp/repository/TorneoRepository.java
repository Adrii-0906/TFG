package com.golapp.repository;

import com.golapp.model.Torneo;
import com.golapp.model.enums.TipoTorneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link Torneo}.
 * Proporciona operaciones CRUD y consultas personalizadas sobre torneos.
 */
@Repository
public interface TorneoRepository extends JpaRepository<Torneo, Long> {

    /**
     * Busca todos los torneos creados por un organizador concreto.
     *
     * @param organizadorId ID del usuario organizador
     * @return lista de torneos gestionados por ese organizador
     */
    List<Torneo> findByOrganizadorId(Long organizadorId);

    /**
     * Busca torneos por su tipo (LIGA o ELIMINATORIA).
     *
     * @param tipoTorneo tipo de torneo
     * @return lista de torneos de ese tipo
     */
    List<Torneo> findByTipoTorneo(TipoTorneo tipoTorneo);

    /**
     * Busca torneos cuyo nombre contenga el texto indicado (búsqueda parcial, sin distinguir mayúsculas).
     *
     * @param nombre texto a buscar en el nombre
     * @return lista de torneos que coincidan
     */
    List<Torneo> findByNombreContainingIgnoreCase(String nombre);
}
