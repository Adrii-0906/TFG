package com.golapp.controller;

import com.golapp.model.Equipo;
import com.golapp.model.Jugador;
import com.golapp.model.Partido;
import com.golapp.repository.EquipoRepository;
import com.golapp.repository.JugadorRepository;
import com.golapp.repository.PartidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API pública para participantes (acceso por código de equipo, sin JWT).
 */
@RestController
@RequestMapping("/api/public/participantes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ParticipanteController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final PartidoRepository partidoRepository;

    /**
     * GET /api/public/participantes/equipo/{codigo}
     * Datos básicos del equipo por código de acceso.
     */
    @GetMapping("/equipo/{codigo}")
    public ResponseEntity<?> obtenerEquipo(@PathVariable String codigo) {
        return equipoRepository.findByCodigoAcceso(codigo.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/public/participantes/equipo/{codigo}/partidos
     * Partidos donde participa este equipo.
     */
    @GetMapping("/equipo/{codigo}/partidos")
    public ResponseEntity<?> obtenerPartidos(@PathVariable String codigo) {
        return equipoRepository.findByCodigoAcceso(codigo.toUpperCase()).map(equipo -> {
            List<Partido> locales = partidoRepository.findByEquipoLocalId(equipo.getId());
            List<Partido> visitantes = partidoRepository.findByEquipoVisitanteId(equipo.getId());
            locales.addAll(visitantes);
            locales.sort((a, b) -> {
                if (a.getFechaPartido() == null && b.getFechaPartido() == null) return 0;
                if (a.getFechaPartido() == null) return 1;
                if (b.getFechaPartido() == null) return -1;
                return a.getFechaPartido().compareTo(b.getFechaPartido());
            });
            return ResponseEntity.ok(locales);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/public/participantes/equipo/{codigo}/plantilla
     * Jugadores del equipo.
     */
    @GetMapping("/equipo/{codigo}/plantilla")
    public ResponseEntity<?> obtenerPlantilla(@PathVariable String codigo) {
        return equipoRepository.findByCodigoAcceso(codigo.toUpperCase()).map(equipo -> {
            List<Jugador> jugadores = jugadorRepository.findByEquipoId(equipo.getId());
            return ResponseEntity.ok(jugadores);
        }).orElse(ResponseEntity.notFound().build());
    }
}
