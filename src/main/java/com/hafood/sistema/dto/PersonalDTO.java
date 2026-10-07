package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoPersonal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalDTO {

    private Long id;

    @NotNull(message = "La sede es obligatoria")
    private Long sedeId;

    private String sedeNombre;

    @Min(value = 1, message = "El código debe ser mayor a 0")
    @Max(value = 9999, message = "El código no puede superar 9999")
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo es obligatorio")
    private TipoPersonal tipo;

    private Boolean activo;
}