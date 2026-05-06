package com.golapp.service;

import com.golapp.model.Equipo;
import com.golapp.model.Torneo;
import com.golapp.model.Usuario;
import com.golapp.model.enums.Rol;
import com.golapp.repository.EquipoRepository;
import com.golapp.repository.TorneoRepository;
import com.golapp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de torneos.
 * Contiene la lógica de negocio para crear torneos e inscribir equipos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TorneoService {

    private final TorneoRepository torneoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;

    // ── Creación ────────────────────────────────────

    /**
     * Crea un nuevo torneo validando que el usuario creador tenga rol ORGANIZADOR.
     *
     * @param torneo        entidad Torneo con los datos del torneo
     * @param organizadorId ID del usuario que crea el torneo
     * @return el torneo persistido con su ID generado
     * @throws IllegalArgumentException si el usuario no existe o no tiene rol ORGANIZADOR
     */
    @Transactional
    public Torneo crearTorneo(Torneo torneo, Long organizadorId) {
        // Buscar al usuario creador
        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario con ID: " + organizadorId));

        // Validar que el usuario tenga rol ORGANIZADOR
        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new IllegalArgumentException(
                    "El usuario '" + organizador.getUsername()
                            + "' no tiene permisos para crear torneos. "
                            + "Rol requerido: ORGANIZADOR, rol actual: " + organizador.getRol());
        }

        torneo.setOrganizador(organizador);
        log.info("Creando torneo '{}' (tipo: {}) por organizador '{}'",
                torneo.getNombre(), torneo.getTipoTorneo(), organizador.getUsername());

        return torneoRepository.save(torneo);
    }

    // ── Inscripción de Equipos ──────────────────────

    /**
     * Inscribe un equipo en un torneo (añade el registro en la tabla intermedia torneo_equipos).
     *
     * @param torneoId ID del torneo
     * @param equipoId ID del equipo a inscribir
     * @return el torneo actualizado con el equipo añadido
     * @throws IllegalArgumentException si el torneo o equipo no existen, o si el equipo ya está inscrito
     */
    @Transactional
    public Torneo inscribirEquipo(Long torneoId, Long equipoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún torneo con ID: " + torneoId));

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún equipo con ID: " + equipoId));

        // Comprobar que el equipo no esté ya inscrito
        boolean yaInscrito = torneo.getEquipos().stream()
                .anyMatch(e -> e.getId().equals(equipoId));

        if (yaInscrito) {
            throw new IllegalArgumentException(
                    "El equipo '" + equipo.getNombre() + "' ya está inscrito en el torneo '" + torneo.getNombre() + "'");
        }

        torneo.getEquipos().add(equipo);
        log.info("Equipo '{}' inscrito en torneo '{}'", equipo.getNombre(), torneo.getNombre());

        return torneoRepository.save(torneo);
    }

    // ── Búsquedas ───────────────────────────────────

    /**
     * Busca un torneo por su ID.
     *
     * @param id identificador del torneo
     * @return el torneo encontrado
     * @throws IllegalArgumentException si no se encuentra el torneo
     */
    @Transactional(readOnly = true)
    public Torneo buscarPorId(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún torneo con ID: " + id));
    }

    /**
     * Obtiene todos los torneos de un organizador concreto.
     *
     * @param organizadorId ID del organizador
     * @return lista de torneos del organizador
     */
    @Transactional(readOnly = true)
    public List<Torneo> obtenerPorOrganizador(Long organizadorId) {
        return torneoRepository.findByOrganizadorId(organizadorId);
    }

    /**
     * Obtiene todos los torneos registrados.
     *
     * @return lista de todos los torneos
     */
    @Transactional(readOnly = true)
    public List<Torneo> obtenerTodos() {
        return torneoRepository.findAll();
    }

    /**
     * Guarda un torneo (para actualizaciones de relaciones).
     *
     * @param torneo entidad torneo a guardar
     * @return el torneo persistido
     */
    @Transactional
    public Torneo guardar(Torneo torneo) {
        return torneoRepository.save(torneo);
    }
}
