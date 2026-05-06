package com.golapp.service;

import com.golapp.dto.EquipoDto;
import com.golapp.dto.EquipoExportDto;
import com.golapp.dto.JugadorExportDto;
import com.golapp.model.Equipo;
import com.golapp.model.Jugador;
import com.golapp.model.Usuario;
import com.golapp.repository.EquipoRepository;
import com.golapp.repository.UsuarioRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * Servicio para la gestión de equipos.
 * Contiene la lógica de negocio para crear, listar y eliminar equipos
 * asociados al organizador autenticado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private String generarCodigoAcceso() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            codigo = sb.toString();
        } while (equipoRepository.findByCodigoAcceso(codigo).isPresent());
        return codigo;
    }

    // ── Creación ────────────────────────────────────

    /**
     * Crea un nuevo equipo asociado al organizador autenticado.
     */
    @Transactional
    public Equipo crearEquipo(EquipoDto dto, String emailOrganizador) {
        Usuario organizador = usuarioRepository.findByEmail(emailOrganizador)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario con email: " + emailOrganizador));

        Equipo equipo = Equipo.builder()
                .nombre(dto.getNombre())
                .escudoUrl(dto.getEscudoUrl())
                .nombreDelegado(dto.getNombreDelegado())
                .telefonoContacto(dto.getTelefonoContacto())
                .emailContacto(dto.getEmailContacto())
                .codigoAcceso(generarCodigoAcceso())
                .usuario(organizador)
                .build();

        log.info("Creando equipo '{}' por organizador '{}'", equipo.getNombre(), organizador.getUsername());
        return equipoRepository.save(equipo);
    }

    // ── Consultas ───────────────────────────────────

    /**
     * Obtiene todos los equipos del organizador autenticado.
     *
     * @param emailOrganizador email del usuario autenticado
     * @return lista de equipos del organizador
     */
    @Transactional(readOnly = true)
    public List<Equipo> obtenerMisEquipos(String emailOrganizador) {
        return equipoRepository.findByUsuarioEmail(emailOrganizador);
    }

    /**
     * Busca un equipo por su ID.
     *
     * @param id identificador del equipo
     * @return el equipo encontrado
     * @throws IllegalArgumentException si no se encuentra el equipo
     */
    @Transactional(readOnly = true)
    public Equipo buscarPorId(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún equipo con ID: " + id));
    }

    /**
     * Obtiene todos los equipos registrados.
     *
     * @return lista de todos los equipos
     */
    @Transactional(readOnly = true)
    public List<Equipo> obtenerTodos() {
        return equipoRepository.findAll();
    }

    // ── Eliminación ─────────────────────────────────

    /**
     * Elimina un equipo verificando que pertenezca al organizador.
     *
     * @param equipoId         ID del equipo a eliminar
     * @param emailOrganizador email del organizador autenticado
     * @throws IllegalArgumentException si el equipo no existe o no pertenece al organizador
     */
    @Transactional
    public void eliminarEquipo(Long equipoId, String emailOrganizador) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún equipo con ID: " + equipoId));

        if (equipo.getUsuario() == null || !equipo.getUsuario().getEmail().equals(emailOrganizador)) {
            throw new IllegalArgumentException("No tienes permisos para eliminar este equipo.");
        }

        log.info("Eliminando equipo '{}' (ID: {})", equipo.getNombre(), equipoId);
        equipoRepository.delete(equipo);
    }

    // ── Exportación / Importación ───────────────────

    /**
     * Exporta los equipos del organizador, incluyendo sus jugadores.
     * Si idsSeleccionados no es nulo ni está vacío, filtra la exportación a esos IDs específicos.
     */
    @Transactional(readOnly = true)
    public List<EquipoExportDto> exportarMisEquipos(String emailOrganizador, List<Long> idsSeleccionados) {
        List<Equipo> equipos = equipoRepository.findByUsuarioEmail(emailOrganizador);
        
        if (idsSeleccionados != null && !idsSeleccionados.isEmpty()) {
            equipos = equipos.stream()
                    .filter(e -> idsSeleccionados.contains(e.getId()))
                    .collect(Collectors.toList());
        }
        
        return equipos.stream().map(equipo -> EquipoExportDto.builder()
                .nombre(equipo.getNombre())
                .escudoUrl(equipo.getEscudoUrl())
                .nombreDelegado(equipo.getNombreDelegado())
                .telefonoContacto(equipo.getTelefonoContacto())
                .emailContacto(equipo.getEmailContacto())
                .jugadores(equipo.getJugadores().stream().map(jugador -> JugadorExportDto.builder()
                        .nombreCompleto(jugador.getNombreCompleto())
                        .dorsal(jugador.getDorsal())
                        .posicion(jugador.getPosicion())
                        .build()).collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
    }

    /**
     * Importa una lista de equipos, creando copias nuevas asociadas al organizador actual.
     */
    @Transactional
    public void importarEquipos(List<EquipoExportDto> dtos, String emailOrganizador) {
        Usuario usuario = usuarioRepository.findByEmail(emailOrganizador)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        for (EquipoExportDto dto : dtos) {
            Equipo nuevoEquipo = Equipo.builder()
                    .nombre(dto.getNombre())
                    .escudoUrl(dto.getEscudoUrl())
                    .nombreDelegado(dto.getNombreDelegado())
                    .telefonoContacto(dto.getTelefonoContacto())
                    .emailContacto(dto.getEmailContacto())
                    .usuario(usuario)
                    .codigoAcceso(generarCodigoAcceso())
                    .build();

            // Añadir los jugadores al nuevo equipo
            if (dto.getJugadores() != null) {
                for (JugadorExportDto jDto : dto.getJugadores()) {
                    Jugador nuevoJugador = Jugador.builder()
                            .nombreCompleto(jDto.getNombreCompleto())
                            .dorsal(jDto.getDorsal())
                            .posicion(jDto.getPosicion())
                            .equipo(nuevoEquipo) // Establecer la relación
                            .build();
                    nuevoEquipo.getJugadores().add(nuevoJugador);
                }
            }

            equipoRepository.save(nuevoEquipo);
        }
        log.info("Importados {} equipos para el usuario {}", dtos.size(), emailOrganizador);
    }
}
