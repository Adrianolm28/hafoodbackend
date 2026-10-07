package com.hafood.sistema.mapper;

import com.hafood.sistema.constant.EstadoMesa;
import com.hafood.sistema.domain.estructura.Mesa;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaMesa;
import com.hafood.sistema.dto.MesaDTO;

public final class MesaMapper {

    private MesaMapper() {
    }

    public static MesaDTO toDTO(Mesa mesa, EstadoMesa estado, CuentaMesa vigente) {
        if (mesa == null) return null;

        MesaDTO dto = MesaDTO.builder()
                .id(mesa.getId())
                .nombre(mesa.getNombre())
                .capacidad(mesa.getCapacidad())
                .forma(mesa.getForma())
                .posX(mesa.getPosX())
                .posY(mesa.getPosY())
                .ancho(mesa.getAncho())
                .alto(mesa.getAlto())
                .temporal(mesa.getTemporal())
                .bloqueada(mesa.getBloqueada())
                .activa(mesa.getActiva())
                .estado(estado)
                .build();

        if (mesa.getSede() != null) {
            dto.setSedeId(mesa.getSede().getId());
        }

        if (mesa.getSeccion() != null) {
            dto.setSeccionId(mesa.getSeccion().getId());
            dto.setSeccionNombre(mesa.getSeccion().getNombre());
        }

        if (vigente != null) {
            Cuenta cuenta = vigente.getCuenta();
            dto.setCuentaId(cuenta.getId());
            dto.setMozoNombre(cuenta.getMozo().getNombre());
            dto.setMozoCodigo(cuenta.getMozo().getCodigo());
            dto.setTotal(cuenta.getTotal());
            dto.setAbiertaEn(cuenta.getAbiertaEn());
        }

        return dto;
    }
}