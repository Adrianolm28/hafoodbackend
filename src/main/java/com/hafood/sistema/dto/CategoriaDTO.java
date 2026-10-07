package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoCategoria;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private TipoCategoria tipo;
    private Boolean activo;
}