package com.golapp.service;

import com.golapp.dto.JugadorDto;
import com.golapp.model.Equipo;
import com.golapp.model.Jugador;
import com.golapp.repository.EquipoRepository;
import com.golapp.repository.JugadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio para la gestión de jugadores.
 * Contiene la lógica de negocio para añadir, listar y eliminar jugadores
 * dentro de un equipo, verificando siempre que el equipo pertenezca
 * al organizador autenticado.
 *
 * Reglas de plantilla: mínimo 11, máximo 22 jugadores por equipo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JugadorService {

    private static final int MAX_JUGADORES = 22;

    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;

    // ── Creación ────────────────────────────────────

    /**
     * Añade un nuevo jugador a un equipo.
     * Verifica que el equipo pertenece al organizador autenticado
     * y que la plantilla no ha alcanzado el límite de 22 jugadores.
     *
     * @param equipoId         ID del equipo
     * @param dto              datos del jugador a crear
     * @param emailOrganizador email del usuario autenticado (extraído del JWT)
     * @return el jugador persistido
     * @throws IllegalArgumentException si el equipo no existe o no pertenece al organizador
     * @throws ResponseStatusException  si la plantilla ha alcanzado el límite de 22 jugadores
     */
    @Transactional
    public Jugador añadirJugador(Long equipoId, JugadorDto dto, String emailOrganizador) {
        Equipo equipo = buscarEquipoConVerificacion(equipoId, emailOrganizador);

        // Verificar límite máximo de plantilla
        long totalJugadores = jugadorRepository.countByEquipoId(equipoId);
        if (totalJugadores >= MAX_JUGADORES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La plantilla ha alcanzado el límite máximo de " + MAX_JUGADORES + " jugadores.");
        }

        Jugador jugador = Jugador.builder()
                .nombreCompleto(dto.getNombreCompleto())
                .dorsal(dto.getDorsal())
                .posicion(dto.getPosicion())
                .equipo(equipo)
                .build();

        log.info("Añadiendo jugador '{}' (dorsal {}) al equipo '{}' [{}/{}]",
                jugador.getNombreCompleto(), jugador.getDorsal(), equipo.getNombre(),
                totalJugadores + 1, MAX_JUGADORES);

        return jugadorRepository.save(jugador);
    }

    // ── Consultas ───────────────────────────────────

    /**
     * Obtiene todos los jugadores de un equipo.
     * Verifica que el equipo pertenece al organizador autenticado.
     *
     * @param equipoId         ID del equipo
     * @param emailOrganizador email del usuario autenticado
     * @return lista de jugadores del equipo
     */
    @Transactional(readOnly = true)
    public List<Jugador> obtenerJugadoresDeEquipo(Long equipoId, String emailOrganizador) {
        buscarEquipoConVerificacion(equipoId, emailOrganizador);
        return jugadorRepository.findByEquipoId(equipoId);
    }

    /**
     * Obtiene todos los jugadores de un equipo sin verificación de propiedad.
     * Usado para el Acta del Partido.
     */
    @Transactional(readOnly = true)
    public List<Jugador> obtenerJugadoresPorEquipo(Long equipoId) {
        equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún equipo con ID: " + equipoId));
        return jugadorRepository.findByEquipoId(equipoId);
    }

    // ── Eliminación ─────────────────────────────────

    /**
     * Elimina un jugador verificando que pertenece a un equipo del organizador.
     *
     * @param equipoId         ID del equipo
     * @param jugadorId        ID del jugador a eliminar
     * @param emailOrganizador email del organizador autenticado
     * @throws IllegalArgumentException si el equipo/jugador no existe o no pertenece al organizador
     */
    @Transactional
    public void eliminarJugador(Long equipoId, Long jugadorId, String emailOrganizador) {
        buscarEquipoConVerificacion(equipoId, emailOrganizador);

        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún jugador con ID: " + jugadorId));

        if (!jugador.getEquipo().getId().equals(equipoId)) {
            throw new IllegalArgumentException("El jugador no pertenece a este equipo.");
        }

        log.info("Eliminando jugador '{}' (ID: {}) del equipo ID: {}",
                jugador.getNombreCompleto(), jugadorId, equipoId);

        jugadorRepository.delete(jugador);
    }

    // ── Helper privado ──────────────────────────────

    /**
     * Busca un equipo por su ID y verifica que pertenece al organizador autenticado.
     *
     * @param equipoId         ID del equipo
     * @param emailOrganizador email del usuario autenticado
     * @return el equipo verificado
     * @throws IllegalArgumentException si el equipo no existe o no pertenece al organizador
     */
    private Equipo buscarEquipoConVerificacion(Long equipoId, String emailOrganizador) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún equipo con ID: " + equipoId));

        if (equipo.getUsuario() == null || !equipo.getUsuario().getEmail().equals(emailOrganizador)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para gestionar jugadores de este equipo.");
        }

        return equipo;
    }
}
