package com.golapp.service;

import com.golapp.model.Usuario;
import com.golapp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de usuarios.
 * Contiene la lógica de negocio relacionada con el registro y búsqueda de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Registro ────────────────────────────────────

    /**
     * Registra un nuevo usuario en la plataforma.
     * Valida que no exista otro usuario con el mismo email o username.
     * Cifra la contraseña con BCrypt antes de persistirla.
     *
     * @param usuario entidad Usuario a registrar
     * @return el usuario persistido con su ID generado
     * @throws IllegalArgumentException si el email o username ya están en uso
     */
    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        // Comprobar duplicado de email
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con el email: " + usuario.getEmail());
        }

        // Comprobar duplicado de username
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con el username: " + usuario.getUsername());
        }

        // Cifrar contraseña con BCrypt
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        log.info("Registrando nuevo usuario: {} ({})", usuario.getUsername(), usuario.getRol());
        return usuarioRepository.save(usuario);
    }

    // ── Búsquedas ───────────────────────────────────

    /**
     * Busca un usuario por su ID.
     *
     * @param id identificador del usuario
     * @return el usuario encontrado
     * @throws IllegalArgumentException si no se encuentra el usuario
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario con ID: " + id));
    }

    /**
     * Busca un usuario por su email.
     *
     * @param email email del usuario
     * @return el usuario encontrado
     * @throws IllegalArgumentException si no se encuentra el usuario
     */
    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario con email: " + email));
    }

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @return lista de todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }
}
