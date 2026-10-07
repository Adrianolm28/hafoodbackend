package com.hafood.sistema.dto;

import com.hafood.sistema.constant.TipoCategoria;

import java.math.BigDecimal;
import java.util.List;

public record CartaPublicaDTO(
        String sede,
        String nombre,
        boolean mostrarImagenes,
        String fondoMovilUrl,
        String fondoEscritorioUrl,
        String logoUrl,
        List<Grupo> grupos
) {

    public record Grupo(String nombre, TipoCategoria tipo, List<Producto> productos) {
    }

    public record Producto(String nombre, String descripcion, BigDecimal precio, boolean agotado, String imagenUrl) {
    }
}
