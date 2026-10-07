package com.hafood.sistema.service;

import com.hafood.sistema.domain.inventario.Proveedor;
import com.hafood.sistema.dto.ProveedorDTO;
import com.hafood.sistema.mapper.Mapper;
import com.hafood.sistema.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public Page<ProveedorDTO> listar(boolean soloActivos, Pageable pageable) {
        Page<Proveedor> page = soloActivos
                ? proveedorRepository.findByActivoTrue(pageable)
                : proveedorRepository.findAll(pageable);
        return page.map(Mapper::toDTO);
    }

    @Transactional
    public ProveedorDTO crear(ProveedorDTO dto) {
        String nombre = dto.getNombre().trim();

        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            throw conflicto();
        }

        Proveedor proveedor = Proveedor.builder()
                .nombre(nombre)
                .ruc(textoONulo(dto.getRuc()))
                .telefono(textoONulo(dto.getTelefono()))
                .contacto(textoONulo(dto.getContacto()))
                .build();

        return Mapper.toDTO(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorDTO dto) {
        Proveedor proveedor = buscar(id);
        String nombre = dto.getNombre().trim();

        if (proveedorRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw conflicto();
        }

        proveedor.setNombre(nombre);
        proveedor.setRuc(textoONulo(dto.getRuc()));
        proveedor.setTelefono(textoONulo(dto.getTelefono()));
        proveedor.setContacto(textoONulo(dto.getContacto()));

        if (dto.getActivo() != null) {
            proveedor.setActivo(dto.getActivo());
        }

        return Mapper.toDTO(proveedorRepository.save(proveedor));
    }

    @Transactional
    public void desactivar(Long id) {
        Proveedor proveedor = buscar(id);
        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    private Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El proveedor no existe"));
    }

    private ResponseStatusException conflicto() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese nombre");
    }

    private String textoONulo(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}