package com.golapp.service;

import com.golapp.dto.ClasificacionDTO;
import com.golapp.model.Equipo;
import com.golapp.model.Partido;
import com.golapp.model.Torneo;
import com.golapp.model.enums.EstadoPartido;
import com.golapp.repository.PartidoRepository;
import com.golapp.repository.TorneoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para el cálculo dinámico de la clasificación de un torneo.
 *
 * ─── Sistema de puntos ───
 *   Victoria:  3 puntos
 *   Empate:    1 punto
 *   Derrota:   0 puntos
 *
 * ─── Criterios de ordenación ───
 *   1.º Puntos (mayor primero)
 *   2.º Diferencia de goles (mayor primero)
 *   3.º Goles a favor (mayor primero)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClasificacionService {

    private final PartidoRepository partidoRepository;
    private final TorneoRepository torneoRepository;

    /**
     * Calcula y devuelve la clasificación actual de un torneo.
     * Solo se tienen en cuenta los partidos con estado {@link EstadoPartido#FINALIZADO}.
     *
     * @param torneoId ID del torneo
     * @return lista de {@link ClasificacionDTO} ordenada por posición en la tabla
     * @throws IllegalArgumentException si el torneo no existe
     */
    @Transactional(readOnly = true)
    public List<ClasificacionDTO> obtenerClasificacion(Long torneoId) {
        // Verificar que el torneo existe
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún torneo con ID: " + torneoId));

        // Obtener solo los partidos FINALIZADOS
        List<Partido> partidosFinalizados = partidoRepository
                .findByTorneoIdAndEstado(torneoId, EstadoPartido.FINALIZADO);

        // Inicializar mapa con TODOS los equipos inscritos (incluso los que aún no han jugado)
        Map<Long, ClasificacionDTO> clasificacionMap = new LinkedHashMap<>();

        for (Equipo equipo : torneo.getEquipos()) {
            clasificacionMap.put(equipo.getId(), ClasificacionDTO.builder()
                    .equipoId(equipo.getId())
                    .equipoNombre(equipo.getNombre())
                    .escudoUrl(equipo.getEscudoUrl())
                    .build());
        }

        // Procesar cada partido finalizado
        for (Partido partido : partidosFinalizados) {
            Long localId = partido.getEquipoLocal().getId();
            Long visitanteId = partido.getEquipoVisitante().getId();

            ClasificacionDTO statsLocal = clasificacionMap.get(localId);
            ClasificacionDTO statsVisitante = clasificacionMap.get(visitanteId);

            if (statsLocal == null || statsVisitante == null) {
                log.warn("Partido ID {} tiene equipos no inscritos en el torneo. Se omite.", partido.getId());
                continue;
            }

            int golesLocal = partido.getGolesLocal();
            int golesVisitante = partido.getGolesVisitante();

            // ── Actualizar estadísticas del equipo LOCAL ──
            statsLocal.setPartidosJugados(statsLocal.getPartidosJugados() + 1);
            statsLocal.setGolesAFavor(statsLocal.getGolesAFavor() + golesLocal);
            statsLocal.setGolesEnContra(statsLocal.getGolesEnContra() + golesVisitante);

            // ── Actualizar estadísticas del equipo VISITANTE ──
            statsVisitante.setPartidosJugados(statsVisitante.getPartidosJugados() + 1);
            statsVisitante.setGolesAFavor(statsVisitante.getGolesAFavor() + golesVisitante);
            statsVisitante.setGolesEnContra(statsVisitante.getGolesEnContra() + golesLocal);

            // ── Asignar puntos según resultado ──
            if (golesLocal > golesVisitante) {
                // Victoria local
                statsLocal.setVictorias(statsLocal.getVictorias() + 1);
                statsLocal.setPuntos(statsLocal.getPuntos() + 3);
                statsVisitante.setDerrotas(statsVisitante.getDerrotas() + 1);

            } else if (golesVisitante > golesLocal) {
                // Victoria visitante
                statsVisitante.setVictorias(statsVisitante.getVictorias() + 1);
                statsVisitante.setPuntos(statsVisitante.getPuntos() + 3);
                statsLocal.setDerrotas(statsLocal.getDerrotas() + 1);

            } else {
                // Empate
                statsLocal.setEmpates(statsLocal.getEmpates() + 1);
                statsLocal.setPuntos(statsLocal.getPuntos() + 1);
                statsVisitante.setEmpates(statsVisitante.getEmpates() + 1);
                statsVisitante.setPuntos(statsVisitante.getPuntos() + 1);
            }
        }

        // Ordenar la clasificación usando el Comparable de ClasificacionDTO
        List<ClasificacionDTO> clasificacion = clasificacionMap.values().stream()
                .sorted()
                .collect(Collectors.toList());

        log.info("Clasificación calculada para torneo '{}': {} equipos, {} partidos procesados",
                torneo.getNombre(), clasificacion.size(), partidosFinalizados.size());

        return clasificacion;
    }
}
