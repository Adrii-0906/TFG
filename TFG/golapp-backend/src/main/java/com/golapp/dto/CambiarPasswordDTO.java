package com.golapp.dto;

import lombok.Data;

/**
 * DTO para cambio de contraseña.
 */
@Data
public class CambiarPasswordDTO {
    private String passwordActual;
    private String nuevaPassword;
}
