package com.novabank.dto;

import com.novabank.validation.ValidDni;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 150, message = "Los apellidos no pueden superar 150 caracteres")
        String apellidos,

        @NotBlank(message = "El DNI es obligatorio")
        @ValidDni(message = "El DNI no es valido")
        @Schema(
                description = "DNI español válido. Debe tener 8 dígitos y una letra correcta.",
                example = "12345678Z"
        )
        String dni,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato valido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String email,

        @NotBlank(message = "El telefono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
        String telefono
) {
}
