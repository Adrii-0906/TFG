package com.golapp.controller;

import com.golapp.dto.AuthResponse;
import com.golapp.dto.LoginRequest;
import com.golapp.dto.RegistroRequest;
import com.golapp.model.Usuario;
import com.golapp.model.enums.Rol;
import com.golapp.repository.UsuarioRepository;
import com.golapp.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de autenticación.
 * Expone los endpoints públicos de registro y login.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── POST /api/auth/registro ─────────────────────

    /**
     * Registra un nuevo usuario con contraseña cifrada con BCrypt.
     *
     * @param request datos de registro
     * @return 201 Created con el token JWT y datos del usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequest request) {
        try {
            // Validar duplicados
            if (usuarioRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Ya existe un usuario con el email: " + request.getEmail()));
            }
            if (usuarioRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Ya existe un usuario con el username: " + request.getUsername()));
            }

            // Rol siempre es ORGANIZADOR (participantes acceden por código)
            Rol rol = Rol.ORGANIZADOR;

            // Crear la entidad con contraseña cifrada
            Usuario usuario = Usuario.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .nombre(request.getNombre())
                    .apellidos(request.getApellidos())
                    .rol(rol)
                    .build();

            Usuario savedUser = usuarioRepository.save(usuario);
            log.info("Usuario registrado: {} ({})", savedUser.getUsername(), savedUser.getRol());

            // Generar token JWT
            User userDetails = new User(
                    savedUser.getEmail(),
                    savedUser.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + savedUser.getRol().name()))
            );

            String token = jwtService.generateToken(
                    Map.of("rol", savedUser.getRol().name(), "userId", savedUser.getId()),
                    userDetails
            );

            // Respuesta con token
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .tipo("Bearer")
                    .userId(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .nombre(savedUser.getNombre())
                    .apellidos(savedUser.getApellidos())
                    .rol(savedUser.getRol())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error en registro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/auth/login ────────────────────────

    /**
     * Autentica un usuario existente y devuelve un token JWT.
     *
     * @param request credenciales de login (email + password)
     * @return 200 OK con el token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Autenticar con Spring Security (valida contraseña con BCrypt)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Buscar usuario en la BD
            Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

            // Generar token JWT con claims del rol y userId
            User userDetails = new User(
                    usuario.getEmail(),
                    usuario.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
            );

            String token = jwtService.generateToken(
                    Map.of("rol", usuario.getRol().name(), "userId", usuario.getId()),
                    userDetails
            );

            // Respuesta
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .tipo("Bearer")
                    .userId(usuario.getId())
                    .username(usuario.getUsername())
                    .email(usuario.getEmail())
                    .nombre(usuario.getNombre())
                    .apellidos(usuario.getApellidos())
                    .rol(usuario.getRol())
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido para: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Credenciales inválidas. Comprueba tu email y contraseña."));
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Error interno al procesar el login."));
        }
    }
}
