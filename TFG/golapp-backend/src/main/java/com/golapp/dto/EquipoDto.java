package com.golapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la creación y actualización de equipos.
 * Se usa en el controlador para recibir datos del frontend
 * sin exponer la entidad JPA directamente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipoDto {

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La URL del escudo no puede superar los 500 caracteres")
    private String escudoUrl;

    @Size(max = 100, message = "El nombre del delegado no puede superar los 100 caracteres")
    private String nombreDelegado;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String telefonoContacto;

    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String emailContacto;
}
