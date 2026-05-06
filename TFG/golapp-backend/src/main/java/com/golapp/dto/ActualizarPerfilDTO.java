package com.golapp.dto;

import lombok.Data;

/**
 * DTO para actualizar perfil del usuario.
 */
@Data
public class ActualizarPerfilDTO {
    private String nombre;
    private String telefono;
    private String avatarUrl;
    private Integer duracionPartidoDefecto;
}
