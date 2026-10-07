package com.hafood.sistema.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import com.hafood.sistema.constant.TipoTraspaso;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TraspasoDTO {
    private Long id;
    private Long origenId;
    private String origenNombre;
    private Long origenSedeId;
    private String origenSedeNombre;
    private Long destinoId;
    private String destinoNombre;
    private Long destinoSedeId;
    private String destinoSedeNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private String observacion;
    private LocalDateTime fecha;
    private TipoTraspaso tipo;
    private Long prestamoId;
    private List<TraspasoDetalleDTO> detalles;
}