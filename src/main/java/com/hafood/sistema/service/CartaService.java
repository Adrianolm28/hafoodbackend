package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoImagen;
import com.hafood.sistema.domain.carta.Carta;
import com.hafood.sistema.domain.carta.CartaBebida;
import com.hafood.sistema.domain.carta.CartaCategoria;
import com.hafood.sistema.domain.carta.CartaPlato;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.dto.CartaDTO;
import com.hafood.sistema.dto.request.CartaRequest;
import com.hafood.sistema.dto.request.DuplicarCartaRequest;
import com.hafood.sistema.mapper.CartaMapper;
import com.hafood.sistema.repository.CartaBebidaRepository;
import com.hafood.sistema.repository.CartaCategoriaRepository;
import com.hafood.sistema.repository.CartaPlatoRepository;
import com.hafood.sistema.repository.CartaRepository;
import com.hafood.sistema.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartaService {

    private static final String ALFABETO = "23456789abcdefghjkmnpqrstuvwxyz";
    private static final int LONGITUD_CODIGO = 8;

    private final SecureRandom random = new SecureRandom();

    private final CartaRepository cartaRepository;
    private final SedeRepository sedeRepository;
    private final CartaCategoriaRepository cartaCategoriaRepository;
    private final CartaPlatoRepository cartaPlatoRepository;
    private final CartaBebidaRepository cartaBebidaRepository;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public List<CartaDTO> listarPorSede(Long sedeId) {
        buscarSede(sedeId);
        return cartaRepository.listarPorSede(sedeId).stream().map(CartaMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CartaDTO obtener(Long id) {
        return CartaMapper.toDTO(buscar(id));
    }

    @Transactional
    public CartaDTO crear(CartaRequest request) {
        if (request.sedeId() == null) {
            throw invalida("La sede es obligatoria");
        }
        Sede sede = buscarSede(request.sedeId());
        String nombre = request.nombre().trim();
        validarNombreLibre(sede.getId(), nombre, null);

        Carta carta = Carta.builder()
                .sede(sede)
                .nombre(nombre)
                .codigo(generarCodigo())
                .mostrarImagenes(Boolean.TRUE.equals(request.mostrarImagenes()))
                .build();

        return CartaMapper.toDTO(cartaRepository.save(carta));
    }

    @Transactional
    public CartaDTO actualizar(Long id, CartaRequest request) {
        Carta carta = buscar(id);
        String nombre = request.nombre().trim();
        validarNombreLibre(carta.getSede().getId(), nombre, id);

        carta.setNombre(nombre);
        if (request.mostrarImagenes() != null) {
            carta.setMostrarImagenes(request.mostrarImagenes());
        }
        if (request.activo() != null) {
            carta.setActivo(request.activo());
        }

        return CartaMapper.toDTO(cartaRepository.save(carta));
    }

    @Transactional
    public void desactivar(Long id) {
        Carta carta = buscar(id);
        carta.setActivo(false);
        cartaRepository.save(carta);
    }

    @Transactional
    public CartaDTO duplicar(Long id, DuplicarCartaRequest request) {
        Carta origen = buscar(id);
        Sede sede = buscarSede(request.sedeId());
        String nombre = request.nombre().trim();
        validarNombreLibre(sede.getId(), nombre, null);

        Carta copia = Carta.builder()
                .sede(sede)
                .nombre(nombre)
                .codigo(generarCodigo())
                .mostrarImagenes(origen.isMostrarImagenes())
                .fondoMovil(imageService.copiar(origen.getFondoMovil()))
                .fondoEscritorio(imageService.copiar(origen.getFondoEscritorio()))
                .logo(imageService.copiar(origen.getLogo()))
                .build();
        copia = cartaRepository.save(copia);

        Carta destino = copia;
        cartaCategoriaRepository.saveAll(cartaCategoriaRepository.findByCartaId(origen.getId()).stream()
                .map(f -> CartaCategoria.builder().carta(destino).categoria(f.getCategoria())
                        .habilitada(f.isHabilitada()).orden(f.getOrden()).build())
                .toList());
        cartaPlatoRepository.saveAll(cartaPlatoRepository.findByCartaId(origen.getId()).stream()
                .map(f -> CartaPlato.builder().carta(destino).plato(f.getPlato())
                        .habilitado(f.isHabilitado()).precio(f.getPrecio()).orden(f.getOrden()).build())
                .toList());
        cartaBebidaRepository.saveAll(cartaBebidaRepository.findByCartaId(origen.getId()).stream()
                .map(f -> CartaBebida.builder().carta(destino).bebida(f.getBebida())
                        .habilitado(f.isHabilitado()).precio(f.getPrecio()).orden(f.getOrden()).build())
                .toList());

        return CartaMapper.toDTO(destino);
    }

    @Transactional
    public CartaDTO subirImagen(Long id, TipoImagen tipo, MultipartFile archivo) {
        Carta carta = buscar(id);
        String nueva = imageService.procesarYGuardar(archivo, tipo);
        String anterior = obtenerRuta(carta, tipo);
        asignarRuta(carta, tipo, nueva);
        cartaRepository.save(carta);
        imageService.eliminarTrasCommit(anterior);
        return CartaMapper.toDTO(carta);
    }

    @Transactional
    public CartaDTO quitarImagen(Long id, TipoImagen tipo) {
        Carta carta = buscar(id);
        String anterior = obtenerRuta(carta, tipo);
        asignarRuta(carta, tipo, null);
        cartaRepository.save(carta);
        imageService.eliminarTrasCommit(anterior);
        return CartaMapper.toDTO(carta);
    }

    private String obtenerRuta(Carta carta, TipoImagen tipo) {
        return switch (tipo) {
            case FONDO_MOVIL -> carta.getFondoMovil();
            case FONDO_ESCRITORIO -> carta.getFondoEscritorio();
            case LOGO -> carta.getLogo();
            default -> throw invalida("Tipo de imagen no válido");
        };
    }

    private void asignarRuta(Carta carta, TipoImagen tipo, String ruta) {
        switch (tipo) {
            case FONDO_MOVIL -> carta.setFondoMovil(ruta);
            case FONDO_ESCRITORIO -> carta.setFondoEscritorio(ruta);
            case LOGO -> carta.setLogo(ruta);
            default -> throw invalida("Tipo de imagen no válido");
        }
    }

    private Carta buscar(Long id) {
        return cartaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La carta no existe"));
    }

    private Sede buscarSede(Long sedeId) {
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sede no existe"));
    }

    private void validarNombreLibre(Long sedeId, String nombre, Long excluirId) {
        boolean existe = excluirId == null
                ? cartaRepository.existsBySedeIdAndNombreIgnoreCase(sedeId, nombre)
                : cartaRepository.existsBySedeIdAndNombreIgnoreCaseAndIdNot(sedeId, nombre, excluirId);
        if (existe) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carta con ese nombre en esta sede");
        }
    }

    private String generarCodigo() {
        for (int intento = 0; intento < 10; intento++) {
            StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);
            for (int i = 0; i < LONGITUD_CODIGO; i++) {
                codigo.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            }
            if (!cartaRepository.existsByCodigo(codigo.toString())) {
                return codigo.toString();
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el código de la carta");
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
