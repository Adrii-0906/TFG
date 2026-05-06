package com.golapp.dto;

import com.golapp.model.enums.Rol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta tras un login exitoso.
 * Incluye el token JWT y la información básica del usuario autenticado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tipo;
    private Long userId;
    private String username;
    private String email;
    private String nombre;
    private String apellidos;
    private Rol rol;
}
