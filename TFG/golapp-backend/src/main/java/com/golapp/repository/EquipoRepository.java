package com.golapp.repository;

import com.golapp.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Equipo}.
 * Proporciona operaciones CRUD y consultas personalizadas sobre equipos.
 */
@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    /**
     * Busca un equipo por su nombre exacto.
     *
     * @param nombre nombre del equipo
     * @return un Optional con el equipo si existe
     */
    Optional<Equipo> findByNombre(String nombre);

    /**
     * Busca equipos cuyo nombre contenga el texto indicado (sin distinguir mayúsculas).
     *
     * @param nombre texto a buscar
     * @return lista de equipos que coincidan
     */
    List<Equipo> findByNombreContainingIgnoreCase(String nombre);

    /**
     * Comprueba si ya existe un equipo con ese nombre.
     *
     * @param nombre nombre a comprobar
     * @return true si el equipo ya existe
     */
    boolean existsByNombre(String nombre);

    /**
     * Obtiene todos los equipos creados por un organizador, buscando por su email.
     * Spring Data JPA navega la relación Equipo → Usuario → email.
     *
     * @param email email del organizador
     * @return lista de equipos del organizador
     */
    List<Equipo> findByUsuarioEmail(String email);

    /**
     * Obtiene todos los equipos creados por un organizador, buscando por su ID.
     *
     * @param usuarioId ID del organizador
     * @return lista de equipos del organizador
     */
    List<Equipo> findByUsuarioId(Long usuarioId);

    Optional<Equipo> findByCodigoAcceso(String codigoAcceso);
}
