package com.hafood.sistema.mapper;

import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.dto.CartaDTO;

public final class CartaMapper {

    private CartaMapper() {
    }

    public static CartaDTO toDTO(Carta carta) {
        return new CartaDTO(
                carta.getId(),
                carta.getSede().getId(),
                carta.getSede().getNombre(),
                carta.getNombre(),
                carta.getCodigo(),
                carta.isActivo(),
                carta.isMostrarImagenes(),
                ImagenUrl.de(carta.getFondoMovil()),
                ImagenUrl.de(carta.getFondoEscritorio()),
                ImagenUrl.de(carta.getLogo())
        );
    }
}
