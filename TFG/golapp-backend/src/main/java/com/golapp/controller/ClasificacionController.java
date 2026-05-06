package com.golapp.controller;

import com.golapp.dto.ClasificacionDTO;
import com.golapp.service.ClasificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la clasificación de torneos.
 * Expone el endpoint para consultar la tabla de posiciones calculada dinámicamente.
 */
@RestController
@RequestMapping("/api/clasificacion")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ClasificacionController {

    private final ClasificacionService clasificacionService;

    // ── GET /api/clasificacion/{torneoId} ───────────

    /**
     * Devuelve la tabla de clasificación de un torneo, ordenada por:
     *   1.º Puntos (mayor primero)
     *   2.º Diferencia de goles
     *   3.º Goles a favor
     *
     * @param torneoId ID del torneo
     * @return 200 OK con la lista de ClasificacionDTO ordenada, o 404 si el torneo no existe
     */
    @GetMapping("/{torneoId}")
    public ResponseEntity<?> obtenerClasificacion(@PathVariable Long torneoId) {
        try {
            List<ClasificacionDTO> clasificacion = clasificacionService.obtenerClasificacion(torneoId);
            return ResponseEntity.ok(clasificacion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
