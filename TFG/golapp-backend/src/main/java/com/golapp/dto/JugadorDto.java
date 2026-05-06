package com.golapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la creación de jugadores.
 * Se usa en el controlador para recibir datos del frontend
 * sin exponer la entidad JPA directamente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JugadorDto {

    @NotBlank(message = "El nombre del jugador es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombreCompleto;

    private Integer dorsal;

    @Size(max = 50, message = "La posición no puede superar los 50 caracteres")
    private String posicion;
}
