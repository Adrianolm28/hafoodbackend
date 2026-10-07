package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.EstadoMesa;
import com.hafood.sistema.constant.FormaMesa;
import com.hafood.sistema.domain.estructura.Mesa;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.MesaDTO;
import com.hafood.sistema.dto.MesaPosicionRequest;
import com.hafood.sistema.dto.MesaTemporalRequest;
import com.hafood.sistema.mapper.MesaMapper;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.MesaRepository;
import com.hafood.sistema.repository.SeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hafood.sistema.domain.pos.CuentaMesa;
import com.hafood.sistema.repository.CuentaMesaRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaService {

    public static final int GRID_COLUMNAS = 24;
    public static final int GRID_FILAS = 14;

    private static final int CAPACIDAD_POR_DEFECTO = 4;
    private static final int LIMITE_TEMPORALES = 500;

    private final MesaRepository mesaRepository;
    private final SeccionRepository seccionRepository;
    private final SedeAccesoService sedeAccesoService;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaMesaRepository cuentaMesaRepository;

    private record Posicion(int x, int y) {
    }

    @Transactional(readOnly = true)
    public List<MesaDTO> listar(Long sedeId, boolean incluirInactivas, Usuario actor) {
        sedeAccesoService.exigirAcceso(actor, sedeId);
        List<Mesa> mesas = incluirInactivas
                ? mesaRepository.findBySedeIdOrderByIdAsc(sedeId)
                : mesaRepository.findBySedeIdAndActivaTrueOrderByIdAsc(sedeId);
        Map<Long, CuentaMesa> vigentes = cuentaMesaRepository.findVigentesBySedeId(sedeId).stream()
                .collect(Collectors.toMap(cm -> cm.getMesa().getId(), Function.identity()));
        Map<Long, Long> listos = cuentaLineaRepository.contarPorCuenta(sedeId, EstadoLinea.LISTA).stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));

        return mesas.stream().map(m -> {
            CuentaMesa vigente = vigentes.get(m.getId());
            MesaDTO dto = aDTO(m, vigente);

            if (vigente != null) {
                dto.setListos(listos.getOrDefault(vigente.getCuenta().getId(), 0L).intValue());
            }

            return dto;
        }).toList();
    }

    @Transactional
    public MesaDTO crear(MesaDTO dto, Usuario actor) {
        Seccion seccion = buscarSeccion(dto.getSeccionId());
        Long sedeId = seccion.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);
        exigirSeccionActiva(seccion);

        String nombre = dto.getNombre().trim();

        if (mesaRepository.existsBySedeIdAndNombreIgnoreCaseAndActivaTrue(sedeId, nombre)) {
            throw nombreEnUso();
        }

        FormaMesa forma = dto.getForma() != null ? dto.getForma() : FormaMesa.CUADRADA;
        int ancho = dto.getAncho() != null ? dto.getAncho() : anchoPorDefecto(forma);
        int alto = dto.getAlto() != null ? dto.getAlto() : 2;
        Posicion posicion = resolverPosicion(seccion.getId(), null, dto.getPosX(), dto.getPosY(), ancho, alto, false);

        Mesa mesa = Mesa.builder()
                .sede(seccion.getSede())
                .seccion(seccion)
                .nombre(nombre)
                .capacidad(dto.getCapacidad() != null ? dto.getCapacidad() : CAPACIDAD_POR_DEFECTO)
                .forma(forma)
                .posX(posicion.x())
                .posY(posicion.y())
                .ancho(ancho)
                .alto(alto)
                .bloqueada(Boolean.TRUE.equals(dto.getBloqueada()))
                .build();

        return guardar(mesa);
    }

    @Transactional
    public MesaDTO crearTemporal(MesaTemporalRequest request, Usuario actor) {
        Seccion seccion = buscarSeccion(request.seccionId());
        Long sedeId = seccion.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);
        exigirSeccionActiva(seccion);

        Posicion posicion = resolverPosicion(seccion.getId(), null, null, null, 2, 2, false);

        Mesa mesa = Mesa.builder()
                .sede(seccion.getSede())
                .seccion(seccion)
                .nombre(siguienteNombreTemporal(sedeId))
                .capacidad(request.capacidad() != null ? request.capacidad() : CAPACIDAD_POR_DEFECTO)
                .forma(FormaMesa.CUADRADA)
                .posX(posicion.x())
                .posY(posicion.y())
                .ancho(2)
                .alto(2)
                .temporal(true)
                .build();

        return guardar(mesa);
    }

    @Transactional
    public MesaDTO actualizar(Long id, MesaDTO dto, Usuario actor) {
        Mesa mesa = buscar(id);
        Long sedeId = mesa.getSede().getId();
        sedeAccesoService.exigirAcceso(actor, sedeId);

        boolean cambiaSeccion = !mesa.getSeccion().getId().equals(dto.getSeccionId());
        Seccion seccion = cambiaSeccion ? buscarSeccion(dto.getSeccionId()) : mesa.getSeccion();

        if (!seccion.getSede().getId().equals(sedeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sección debe pertenecer a la misma sede de la mesa");
        }

        boolean estabaActiva = Boolean.TRUE.equals(mesa.getActiva());
        boolean seraActiva = dto.getActiva() == null ? estabaActiva : dto.getActiva();
        boolean seraBloqueada = dto.getBloqueada() == null ? Boolean.TRUE.equals(mesa.getBloqueada()) : dto.getBloqueada();

        if (((estabaActiva && !seraActiva) || seraBloqueada) && cuentaMesaRepository.existsByMesaIdAndHastaIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa tiene una cuenta abierta");
        }
        String nombre = dto.getNombre().trim();
        int ancho = dto.getAncho() != null ? dto.getAncho() : mesa.getAncho();
        int alto = dto.getAlto() != null ? dto.getAlto() : mesa.getAlto();

        if (seraActiva) {
            exigirSeccionActiva(seccion);

            if (mesaRepository.existsBySedeIdAndNombreIgnoreCaseAndActivaTrueAndIdNot(sedeId, nombre, id)) {
                throw nombreEnUso();
            }

            Integer solicitadaX = dto.getPosX() != null ? dto.getPosX() : (cambiaSeccion ? null : mesa.getPosX());
            Integer solicitadaY = dto.getPosY() != null ? dto.getPosY() : (cambiaSeccion ? null : mesa.getPosY());
            Posicion posicion = resolverPosicion(seccion.getId(), id, solicitadaX, solicitadaY, ancho, alto, !estabaActiva);
            mesa.setPosX(posicion.x());
            mesa.setPosY(posicion.y());
        }

        mesa.setSeccion(seccion);
        mesa.setNombre(nombre);
        mesa.setCapacidad(dto.getCapacidad() != null ? dto.getCapacidad() : mesa.getCapacidad());
        mesa.setForma(dto.getForma() != null ? dto.getForma() : mesa.getForma());
        mesa.setAncho(ancho);
        mesa.setAlto(alto);
        mesa.setActiva(seraActiva);

        if (dto.getBloqueada() != null) {
            mesa.setBloqueada(dto.getBloqueada());
        }

        return guardar(mesa);
    }

    @Transactional
    public MesaDTO moverPosicion(Long id, MesaPosicionRequest request, Usuario actor) {
        Mesa mesa = buscar(id);
        sedeAccesoService.exigirAcceso(actor, mesa.getSede().getId());

        if (!Boolean.TRUE.equals(mesa.getActiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa está inactiva");
        }

        Posicion posicion = resolverPosicion(
                mesa.getSeccion().getId(), id, request.posX(), request.posY(), mesa.getAncho(), mesa.getAlto(), false);
        mesa.setPosX(posicion.x());
        mesa.setPosY(posicion.y());

        return guardar(mesa);
    }

    @Transactional
    public void desactivar(Long id, Usuario actor) {
        Mesa mesa = buscar(id);
        sedeAccesoService.exigirAcceso(actor, mesa.getSede().getId());
        if (cuentaMesaRepository.existsByMesaIdAndHastaIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa tiene una cuenta abierta");
        }
        mesa.setActiva(false);
        mesaRepository.save(mesa);
    }

    private EstadoMesa calcularEstado(Mesa mesa, CuentaMesa vigente) {
        if (Boolean.TRUE.equals(mesa.getBloqueada())) {
            return EstadoMesa.BLOQUEADA;
        }

        if (vigente == null) {
            return EstadoMesa.LIBRE;
        }

        return switch (vigente.getCuenta().getEstado()) {
            case PRECUENTA -> EstadoMesa.ESPERANDO_PAGO;
            case PAGO_PARCIAL -> EstadoMesa.PAGO_PARCIAL;
            default -> EstadoMesa.OCUPADA;
        };
    }

    private MesaDTO aDTO(Mesa mesa) {
        return aDTO(mesa, cuentaMesaRepository.findVigenteByMesaId(mesa.getId()).orElse(null));
    }

    private MesaDTO aDTO(Mesa mesa, CuentaMesa vigente) {
        return MesaMapper.toDTO(mesa, calcularEstado(mesa, vigente), vigente);
    }

    private MesaDTO guardar(Mesa mesa) {
        try {
            return aDTO(mesaRepository.saveAndFlush(mesa));
        } catch (DataIntegrityViolationException e) {
            throw nombreEnUso();
        }
    }

    private Mesa buscar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La mesa no existe"));
    }

    private Seccion buscarSeccion(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección especificada no existe"));
    }

    private void exigirSeccionActiva(Seccion seccion) {
        if (Boolean.FALSE.equals(seccion.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección está inactiva");
        }
    }

    private int anchoPorDefecto(FormaMesa forma) {
        return forma == FormaMesa.RECTANGULAR ? 3 : 2;
    }

    private List<Mesa> ocupadasEnSeccion(Long seccionId, Long excluirMesaId) {
        return mesaRepository.findBySeccionIdForUpdate(seccionId).stream()
                .filter(m -> Boolean.TRUE.equals(m.getActiva()) && !m.getId().equals(excluirMesaId))
                .toList();
    }

    private Posicion resolverPosicion(Long seccionId, Long mesaId, Integer posX, Integer posY,
                                      int ancho, int alto, boolean reubicarSiChoca) {
        List<Mesa> ocupadas = ocupadasEnSeccion(seccionId, mesaId);

        if (posX == null || posY == null) {
            return ubicarLibre(ocupadas, ancho, alto);
        }

        boolean dentro = posX >= 0 && posY >= 0 && posX + ancho <= GRID_COLUMNAS && posY + alto <= GRID_FILAS;

        if (!dentro) {
            if (reubicarSiChoca) {
                return ubicarLibre(ocupadas, ancho, alto);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La mesa queda fuera del plano");
        }

        if (choca(posX, posY, ancho, alto, ocupadas)) {
            if (reubicarSiChoca) {
                return ubicarLibre(ocupadas, ancho, alto);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa posición está ocupada por otra mesa");
        }

        return new Posicion(posX, posY);
    }

    private Posicion ubicarLibre(List<Mesa> ocupadas, int ancho, int alto) {
        for (int y = 0; y + alto <= GRID_FILAS; y++) {
            for (int x = 0; x + ancho <= GRID_COLUMNAS; x++) {
                if (!choca(x, y, ancho, alto, ocupadas)) {
                    return new Posicion(x, y);
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay espacio libre en el plano de esta sección");
    }

    private boolean choca(int x, int y, int ancho, int alto, List<Mesa> ocupadas) {
        return ocupadas.stream().anyMatch(m ->
                x < m.getPosX() + m.getAncho()
                        && m.getPosX() < x + ancho
                        && y < m.getPosY() + m.getAlto()
                        && m.getPosY() < y + alto);
    }

    private String siguienteNombreTemporal(Long sedeId) {
        for (int i = 1; i <= LIMITE_TEMPORALES; i++) {
            String nombre = "Extra " + i;
            if (!mesaRepository.existsBySedeIdAndNombreIgnoreCaseAndActivaTrue(sedeId, nombre)) {
                return nombre;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Hay demasiadas mesas temporales activas");
    }

    private ResponseStatusException nombreEnUso() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una mesa activa con ese nombre en esta sede");
    }
}