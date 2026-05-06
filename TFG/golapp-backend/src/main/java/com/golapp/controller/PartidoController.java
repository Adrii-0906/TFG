package com.golapp.controller;

import com.golapp.dto.PartidoDTO;
import com.golapp.dto.ResultadoDTO;
import com.golapp.model.Partido;
import com.golapp.service.PartidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de partidos.
 * Expone los endpoints de generación de calendario, creación manual y registro de resultados.
 */
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PartidoController {

    private final PartidoService partidoService;

    // ── GET /api/partidos/mis-partidos ──────────────

    /**
     * Obtiene todos los partidos del organizador autenticado (de todos sus torneos).
     */
    @GetMapping("/api/partidos/mis-partidos")
    public ResponseEntity<?> obtenerMisPartidos() {
        try {
            String email = getEmailAutenticado();
            List<Partido> partidos = partidoService.obtenerPartidosDelOrganizador(email);
            return ResponseEntity.ok(partidos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/partidos/torneo/{torneoId} ─────────

    /**
     * Obtiene todos los partidos de un torneo concreto.
     */
    @GetMapping("/api/partidos/torneo/{torneoId}")
    public ResponseEntity<List<Partido>> obtenerPorTorneo(@PathVariable Long torneoId) {
        List<Partido> partidos = partidoService.obtenerPorTorneo(torneoId);
        return ResponseEntity.ok(partidos);
    }

    // ── POST /api/torneos/{torneoId}/partidos ───────

    /**
     * Crea un partido manualmente dentro de un torneo.
     * Valida que los equipos estén inscritos y que no sean el mismo.
     */
    @PostMapping("/api/torneos/{torneoId}/partidos")
    public ResponseEntity<?> crearPartido(@PathVariable Long torneoId,
                                            @RequestBody PartidoDTO dto) {
        try {
            String email = getEmailAutenticado();
            Partido partido = partidoService.crearPartido(torneoId, dto, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(partido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/partidos/generar/{torneoId} ───────

    /**
     * Genera automáticamente el calendario Round-Robin para un torneo de tipo LIGA.
     */
    @PostMapping("/api/partidos/generar/{torneoId}")
    public ResponseEntity<?> generarCalendario(@PathVariable Long torneoId) {
        try {
            List<Partido> partidos = partidoService.generarCalendario(torneoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(partidos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/partidos/{partidoId}/resultado ─────

    /**
     * Registra o actualiza el resultado de un partido y lo marca como FINALIZADO.
     */
    @PutMapping("/api/partidos/{partidoId}/resultado")
    public ResponseEntity<?> registrarResultado(@PathVariable Long partidoId,
                                                 @RequestBody ResultadoDTO resultadoDTO) {
        try {
            Partido partido = partidoService.registrarResultado(partidoId, resultadoDTO);
            return ResponseEntity.ok(partido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/partidos/{partidoId}/fecha ────────

    /**
     * Actualiza la fecha/hora de un partido.
     */
    @PutMapping("/api/partidos/{partidoId}/fecha")
    public ResponseEntity<?> actualizarFecha(@PathVariable Long partidoId,
                                              @RequestBody Map<String, String> body) {
        try {
            String fechaStr = body.get("fechaPartido");
            if (fechaStr == null || fechaStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La fecha es obligatoria."));
            }
            java.time.LocalDateTime fecha = java.time.LocalDateTime.parse(fechaStr);
            Partido partido = partidoService.actualizarFecha(partidoId, fecha);
            return ResponseEntity.ok(partido);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ──────────────────────────────────────

    private String getEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
