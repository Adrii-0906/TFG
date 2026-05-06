package com.golapp.controller;

import com.golapp.dto.EquipoDto;
import com.golapp.model.Equipo;
import com.golapp.service.EquipoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de equipos.
 *
 * Endpoints:
 * - POST   /api/equipos         → Crea un equipo para el organizador autenticado
 * - GET    /api/equipos/mis-equipos → Lista los equipos del organizador autenticado
 * - GET    /api/equipos/{id}     → Obtiene un equipo por ID (público)
 * - DELETE /api/equipos/{id}     → Elimina un equipo (solo su dueño)
 */
@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoService equipoService;

    // ── POST /api/equipos ──────────────────────────

    /**
     * Crea un nuevo equipo asociado al organizador autenticado.
     * El email se extrae automáticamente del token JWT via SecurityContext.
     */
    @PostMapping
    public ResponseEntity<?> crearEquipo(@Valid @RequestBody EquipoDto dto) {
        try {
            String emailOrganizador = getEmailAutenticado();
            Equipo nuevoEquipo = equipoService.crearEquipo(dto, emailOrganizador);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEquipo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/equipos/mis-equipos ───────────────

    /**
     * Lista los equipos del organizador autenticado.
     * El email se extrae automáticamente del token JWT.
     */
    @GetMapping("/mis-equipos")
    public ResponseEntity<List<Equipo>> obtenerMisEquipos() {
        String emailOrganizador = getEmailAutenticado();
        List<Equipo> equipos = equipoService.obtenerMisEquipos(emailOrganizador);
        return ResponseEntity.ok(equipos);
    }

    // ── GET /api/equipos/{id} ──────────────────────

    /**
     * Obtiene un equipo por su ID (acceso público para lectura).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            Equipo equipo = equipoService.buscarPorId(id);
            return ResponseEntity.ok(equipo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/equipos/{id} ───────────────────

    /**
     * Elimina un equipo. Solo el organizador dueño puede eliminarlo.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarEquipo(@PathVariable Long id) {
        try {
            String emailOrganizador = getEmailAutenticado();
            equipoService.eliminarEquipo(id, emailOrganizador);
            return ResponseEntity.ok(Map.of("mensaje", "Equipo eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/equipos/exportar ──────────────────

    /**
     * Exporta los equipos del organizador a formato JSON.
     * Permite filtrar por IDs específicos.
     */
    @GetMapping("/exportar")
    public ResponseEntity<List<com.golapp.dto.EquipoExportDto>> exportarEquipos(
            @RequestParam(required = false) List<Long> ids) {
        String emailOrganizador = getEmailAutenticado();
        List<com.golapp.dto.EquipoExportDto> exportacion = equipoService.exportarMisEquipos(emailOrganizador, ids);
        return ResponseEntity.ok(exportacion);
    }

    // ── POST /api/equipos/importar ─────────────────

    /**
     * Importa una lista de equipos en formato JSON y los asocia al organizador.
     */
    @PostMapping("/importar")
    public ResponseEntity<?> importarEquipos(@RequestBody List<com.golapp.dto.EquipoExportDto> dtos) {
        try {
            String emailOrganizador = getEmailAutenticado();
            equipoService.importarEquipos(dtos, emailOrganizador);
            return ResponseEntity.ok(Map.of("mensaje", "Equipos importados correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper privado ─────────────────────────────

    /**
     * Extrae el email del usuario autenticado desde el SecurityContext.
     * El JwtAuthenticationFilter almacena el email como principal (getName()).
     */
    private String getEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
