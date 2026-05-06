package com.golapp.service;

import com.golapp.dto.PartidoDTO;
import com.golapp.model.Equipo;
import com.golapp.model.Partido;
import com.golapp.model.Torneo;
import com.golapp.model.enums.EstadoPartido;
import com.golapp.model.enums.TipoTorneo;
import com.golapp.repository.EquipoRepository;
import com.golapp.repository.PartidoRepository;
import com.golapp.repository.TorneoRepository;
import com.golapp.repository.EventoPartidoRepository;
import com.golapp.repository.JugadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio para la gestión de partidos.
 * Incluye lógica de negocio como la generación de calendario (Round-Robin)
 * y el registro de resultados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final TorneoRepository torneoRepository;
    private final EquipoRepository equipoRepository;
    private final EventoPartidoRepository eventoPartidoRepository;
    private final JugadorRepository jugadorRepository;

    // ── Creación manual de un partido ───────────────

    /**
     * Crea un partido manualmente dentro de un torneo.
     * Valida que ambos equipos estén inscritos en el torneo y que no sean el mismo.
     *
     * @param torneoId ID del torneo
     * @param dto      datos del partido (equipoLocalId, equipoVisitanteId, fechaPartido)
     * @param email    email del organizador autenticado
     * @return el partido creado
     */
    @Transactional
    public Partido crearPartido(Long torneoId, PartidoDTO dto, String email) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún torneo con ID: " + torneoId));

        // Validar que el torneo pertenece al organizador
        if (!torneo.getOrganizador().getEmail().equals(email)) {
            throw new IllegalArgumentException("No tienes permisos para gestionar este torneo.");
        }

        // Validar que los equipos no sean el mismo
        if (dto.getEquipoLocalId().equals(dto.getEquipoVisitanteId())) {
            throw new IllegalArgumentException("El equipo local y visitante no pueden ser el mismo.");
        }

        // Obtener los equipos
        Equipo equipoLocal = equipoRepository.findById(dto.getEquipoLocalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el equipo local con ID: " + dto.getEquipoLocalId()));

        Equipo equipoVisitante = equipoRepository.findById(dto.getEquipoVisitanteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el equipo visitante con ID: " + dto.getEquipoVisitanteId()));

        // Validar que ambos equipos están inscritos en el torneo
        List<Long> idsInscritos = torneo.getEquipos().stream().map(Equipo::getId).toList();
        if (!idsInscritos.contains(equipoLocal.getId())) {
            throw new IllegalArgumentException(
                    "El equipo '" + equipoLocal.getNombre() + "' no está inscrito en este torneo.");
        }
        if (!idsInscritos.contains(equipoVisitante.getId())) {
            throw new IllegalArgumentException(
                    "El equipo '" + equipoVisitante.getNombre() + "' no está inscrito en este torneo.");
        }

        // Parsear fecha
        LocalDateTime fecha = null;
        if (dto.getFechaPartido() != null && !dto.getFechaPartido().isBlank()) {
            fecha = LocalDateTime.parse(dto.getFechaPartido());
        }

        Partido partido = Partido.builder()
                .torneo(torneo)
                .equipoLocal(equipoLocal)
                .equipoVisitante(equipoVisitante)
                .fechaPartido(fecha)
                .golesLocal(0)
                .golesVisitante(0)
                .estado(EstadoPartido.PROGRAMADO)
                .build();

        Partido guardado = partidoRepository.save(partido);
        log.info("Partido creado: {} vs {} en torneo '{}'",
                equipoLocal.getNombre(), equipoVisitante.getNombre(), torneo.getNombre());

        return guardado;
    }

    // ── Generación de Calendario ────────────────────

    /**
     * Genera un calendario automático Round-Robin para todos los equipos inscritos en un torneo de tipo LIGA.
     */
    @Transactional
    public List<Partido> generarCalendario(Long torneoId) {
        Torneo torneo = torneoRepository.findById(torneoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún torneo con ID: " + torneoId));

        if (torneo.getTipoTorneo() != TipoTorneo.LIGA) {
            throw new IllegalArgumentException(
                    "La generación automática de calendario solo está disponible para torneos de tipo LIGA. "
                            + "El torneo '" + torneo.getNombre() + "' es de tipo " + torneo.getTipoTorneo());
        }

        List<Partido> partidosExistentes = partidoRepository.findByTorneoId(torneoId);
        if (!partidosExistentes.isEmpty()) {
            throw new IllegalStateException(
                    "El torneo '" + torneo.getNombre() + "' ya tiene " + partidosExistentes.size()
                            + " partidos generados. Elimine los existentes antes de regenerar el calendario.");
        }

        List<Equipo> equipos = torneo.getEquipos();
        if (equipos.size() < 2) {
            throw new IllegalArgumentException(
                    "Se necesitan al menos 2 equipos inscritos para generar el calendario. "
                            + "Equipos inscritos actualmente: " + equipos.size());
        }

        List<Partido> partidos = generarRoundRobin(torneo, equipos);
        List<Partido> partidosGuardados = partidoRepository.saveAll(partidos);

        log.info("Calendario generado para torneo '{}': {} partidos en {} jornadas",
                torneo.getNombre(), partidosGuardados.size(),
                partidosGuardados.stream().mapToInt(Partido::getJornada).max().orElse(0));

        return partidosGuardados;
    }

    private List<Partido> generarRoundRobin(Torneo torneo, List<Equipo> equiposOriginal) {
        List<Equipo> equipos = new ArrayList<>(equiposOriginal);

        if (equipos.size() % 2 != 0) {
            equipos.add(null);
        }

        int numEquipos = equipos.size();
        int numJornadas = numEquipos - 1;
        int partidosPorJornada = numEquipos / 2;

        List<Partido> partidos = new ArrayList<>();

        for (int jornada = 1; jornada <= numJornadas; jornada++) {
            for (int i = 0; i < partidosPorJornada; i++) {
                Equipo local = equipos.get(i);
                Equipo visitante = equipos.get(numEquipos - 1 - i);

                if (local == null || visitante == null) {
                    continue;
                }

                partidos.add(Partido.builder()
                        .torneo(torneo)
                        .equipoLocal(local)
                        .equipoVisitante(visitante)
                        .jornada(jornada)
                        .golesLocal(0)
                        .golesVisitante(0)
                        .estado(EstadoPartido.PROGRAMADO)
                        .build());
            }

            Equipo ultimo = equipos.remove(numEquipos - 1);
            equipos.add(1, ultimo);
        }

        return partidos;
    }

    // ── Registro de Resultados ──────────────────────

    /**
     * Registra o actualiza el resultado de un partido y cambia su estado a FINALIZADO.
     */
    @Transactional
    public Partido registrarResultado(Long partidoId, com.golapp.dto.ResultadoDTO dto) {
        if (dto.getGolesLocal() < 0 || dto.getGolesVisitante() < 0) {
            throw new IllegalArgumentException("Los goles no pueden ser negativos");
        }

        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún partido con ID: " + partidoId));

        if (partido.getEstado() == EstadoPartido.FINALIZADO) {
            throw new IllegalStateException(
                    "El partido con ID " + partidoId + " ya está FINALIZADO. "
                            + "Resultado actual: " + partido.getResultado());
        }

        partido.setGolesLocal(dto.getGolesLocal());
        partido.setGolesVisitante(dto.getGolesVisitante());
        partido.setEstado(EstadoPartido.FINALIZADO);

        Partido partidoGuardado = partidoRepository.save(partido);

        // Procesar eventos si los hay
        if (dto.getEventos() != null && !dto.getEventos().isEmpty()) {
            for (com.golapp.dto.EventoDto eventoDto : dto.getEventos()) {
                com.golapp.model.Jugador jugador = jugadorRepository.findById(eventoDto.getJugadorId())
                        .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado con ID: " + eventoDto.getJugadorId()));

                com.golapp.model.EventoPartido evento = com.golapp.model.EventoPartido.builder()
                        .partido(partidoGuardado)
                        .jugador(jugador)
                        .tipoEvento(eventoDto.getTipoEvento())
                        .minuto(eventoDto.getMinuto())
                        .build();

                eventoPartidoRepository.save(evento);

                // Actualizar estadísticas del jugador
                if ("GOL".equalsIgnoreCase(eventoDto.getTipoEvento())) {
                    jugador.setGoles(jugador.getGoles() + 1);
                } else if ("AMARILLA".equalsIgnoreCase(eventoDto.getTipoEvento())) {
                    jugador.setTarjetasAmarillas(jugador.getTarjetasAmarillas() + 1);
                } else if ("ROJA".equalsIgnoreCase(eventoDto.getTipoEvento())) {
                    jugador.setTarjetasRojas(jugador.getTarjetasRojas() + 1);
                }
                jugadorRepository.save(jugador);
            }
        }

        log.info("Resultado registrado para partido ID {}: {} {} - {} {}",
                partidoId,
                partido.getEquipoLocal().getNombre(), dto.getGolesLocal(),
                dto.getGolesVisitante(), partido.getEquipoVisitante().getNombre());

        return partidoGuardado;
    }

    // ── Búsquedas ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<Partido> obtenerPorTorneo(Long torneoId) {
        return partidoRepository.findByTorneoIdOrderByFechaPartidoAsc(torneoId);
    }

    @Transactional(readOnly = true)
    public List<Partido> obtenerPartidosDelOrganizador(String email) {
        return partidoRepository.findByOrganizadorEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Partido> obtenerPorTorneoYEstado(Long torneoId, EstadoPartido estado) {
        return partidoRepository.findByTorneoIdAndEstado(torneoId, estado);
    }

    /**
     * Actualiza la fecha/hora de un partido existente.
     */
    @Transactional
    public Partido actualizarFecha(Long partidoId, java.time.LocalDateTime fecha) {
        Partido partido = partidoRepository.findById(partidoId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado con ID: " + partidoId));
        partido.setFechaPartido(fecha);
        return partidoRepository.save(partido);
    }
}
