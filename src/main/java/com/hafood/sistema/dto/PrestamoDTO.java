package com.hafood.sistema.dto;

import com.hafood.sistema.constant.EstadoPrestamo;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PrestamoDTO {
    private Long id;
    private Long traspasoId;
    private EstadoPrestamo estado;
    private LocalDate fechaLimite;
    private boolean vencido;
    private LocalDateTime fecha;
    private Long prestamistaSedeId;
    private String prestamistaSedeNombre;
    private Long prestatarioSedeId;
    private String prestatarioSedeNombre;
    private Long origenId;
    private String origenNombre;
    private Long destinoId;
    private String destinoNombre;
    private String usuarioNombre;
    private String observacion;
    private List<PrestamoDetalleDTO> detalles;
}