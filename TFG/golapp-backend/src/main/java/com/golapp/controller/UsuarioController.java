package com.golapp.controller;

import com.golapp.dto.ActualizarPerfilDTO;
import com.golapp.dto.CambiarPasswordDTO;
import com.golapp.model.Usuario;
import com.golapp.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la gestión del perfil de usuario.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * GET /api/usuarios/me — Datos del usuario autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<?> obtenerPerfil() {
        String email = getEmailAutenticado();
        return usuarioRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/usuarios/me — Actualizar perfil.
     */
    @PutMapping("/me")
    public ResponseEntity<?> actualizarPerfil(@RequestBody ActualizarPerfilDTO dto) {
        String email = getEmailAutenticado();
        return usuarioRepository.findByEmail(email).map(usuario -> {
            if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
                usuario.setNombre(dto.getNombre());
            }
            if (dto.getTelefono() != null) {
                usuario.setTelefono(dto.getTelefono());
            }
            if (dto.getAvatarUrl() != null) {
                usuario.setAvatarUrl(dto.getAvatarUrl());
            }
            if (dto.getDuracionPartidoDefecto() != null && dto.getDuracionPartidoDefecto() > 0) {
                usuario.setDuracionPartidoDefecto(dto.getDuracionPartidoDefecto());
            }
            usuarioRepository.save(usuario);
            return ResponseEntity.ok(usuario);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/usuarios/me/password — Cambiar contraseña.
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> cambiarPassword(@RequestBody CambiarPasswordDTO dto) {
        String email = getEmailAutenticado();
        return usuarioRepository.findByEmail(email).map(usuario -> {
            if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "La contraseña actual es incorrecta."));
            }
            if (dto.getNuevaPassword() == null || dto.getNuevaPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "La nueva contraseña debe tener al menos 6 caracteres."));
            }
            usuario.setPassword(passwordEncoder.encode(dto.getNuevaPassword()));
            usuarioRepository.save(usuario);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/usuarios/me/avatar — Subir avatar.
     */
    @PostMapping("/me/avatar")
    public ResponseEntity<?> subirAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten archivos de imagen."));
        }
        String email = getEmailAutenticado();
        return usuarioRepository.findByEmail(email).map(usuario -> {
            try {
                Path dirPath = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
                Files.createDirectories(dirPath);

                String ext = file.getOriginalFilename() != null
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'))
                        : ".jpg";
                String filename = UUID.randomUUID() + ext;
                Path filePath = dirPath.resolve(filename);
                file.transferTo(filePath.toFile());

                String avatarUrl = "/uploads/avatars/" + filename;
                usuario.setAvatarUrl(avatarUrl);
                usuarioRepository.save(usuario);

                return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Error al guardar el archivo."));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private String getEmailAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
