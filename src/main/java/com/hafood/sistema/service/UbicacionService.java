package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoUbicacion;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.dto.UbicacionDTO;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.SedeRepository;
import com.hafood.sistema.repository.UbicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UbicacionService {

    private static final String NOMBRE_ALMACEN = "Almacén";

    private final UbicacionRepository ubicacionRepository;
    private final SedeRepository sedeRepository;

    @Transactional
    public List<UbicacionDTO> listarPorSede(Long sedeId) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede no existe"));

        sincronizar(sede);

        return ubicacionRepository.findBySedeIdOrderByTipoAscNombreAsc(sedeId).stream()
                .map(Mapper::toDTO)
                .toList();
    }

    private void sincronizar(Sede sede) {
        if (ubicacionRepository.findFirstBySedeIdAndTipo(sede.getId(), TipoUbicacion.ALMACEN).isEmpty()) {
            ubicacionRepository.save(Ubicacion.builder()
                    .sede(sede)
                    .tipo(TipoUbicacion.ALMACEN)
                    .nombre(NOMBRE_ALMACEN)
                    .build());
        }

        for (Seccion seccion : ubicacionRepository.findSeccionesBySedeId(sede.getId())) {
            Optional<Ubicacion> existente = ubicacionRepository.findBySeccionId(seccion.getId());

            if (existente.isEmpty()) {
                ubicacionRepository.save(Ubicacion.builder()
                        .sede(sede)
                        .tipo(TipoUbicacion.BARRA)
                        .seccion(seccion)
                        .nombre(seccion.getNombre())
                        .build());
            } else if (!existente.get().getNombre().equals(seccion.getNombre())) {
                existente.get().setNombre(seccion.getNombre());
                ubicacionRepository.save(existente.get());
            }
        }
    }
}