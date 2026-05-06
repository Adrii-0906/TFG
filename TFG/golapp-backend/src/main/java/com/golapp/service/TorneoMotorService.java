package com.golapp.service;

import com.golapp.model.Equipo;
import com.golapp.model.Partido;
import com.golapp.model.Torneo;
import com.golapp.model.enums.EstadoPartido;
import com.golapp.model.enums.EstadoTorneo;
import com.golapp.model.enums.TipoTorneo;
import com.golapp.repository.PartidoRepository;
import com.golapp.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motor de torneos.
 * Genera calendarios automáticos para torneos de tipo LIGA y ELIMINATORIA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TorneoMotorService {

    private final TorneoRepository torneoRepository;
    private final PartidoRepository partidoRepository;

    /**
     * Genera el calendario automático para un torneo.
     * - LIGA: Round-Robin (todos contra todos a una vuelta). Mínimo 4 equipos.
     * - ELIMINATORIA: Primera ronda de eliminación directa. Mínimo 4 equipos.
     *
     * @param torneoId ID del torneo
     * @param email    email del organizador autenticado
     * @return lista de partidos generados
     */
    @Transactional
    public List<Partido> generarCalendario(Long torneoId, String email) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el torneo con ID: " + torneoId));

        // Validar permisos
        if (!torneo.getOrganizador().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos para gestionar este torneo.");
        }

        // Validar que esté en BORRADOR (null = torneo legacy, se trata como BORRADOR)
        if (torneo.getEstado() != null && torneo.getEstado() != EstadoTorneo.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El torneo ya ha sido sorteado. Estado actual: " + torneo.getEstado());
        }

        // Validar que no haya partidos previos
        if (!partidoRepository.findByTorneoId(torneoId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El torneo ya tiene partidos generados.");
        }

        List<Equipo> equipos = new ArrayList<>(torneo.getEquipos());
        int n = equipos.size();

        // Validar mínimo de equipos según tipo
        if (torneo.getTipoTorneo() == TipoTorneo.LIGA && n < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se necesitan al menos 4 equipos para una Liga. Inscritos: " + n);
        }
        if (torneo.getTipoTorneo() == TipoTorneo.ELIMINATORIA && n < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se necesitan al menos 4 equipos para una Eliminatoria. Inscritos: " + n);
        }

        // Aleatorizar equipos
        Collections.shuffle(equipos);

        List<Partido> partidos;
        if (torneo.getTipoTorneo() == TipoTorneo.LIGA) {
            partidos = generarLiga(torneo, equipos);
        } else {
            partidos = generarEliminatoria(torneo, equipos);
        }

        List<Partido> guardados = partidoRepository.saveAll(partidos);

        // Marcar torneo como ACTIVO
        torneo.setEstado(EstadoTorneo.ACTIVO);
        torneoRepository.save(torneo);

        log.info("Calendario generado para torneo '{}' ({}): {} partidos",
                torneo.getNombre(), torneo.getTipoTorneo(), guardados.size());

        return guardados;
    }

    // ── LIGA: Round-Robin ────────────────────────────

    private List<Partido> generarLiga(Torneo torneo, List<Equipo> equipos) {
        List<Equipo> lista = new ArrayList<>(equipos);

        if (lista.size() % 2 != 0) {
            lista.add(null); // Equipo fantasma → descansa
        }

        int numEquipos = lista.size();
        int numJornadas = numEquipos - 1;
        int partidosPorJornada = numEquipos / 2;
        List<Partido> partidos = new ArrayList<>();

        for (int jornada = 1; jornada <= numJornadas; jornada++) {
            for (int i = 0; i < partidosPorJornada; i++) {
                Equipo local = lista.get(i);
                Equipo visitante = lista.get(numEquipos - 1 - i);

                if (local == null || visitante == null) continue;

                partidos.add(Partido.builder()
                        .torneo(torneo)
                        .equipoLocal(local)
                        .equipoVisitante(visitante)
                        .jornada(jornada)
                        .fase("Jornada " + jornada)
                        .golesLocal(0)
                        .golesVisitante(0)
                        .estado(EstadoPartido.PROGRAMADO)
                        .build());
            }

            // Rotar: fijo el primero, los demás rotan
            Equipo ultimo = lista.remove(numEquipos - 1);
            lista.add(1, ultimo);
        }

        return partidos;
    }

    // ── ELIMINATORIA: Primera ronda ─────────────────

    private List<Partido> generarEliminatoria(Torneo torneo, List<Equipo> equipos) {
        List<Partido> partidos = new ArrayList<>();
        String fase = obtenerNombreFase(equipos.size());

        for (int i = 0; i < equipos.size() - 1; i += 2) {
            partidos.add(Partido.builder()
                    .torneo(torneo)
                    .equipoLocal(equipos.get(i))
                    .equipoVisitante(equipos.get(i + 1))
                    .jornada(1)
                    .fase(fase)
                    .golesLocal(0)
                    .golesVisitante(0)
                    .estado(EstadoPartido.PROGRAMADO)
                    .build());
        }

        return partidos;
    }

    private String obtenerNombreFase(int numEquipos) {
        if (numEquipos <= 2) return "Final";
        if (numEquipos <= 4) return "Semifinal";
        if (numEquipos <= 8) return "Cuartos de Final";
        if (numEquipos <= 16) return "Octavos de Final";
        return "Ronda " + numEquipos / 2;
    }
}
