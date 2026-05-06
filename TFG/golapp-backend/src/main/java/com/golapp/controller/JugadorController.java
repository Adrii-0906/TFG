package com.golapp.controller;

import com.golapp.dto.JugadorDto;
import com.golapp.model.Jugador;
import com.golapp.service.JugadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de jugadores dentro de un equipo.
 * Ruta base anidada: /api/equipos/{equipoId}/jugadores
 *
 * Endpoints:
 * - POST   /  → Añade un jugador al equipo
 * - GET    /  → Lista la plantilla del equipo
 * - DELETE /{jugadorId} → Elimina un jugador del equipo
 */
@RestController
@RequestMapping("/api/equipos/{equipoId}/jugadores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JugadorController {

    private final JugadorService jugadorService;

    // ── POST /api/equipos/{equipoId}/jugadores ─────

    /**
     * Añade un jugador al equipo.
     * El email del organizador se extrae del token JWT.
     */
    @PostMapping
    public ResponseEntity<?> añadirJugador(@PathVariable Long equipoId,
                                            @Valid @RequestBody JugadorDto dto) {
        try {
            String email = getEmailAutenticado();
            Jugador nuevoJugador = jugadorService.añadirJugador(equipoId, dto, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoJugador);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason() != null ? e.getReason() : "Error de validación"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/equipos/{equipoId}/jugadores ──────

    /**
     * Lista todos los jugadores del equipo.
     * Verifica que el equipo pertenece al organizador autenticado.
     */
    @GetMapping
    public ResponseEntity<?> obtenerJugadores(@PathVariable Long equipoId) {
        try {
            String email = getEmailAutenticado();
            List<Jugador> jugadores = jugadorService.obtenerJugadoresDeEquipo(equipoId, email);
            return ResponseEntity.ok(jugadores);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/equipos/{equipoId}/jugadores/publico ──

    /**
     * Lista todos los jugadores del equipo sin verificar propiedad.
     * Usado para el Acta del Partido donde ambos equipos deben ser visibles.
     */
    @GetMapping("/publico")
    public ResponseEntity<?> obtenerJugadoresPublico(@PathVariable Long equipoId) {
        try {
            List<Jugador> jugadores = jugadorService.obtenerJugadoresPorEquipo(equipoId);
            return ResponseEntity.ok(jugadores);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/equipos/{equipoId}/jugadores/{jugadorId} ──

    /**
     * Elimina un jugador del equipo.
     */
    @DeleteMapping("/{jugadorId}")
    public ResponseEntity<?> eliminarJugador(@PathVariable Long equipoId,
                                              @PathVariable Long jugadorId) {
        try {
            String email = getEmailAutenticado();
            jugadorService.eliminarJugador(equipoId, jugadorId, email);
            return ResponseEntity.ok(Map.of("mensaje", "Jugador eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper privado ─────────────────────────────

    private String getEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
