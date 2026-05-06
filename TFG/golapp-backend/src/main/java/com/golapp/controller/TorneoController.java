package com.golapp.controller;

import com.golapp.dto.ClasificacionDTO;
import com.golapp.model.Equipo;
import com.golapp.model.Partido;
import com.golapp.model.Torneo;
import com.golapp.repository.EquipoRepository;
import com.golapp.service.ClasificacionService;
import com.golapp.service.TorneoMotorService;
import com.golapp.service.TorneoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de torneos.
 * Expone los endpoints de creación de torneos y vinculación de equipos.
 */
@RestController
@RequestMapping("/api/torneos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TorneoController {

    private final TorneoService torneoService;
    private final TorneoMotorService torneoMotorService;
    private final ClasificacionService clasificacionService;
    private final EquipoRepository equipoRepository;

    // ── POST /api/torneos?organizadorId={id} ────────

    /**
     * Crea un nuevo torneo. Requiere el ID del organizador como parámetro.
     */
    @PostMapping
    public ResponseEntity<?> crearTorneo(@RequestBody Torneo torneo,
                                         @RequestParam Long organizadorId) {
        try {
            Torneo nuevoTorneo = torneoService.crearTorneo(torneo, organizadorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTorneo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/torneos/{torneoId}/inscripcion?equipoId={id} ──

    /**
     * Inscribe un equipo existente en un torneo (legacy).
     */
    @PostMapping("/{torneoId}/inscripcion")
    public ResponseEntity<?> inscribirEquipo(@PathVariable Long torneoId,
                                              @RequestParam Long equipoId) {
        try {
            Torneo torneoActualizado = torneoService.inscribirEquipo(torneoId, equipoId);
            return ResponseEntity.ok(torneoActualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/torneos/{id} ───────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            Torneo torneo = torneoService.buscarPorId(id);
            return ResponseEntity.ok(torneo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/torneos ────────────────────────────

    @GetMapping
    public ResponseEntity<List<Torneo>> obtenerTodos() {
        return ResponseEntity.ok(torneoService.obtenerTodos());
    }

    // ── GET /api/torneos/organizador/{organizadorId} ─

    @GetMapping("/organizador/{organizadorId}")
    public ResponseEntity<List<Torneo>> obtenerPorOrganizador(@PathVariable Long organizadorId) {
        return ResponseEntity.ok(torneoService.obtenerPorOrganizador(organizadorId));
    }

    // ══════════════════════════════════════════════════
    // ── Endpoints de Equipos dentro de un Torneo ─────
    // ══════════════════════════════════════════════════

    /**
     * Lista los equipos del torneo.
     * GET /api/torneos/{id}/equipos
     */
    @GetMapping("/{id}/equipos")
    public ResponseEntity<?> obtenerEquiposPorTorneo(@PathVariable Long id) {
        try {
            Torneo torneo = torneoService.buscarPorId(id);
            return ResponseEntity.ok(torneo.getEquipos());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Vincula un equipo EXISTENTE al torneo.
     * Valida que ambos (torneo y equipo) pertenecen al organizador autenticado.
     * POST /api/torneos/{torneoId}/equipos/{equipoId}
     */
    @PostMapping("/{torneoId}/equipos/{equipoId}")
    public ResponseEntity<?> vincularEquipoATorneo(@PathVariable Long torneoId,
                                                     @PathVariable Long equipoId) {
        try {
            String email = getEmailAutenticado();

            Torneo torneo = torneoService.buscarPorId(torneoId);
            if (!torneo.getOrganizador().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permisos sobre este torneo."));
            }

            Equipo equipo = equipoRepository.findById(equipoId)
                    .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado con ID: " + equipoId));

            if (equipo.getUsuario() == null || !equipo.getUsuario().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permisos sobre este equipo."));
            }

            // Verificar que no esté ya vinculado
            boolean yaVinculado = torneo.getEquipos().stream()
                    .anyMatch(e -> e.getId().equals(equipoId));
            if (yaVinculado) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Este equipo ya está inscrito en el torneo."));
            }

            torneo.getEquipos().add(equipo);
            torneoService.guardar(torneo);

            return ResponseEntity.ok(Map.of("mensaje", "Equipo vinculado al torneo correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Desvincula un equipo del torneo (NO lo borra de la BD).
     * DELETE /api/torneos/{torneoId}/equipos/{equipoId}
     */
    @DeleteMapping("/{torneoId}/equipos/{equipoId}")
    public ResponseEntity<?> desvincularEquipoDeTorneo(@PathVariable Long torneoId,
                                                        @PathVariable Long equipoId) {
        try {
            Torneo torneo = torneoService.buscarPorId(torneoId);
            boolean removed = torneo.getEquipos().removeIf(e -> e.getId().equals(equipoId));
            if (!removed) {
                return ResponseEntity.badRequest().body(Map.of("error", "El equipo no está en este torneo"));
            }
            torneoService.guardar(torneo);
            return ResponseEntity.ok(Map.of("mensaje", "Equipo desvinculado del torneo"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ──────────────────────────────────────

    private String getEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ══════════════════════════════════════════════════
    // ── Motor de Torneos ─────────────────────────────
    // ══════════════════════════════════════════════════

    /**
     * Genera el calendario automático (sorteo).
     * POST /api/torneos/{id}/generar-calendario
     */
    @PostMapping("/{id}/generar-calendario")
    public ResponseEntity<?> generarCalendario(@PathVariable Long id) {
        try {
            String email = getEmailAutenticado();
            List<Partido> partidos = torneoMotorService.generarCalendario(id, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(partidos);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason() != null ? e.getReason() : "Error"));
        }
    }

    /**
     * Clasificación de liga.
     * GET /api/torneos/{id}/clasificacion/liga
     */
    @GetMapping("/{id}/clasificacion/liga")
    public ResponseEntity<?> obtenerClasificacionLiga(@PathVariable Long id) {
        try {
            List<ClasificacionDTO> clasificacion = clasificacionService.obtenerClasificacion(id);
            return ResponseEntity.ok(clasificacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bracket de eliminatoria — partidos agrupados por fase.
     * GET /api/torneos/{id}/clasificacion/bracket
     */
    @GetMapping("/{id}/clasificacion/bracket")
    public ResponseEntity<?> obtenerBracket(@PathVariable Long id) {
        try {
            Torneo torneo = torneoService.buscarPorId(id);
            List<Partido> partidos = torneo.getPartidos();

            // Agrupar por fase
            Map<String, List<Map<String, Object>>> bracket = partidos.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.getFase() != null ? p.getFase() : "Sin fase",
                            Collectors.mapping(p -> {
                                Map<String, Object> m = new java.util.LinkedHashMap<>();
                                m.put("id", p.getId());
                                m.put("equipoLocal", Map.of("id", p.getEquipoLocal().getId(), "nombre", p.getEquipoLocal().getNombre()));
                                m.put("equipoVisitante", Map.of("id", p.getEquipoVisitante().getId(), "nombre", p.getEquipoVisitante().getNombre()));
                                m.put("golesLocal", p.getGolesLocal());
                                m.put("golesVisitante", p.getGolesVisitante());
                                m.put("estado", p.getEstado().name());
                                m.put("fase", p.getFase());
                                return m;
                            }, Collectors.toList())
                    ));

            return ResponseEntity.ok(bracket);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
