package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoImagen;
import com.hafood.sistema.domain.barra.Bebida;
import com.hafood.sistema.domain.cocina.Plato;
import com.hafood.sistema.dto.ImagenDTO;
import com.hafood.sistema.mapper.ImagenUrl;
import com.hafood.sistema.repository.BebidaRepository;
import com.hafood.sistema.repository.PlatoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductoImagenService {

    private final PlatoRepository platoRepository;
    private final BebidaRepository bebidaRepository;
    private final ImageService imageService;

    @Transactional
    public ImagenDTO subirPlato(Long id, MultipartFile archivo) {
        Plato plato = buscarPlato(id);
        String nueva = imageService.procesarYGuardar(archivo, TipoImagen.PRODUCTO);
        String anterior = plato.getImagen();
        plato.setImagen(nueva);
        platoRepository.save(plato);
        imageService.eliminarTrasCommit(anterior);
        return new ImagenDTO(ImagenUrl.de(nueva));
    }

    @Transactional
    public void quitarPlato(Long id) {
        Plato plato = buscarPlato(id);
        String anterior = plato.getImagen();
        plato.setImagen(null);
        platoRepository.save(plato);
        imageService.eliminarTrasCommit(anterior);
    }

    @Transactional
    public ImagenDTO subirBebida(Long id, MultipartFile archivo) {
        Bebida bebida = buscarBebida(id);
        String nueva = imageService.procesarYGuardar(archivo, TipoImagen.PRODUCTO);
        String anterior = bebida.getImagen();
        bebida.setImagen(nueva);
        bebidaRepository.save(bebida);
        imageService.eliminarTrasCommit(anterior);
        return new ImagenDTO(ImagenUrl.de(nueva));
    }

    @Transactional
    public void quitarBebida(Long id) {
        Bebida bebida = buscarBebida(id);
        String anterior = bebida.getImagen();
        bebida.setImagen(null);
        bebidaRepository.save(bebida);
        imageService.eliminarTrasCommit(anterior);
    }

    private Plato buscarPlato(Long id) {
        return platoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El plato no existe"));
    }

    private Bebida buscarBebida(Long id) {
        return bebidaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La bebida no existe"));
    }
}
