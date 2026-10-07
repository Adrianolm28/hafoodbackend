package com.hafood.sistema.mapper;

import com.hafood.sistema.domain.estructura.Personal;
import com.hafood.sistema.dto.PersonalDTO;

public final class PersonalMapper {

    private PersonalMapper() {
    }

    public static PersonalDTO toDTO(Personal personal) {
        if (personal == null) return null;

        PersonalDTO dto = PersonalDTO.builder()
                .id(personal.getId())
                .codigo(personal.getCodigo())
                .nombre(personal.getNombre())
                .tipo(personal.getTipo())
                .activo(personal.getActivo())
                .build();

        if (personal.getSede() != null) {
            dto.setSedeId(personal.getSede().getId());
            dto.setSedeNombre(personal.getSede().getNombre());
        }

        return dto;
    }
}