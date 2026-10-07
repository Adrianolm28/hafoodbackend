package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoMesa;
import com.hafood.sistema.constant.FormaMesa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MesaDTO {

    private Long id;
    private Long sedeId;

    @NotNull(message = "La sección es obligatoria")
    private Long seccionId;

    private String seccionNombre;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "El nombre no puede superar los 40 caracteres")
    private String nombre;

    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    @Max(value = 50, message = "La capacidad no puede superar 50")
    private Integer capacidad;

    private FormaMesa forma;

    @Min(value = 0, message = "La posición no puede ser negativa")
    private Integer posX;

    @Min(value = 0, message = "La posición no puede ser negativa")
    private Integer posY;

    @Min(value = 1, message = "El ancho debe ser al menos 1")
    @Max(value = 6, message = "El ancho no puede superar 6")
    private Integer ancho;

    @Min(value = 1, message = "El alto debe ser al menos 1")
    @Max(value = 6, message = "El alto no puede superar 6")
    private Integer alto;

    private Boolean temporal;
    private Boolean bloqueada;
    private Boolean activa;
    private EstadoMesa estado;
    private Long cuentaId;
    private String mozoNombre;
    private Integer mozoCodigo;
    private BigDecimal total;
    private Instant abiertaEn;
    private Integer listos;
}