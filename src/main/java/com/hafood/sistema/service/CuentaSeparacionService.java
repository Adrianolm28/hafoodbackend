package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaDescuento;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.CuentaDTO;
import com.hafood.sistema.dto.request.CobroRequests;
import com.hafood.sistema.repository.CuentaDescuentoRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaPagoRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaSeparacionService {

    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaDescuentoRepository cuentaDescuentoRepository;
    private final CuentaPagoRepository cuentaPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaCalculoService calculoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public CuentaDTO separar(Long cuentaId, CobroRequests.Separar request, Usuario actor) {
        Cuenta principal = cuentaService.bloquear(cuentaId, actor);

        if (principal.getCuentaPadre() != null) {
            throw conflicto("Una cuenta separada no se puede volver a separar");
        }
        if (principal.getEstado() != EstadoCuenta.PRECUENTA) {
            throw conflicto("Para separar la cuenta primero pide la precuenta");
        }
        if (cuentaPagoRepository.existsByCuentaId(cuentaId)) {
            throw conflicto("La cuenta ya tiene pagos y no se puede separar");
        }
        if (cuentaDescuentoRepository.existsByCuentaIdAndLineaIsNullAndActivoTrue(cuentaId)) {
            throw conflicto("La cuenta tiene un descuento general. Reábrela, quítalo y pide la precuenta otra vez");
        }

        calculoService.recalcular(principal);
        BigDecimal original = principal.getTotalPrecuenta();

        if (original == null || principal.getTotal().compareTo(original) != 0) {
            throw conflicto("El total de la cuenta cambió. Reabre la cuenta y pide la precuenta otra vez");
        }

        List<CuentaLinea> vivas = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId).stream()
                .filter(l -> l.getEstado() != EstadoLinea.ANULADA)
                .toList();
        Map<Long, CuentaLinea> porId = new HashMap<>();
        vivas.forEach(l -> porId.put(l.getId(), l));

        Set<Long> conDescuento = cuentaDescuentoRepository.findActivosByCuentaId(cuentaId).stream()
                .filter(d -> d.getLinea() != null)
                .map(d -> d.getLinea().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> pedido = new HashMap<>();

        for (CobroRequests.Grupo grupo : request.grupos()) {
            for (CobroRequests.Item item : grupo.items()) {
                CuentaLinea linea = porId.get(item.lineaId());

                if (linea == null) {
                    throw invalida("Un producto no pertenece a esta cuenta");
                }
                if (item.cantidad() < linea.getCantidad() && conDescuento.contains(linea.getId())) {
                    throw conflicto("«" + linea.getNombre() + "» tiene descuento o cortesía y no se puede dividir. Asígnalo completo");
                }

                pedido.merge(item.lineaId(), item.cantidad(), Integer::sum);
            }
        }

        int unidadesVivas = 0;
        int unidadesPedidas = 0;

        for (CuentaLinea linea : vivas) {
            unidadesVivas += linea.getCantidad();
            int asignadas = pedido.getOrDefault(linea.getId(), 0);

            if (asignadas > linea.getCantidad()) {
                throw invalida("Asignaste más unidades de «" + linea.getNombre() + "» de las que hay en la cuenta");
            }

            unidadesPedidas += asignadas;
        }

        if (unidadesPedidas >= unidadesVivas) {
            throw invalida("La cuenta principal debe quedar con al menos un producto");
        }

        Instant ahora = Instant.now();
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        List<Cuenta> hijas = new ArrayList<>();
        List<CuentaDescuento> descuentos = cuentaDescuentoRepository.findByCuentaId(cuentaId);

        for (CobroRequests.Grupo grupo : request.grupos()) {
            Cuenta hija = cuentaRepository.save(Cuenta.builder()
                    .sede(principal.getSede())
                    .carta(principal.getCarta())
                    .mozo(principal.getMozo())
                    .abiertaPor(usuario)
                    .estado(EstadoCuenta.PRECUENTA)
                    .abiertaEn(principal.getAbiertaEn())
                    .cajaSesion(principal.getCajaSesion())
                    .cuentaPadre(principal)
                    .precuentaEn(ahora)
                    .build());

            Set<Long> movidas = new HashSet<>();

            for (CobroRequests.Item item : grupo.items()) {
                CuentaLinea linea = porId.get(item.lineaId());

                if (item.cantidad().intValue() == linea.getCantidad().intValue()) {
                    if (linea.getCuentaOrigen() == null) {
                        linea.setCuentaOrigen(principal);
                    }
                    linea.setCuenta(hija);
                    cuentaLineaRepository.save(linea);
                    movidas.add(linea.getId());
                } else {
                    linea.setCantidad(linea.getCantidad() - item.cantidad());
                    cuentaLineaRepository.save(linea);
                    cuentaLineaRepository.save(copiar(linea, hija, principal, item.cantidad()));
                }
            }

            descuentos.stream()
                    .filter(d -> d.getLinea() != null && movidas.contains(d.getLinea().getId()))
                    .forEach(d -> d.setCuenta(hija));

            hijas.add(hija);
        }

        BigDecimal sumaHijas = BigDecimal.ZERO;
        StringBuilder detalle = new StringBuilder();

        for (Cuenta hija : hijas) {
            calculoService.recalcular(hija);
            hija.setTotalPrecuenta(hija.getTotal());
            hija.setEstado(hija.getTotal().signum() == 0 ? EstadoCuenta.PAGADA : EstadoCuenta.PRECUENTA);
            cuentaRepository.save(hija);
            sumaHijas = sumaHijas.add(hija.getTotal());

            if (!detalle.isEmpty()) {
                detalle.append(", ");
            }
            detalle.append('#').append(hija.getId()).append(" (S/ ").append(hija.getTotal().toPlainString()).append(')');
        }

        calculoService.recalcular(principal);
        principal.setTotalPrecuenta(principal.getTotal());
        principal.setEstado(principal.getTotal().signum() == 0 ? EstadoCuenta.PAGADA : EstadoCuenta.PRECUENTA);
        cuentaRepository.save(principal);

        if (sumaHijas.add(principal.getTotal()).compareTo(original) != 0) {
            throw conflicto("La separación no cuadra con el total original. No se aplicó ningún cambio");
        }

        Long sedeId = principal.getSede().getId();

        auditoriaService.registrar(actor, sedeId, cuentaId, AccionAuditoria.CUENTA_SEPARADA,
                "total=" + original.toPlainString(),
                "principal=S/ " + principal.getTotal().toPlainString() + ";separadas=" + detalle, null);

        for (Cuenta hija : hijas) {
            auditoriaService.registrar(actor, sedeId, hija.getId(), AccionAuditoria.CUENTA_SEPARADA,
                    null, "separada de la cuenta #" + cuentaId + ";total=" + hija.getTotal().toPlainString(), null);
        }

        return cuentaService.responder(principal);
    }

    @Transactional
    public CuentaDTO reunir(Long hijaId, Usuario actor) {
        Long padreId = cuentaRepository.findById(hijaId)
                .filter(c -> c.getCuentaPadre() != null)
                .map(c -> c.getCuentaPadre().getId())
                .orElseThrow(() -> conflicto("Esa cuenta no es una cuenta separada"));

        Cuenta padre = cuentaService.bloquear(padreId, actor);
        Cuenta hija = cuentaService.bloquear(hijaId, actor);

        if (hija.getCuentaPadre() == null || !hija.getCuentaPadre().getId().equals(padreId)) {
            throw conflicto("La cuenta cambió. Actualiza la pantalla");
        }
        if (padre.getEstado() != EstadoCuenta.PRECUENTA || cuentaPagoRepository.existsByCuentaId(padreId)) {
            throw conflicto("La cuenta principal ya tiene pagos y no puede recibir productos");
        }
        if ((hija.getEstado() != EstadoCuenta.PRECUENTA && hija.getEstado() != EstadoCuenta.PAGADA)
                || cuentaPagoRepository.existsByCuentaId(hijaId)) {
            throw conflicto("Esta cuenta separada ya tiene pagos y no se puede reunir");
        }

        Instant ahora = Instant.now();
        List<CuentaLinea> lineas = cuentaLineaRepository.findByCuentaIdConUsuario(hijaId);
        BigDecimal totalHija = hija.getTotal();

        lineas.forEach(l -> {
            if (l.getCuentaOrigen() != null && l.getCuentaOrigen().getId().equals(padreId)) {
                l.setCuentaOrigen(null);
            }
            l.setCuenta(padre);
        });
        cuentaLineaRepository.saveAll(lineas);

        cuentaDescuentoRepository.findByCuentaId(hijaId).stream()
                .filter(d -> d.getLinea() != null)
                .forEach(d -> d.setCuenta(padre));

        hija.setEstado(EstadoCuenta.FUSIONADA);
        hija.setFusionadaEn(padre);
        hija.setCerradaEn(ahora);
        hija.setTotal(BigDecimal.ZERO.setScale(2));
        hija.setSubtotal(BigDecimal.ZERO.setScale(2));
        hija.setDescuentoTotal(BigDecimal.ZERO.setScale(2));
        hija.setTotalPrecuenta(BigDecimal.ZERO.setScale(2));
        cuentaRepository.save(hija);

        BigDecimal totalPadreAntes = padre.getTotalPrecuenta();
        calculoService.recalcular(padre);
        padre.setTotalPrecuenta(padre.getTotal());
        cuentaRepository.save(padre);

        if (padre.getTotal().compareTo(totalPadreAntes.add(totalHija)) != 0) {
            throw conflicto("La reunión no cuadra con los totales. No se aplicó ningún cambio");
        }

        Long sedeId = padre.getSede().getId();

        auditoriaService.registrar(actor, sedeId, padreId, AccionAuditoria.CUENTA_REUNIDA,
                "total=" + totalPadreAntes.toPlainString(),
                "se reunió la cuenta #" + hijaId + " (S/ " + totalHija.toPlainString() + ");total="
                        + padre.getTotal().toPlainString(), null);
        auditoriaService.registrar(actor, sedeId, hijaId, AccionAuditoria.CUENTA_REUNIDA,
                "total=" + totalHija.toPlainString(), "reunida en la cuenta #" + padreId, null);

        return cuentaService.responder(padre);
    }

    private CuentaLinea copiar(CuentaLinea origen, Cuenta hija, Cuenta principal, int cantidad) {
        return CuentaLinea.builder()
                .cuenta(hija)
                .cuentaOrigen(principal)
                .tipo(origen.getTipo())
                .productoId(origen.getProductoId())
                .nombre(origen.getNombre())
                .cantidad(cantidad)
                .precioUnitario(origen.getPrecioUnitario())
                .nota(origen.getNota())
                .estado(origen.getEstado())
                .cartaId(origen.getCartaId())
                .agregadaPor(origen.getAgregadaPor())
                .agregadaEn(origen.getAgregadaEn())
                .comanda(origen.getComanda())
                .enviadaEn(origen.getEnviadaEn())
                .preparacionEn(origen.getPreparacionEn())
                .listaEn(origen.getListaEn())
                .entregadaEn(origen.getEntregadaEn())
                .build();
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}