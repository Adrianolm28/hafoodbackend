package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.constant.EstadoCuenta;
import com.hafood.sistema.constant.EstadoLinea;
import com.hafood.sistema.constant.RegimenTributario;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.constant.TipoDocumentoCliente;
import com.hafood.sistema.constant.TipoImpuesto;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.pos.OperacionProcesada;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteCliente;
import com.hafood.sistema.domain.sunat.ComprobanteLinea;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.sunat.SedeSunat;
import com.hafood.sistema.domain.sunat.SerieComprobante;
import com.hafood.sistema.domain.sunat.TasaImpuesto;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ComprobanteDTO;
import com.hafood.sistema.dto.request.ComprobanteRequests;
import com.hafood.sistema.repository.ComprobanteLineaRepository;
import com.hafood.sistema.repository.ComprobanteRepository;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import com.hafood.sistema.repository.CuentaLineaRepository;
import com.hafood.sistema.repository.CuentaPagoRepository;
import com.hafood.sistema.repository.CuentaRepository;
import com.hafood.sistema.repository.OperacionProcesadaRepository;
import com.hafood.sistema.repository.SedeSunatRepository;
import com.hafood.sistema.repository.SerieComprobanteRepository;
import com.hafood.sistema.repository.TasaImpuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final String OPERACION_EMITIR = "EMITIR_COMPROBANTE";
    private static final String UNIDAD = "NIU";
    private static final String CLIENTES_VARIOS = "CLIENTES VARIOS";
    private static final long CORRELATIVO_MAXIMO = 99_999_999L;

    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final CuentaLineaRepository cuentaLineaRepository;
    private final CuentaPagoRepository cuentaPagoRepository;
    private final CuentaCalculoService calculoService;
    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteLineaRepository comprobanteLineaRepository;
    private final ConfiguracionSunatRepository configuracionRepository;
    private final SedeSunatRepository sedeSunatRepository;
    private final SerieComprobanteRepository serieRepository;
    private final TasaImpuestoRepository tasaRepository;
    private final OperacionProcesadaRepository operacionRepository;
    private final SedeAccesoService sedeAccesoService;
    private final AuditoriaService auditoriaService;
    private final ComprobanteArchivoService archivoService;

    @Transactional(readOnly = true)
    public Optional<ComprobanteDTO> obtenerDeCuenta(Long cuentaId, Usuario actor) {
        Long sedeId = cuentaRepository.findSedeIdById(cuentaId)
                .orElseThrow(() -> noExiste("La cuenta no existe"));
        sedeAccesoService.exigirAcceso(actor, sedeId);

        return comprobanteRepository.findByCuentaVigenteId(cuentaId).map(this::armar);
    }

    @Transactional
    public ComprobanteDTO emitir(Long cuentaId, ComprobanteRequests.Emitir request, Usuario actor) {
        Cuenta cuenta = cuentaService.bloquear(cuentaId, actor);
        Comprobante vigente = comprobanteRepository.findByCuentaVigenteId(cuentaId).orElse(null);

        if (operacionRepository.existsByClave(request.claveIdempotencia())) {
            if (vigente == null) {
                throw conflicto("Esta operación ya fue procesada. Actualiza la pantalla");
            }
            return armar(vigente);
        }
        if (vigente != null) {
            throw conflicto("La cuenta ya tiene el comprobante " + numero(vigente.getSerie(), vigente.getCorrelativo()));
        }

        BigDecimal totalCuenta = exigirEmitible(cuenta);

        ConfiguracionSunat config = configuracionRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA)
                .orElseThrow(() -> conflicto("La facturación electrónica no está configurada"));

        if (!config.isActiva()) {
            throw conflicto("La facturación electrónica no está activa");
        }
        if (config.getRegimen() == null) {
            throw conflicto("Falta configurar el régimen tributario de la empresa");
        }

        Long sedeId = cuenta.getSede().getId();
        SedeSunat sedeSunat = sedeSunatRepository.findBySedeId(sedeId)
                .orElseThrow(() -> conflicto("La sede no tiene los datos de establecimiento para SUNAT"));

        LocalDate fecha = LocalDate.now(ZONA);
        RegimenTributario regimen = config.getRegimen();
        BigDecimal igvPct = tasaVigente(regimen, TipoImpuesto.IGV, fecha, true);
        BigDecimal ipmPct = tasaVigente(regimen, TipoImpuesto.IPM, fecha, false);

        ComprobanteCliente cliente = armarCliente(request, config.getUmbralBoletaSinDocumento(), totalCuenta);

        List<CuentaLinea> lineasCuenta = cuentaLineaRepository.findByCuentaIdConUsuario(cuentaId).stream()
                .filter(l -> l.getEstado() != EstadoLinea.ANULADA
                        && l.getTotalLinea() != null && l.getTotalLinea().signum() > 0)
                .toList();

        List<ImpuestoCalculador.Desglose> desgloses = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal ipm = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (CuentaLinea linea : lineasCuenta) {
            ImpuestoCalculador.Desglose d = ImpuestoCalculador.desglosar(linea.getTotalLinea(), igvPct, ipmPct);
            desgloses.add(d);
            base = base.add(d.base());
            igv = igv.add(d.igv());
            ipm = ipm.add(d.ipm());
            total = total.add(linea.getTotalLinea());
        }

        if (total.compareTo(totalCuenta) != 0 || base.add(igv).add(ipm).compareTo(total) != 0) {
            throw conflicto("Los importes del comprobante no cuadran con la cuenta. No se emitió nada");
        }

        TipoComprobante tipo = request.tipo();
        SerieComprobante serie = serieRepository.findBySedeIdAndTipoForUpdate(sedeId, tipo)
                .orElseThrow(() -> conflicto("La sede no tiene serie de " + (tipo == TipoComprobante.FACTURA ? "factura" : "boleta")));

        long correlativo = serie.getUltimoCorrelativo() + 1;

        if (correlativo > CORRELATIVO_MAXIMO) {
            throw conflicto("La serie " + serie.getSerie() + " agotó su numeración. Configura una nueva serie");
        }

        serie.setUltimoCorrelativo(correlativo);
        serieRepository.save(serie);

        Comprobante comprobante = comprobanteRepository.saveAndFlush(Comprobante.builder()
                .cuenta(cuenta)
                .cuentaVigenteId(cuentaId)
                .sede(cuenta.getSede())
                .tipo(tipo)
                .serie(serie.getSerie())
                .correlativo(correlativo)
                .fechaEmision(fecha)
                .emitidoPorId(actor.getId())
                .emitidoPorNombre(actor.getUsername())
                .rucEmisor(config.getRuc())
                .razonSocialEmisor(config.getRazonSocial())
                .nombreComercialEmisor(config.getNombreComercial())
                .codigoEstablecimiento(sedeSunat.getCodigoEstablecimiento())
                .direccionFiscal(sedeSunat.getDireccionFiscal())
                .ubigeo(sedeSunat.getUbigeo())
                .ambiente(config.getAmbiente())
                .regimen(regimen)
                .igvPorcentaje(igvPct)
                .ipmPorcentaje(ipmPct)
                .cliente(cliente)
                .opGravada(base)
                .igv(igv)
                .ipm(ipm)
                .total(total)
                .build());

        List<ComprobanteLinea> lineas = new ArrayList<>();

        for (int i = 0; i < lineasCuenta.size(); i++) {
            CuentaLinea l = lineasCuenta.get(i);
            ImpuestoCalculador.Desglose d = desgloses.get(i);
            BigDecimal descuento = nvl(l.getDescuentoLinea()).add(nvl(l.getDescuentoCuenta()));

            lineas.add(ComprobanteLinea.builder()
                    .comprobante(comprobante)
                    .item(i + 1)
                    .cuentaLineaId(l.getId())
                    .descripcion(l.getNombre())
                    .unidadMedida(UNIDAD)
                    .cantidad(l.getCantidad())
                    .precioUnitario(l.getPrecioUnitario())
                    .descuento(descuento)
                    .total(l.getTotalLinea())
                    .baseImponible(d.base())
                    .igv(d.igv())
                    .ipm(d.ipm())
                    .build());
        }

        comprobanteLineaRepository.saveAll(lineas);

        operacionRepository.save(OperacionProcesada.builder()
                .clave(request.claveIdempotencia())
                .tipo(OPERACION_EMITIR)
                .cuentaId(cuentaId)
                .build());

        auditoriaService.registrar(actor, sedeId, cuentaId, AccionAuditoria.COMPROBANTE_EMITIDO, null,
                "numero=" + numero(comprobante.getSerie(), correlativo)
                        + ";tipo=" + tipo
                        + ";total=" + total.toPlainString()
                        + ";base=" + base.toPlainString()
                        + ";igv=" + igv.toPlainString()
                        + ";ipm=" + ipm.toPlainString()
                        + ";cliente=" + cliente.getTipoDocumento()
                        + (cliente.getNumeroDocumento() == null ? "" : " " + cliente.getNumeroDocumento()),
                null);

        archivoService.guardarXml(comprobante, lineas);

        return armar(comprobante, lineas);
    }

    private BigDecimal exigirEmitible(Cuenta cuenta) {
        EstadoCuenta estado = cuenta.getEstado();

        if (estado != EstadoCuenta.PAGADA && estado != EstadoCuenta.CERRADA) {
            throw conflicto("El comprobante se emite cuando la cuenta está pagada");
        }

        calculoService.recalcular(cuenta);
        BigDecimal congelado = cuenta.getTotalPrecuenta();

        if (congelado == null || cuenta.getTotal().compareTo(congelado) != 0) {
            throw conflicto("El total de la cuenta cambió después de la precuenta");
        }
        if (cuentaPagoRepository.totalAplicado(cuenta.getId()).compareTo(congelado) != 0) {
            throw conflicto("La cuenta aún tiene saldo pendiente");
        }
        if (congelado.signum() <= 0) {
            throw conflicto("La cuenta no tiene importe para comprobante");
        }

        return congelado;
    }

    private BigDecimal tasaVigente(RegimenTributario regimen, TipoImpuesto tipo, LocalDate fecha, boolean obligatoria) {
        List<TasaImpuesto> vigentes = tasaRepository.findVigentes(regimen, tipo, fecha);

        if (vigentes.size() > 1) {
            throw conflicto("Hay tasas de " + tipo + " superpuestas para esa fecha. Corrige la configuración");
        }
        if (vigentes.isEmpty()) {
            if (obligatoria) {
                throw conflicto("No hay una tasa de " + tipo + " vigente para el régimen " + regimen);
            }
            return BigDecimal.ZERO.setScale(2);
        }

        return vigentes.get(0).getPorcentaje();
    }

    private ComprobanteCliente armarCliente(ComprobanteRequests.Emitir request, BigDecimal umbral, BigDecimal total) {
        ComprobanteRequests.Cliente c = request.cliente();
        TipoDocumentoCliente tipoDoc = c == null ? TipoDocumentoCliente.SIN_DOCUMENTO : c.tipoDocumento();
        String numero = c == null ? null : limpiar(c.numeroDocumento());
        String nombre = c == null ? null : limpiar(c.nombre());

        if (!DocumentoValidador.esValido(tipoDoc, numero)) {
            throw invalida("El número de documento no es válido para el tipo elegido");
        }

        if (request.tipo() == TipoComprobante.FACTURA) {
            if (tipoDoc != TipoDocumentoCliente.RUC) {
                throw invalida("La factura se emite a un cliente con RUC");
            }
            if (nombre == null) {
                throw invalida("Indica la razón social del cliente");
            }
        } else if (tipoDoc == TipoDocumentoCliente.SIN_DOCUMENTO) {
            if (umbral != null && total.compareTo(umbral) > 0) {
                throw invalida("Para este monto la boleta requiere el documento del cliente");
            }
            nombre = CLIENTES_VARIOS;
        } else if (nombre == null) {
            throw invalida("Indica el nombre del cliente");
        }

        return ComprobanteCliente.builder()
                .tipoDocumento(tipoDoc)
                .numeroDocumento(tipoDoc == TipoDocumentoCliente.SIN_DOCUMENTO ? null : numero)
                .nombre(nombre)
                .direccion(c == null ? null : limpiar(c.direccion()))
                .correo(c == null ? null : limpiar(c.correo()))
                .telefono(c == null ? null : limpiar(c.telefono()))
                .build();
    }

    private ComprobanteDTO armar(Comprobante comprobante) {
        return armar(comprobante, comprobanteLineaRepository.findByComprobanteIdOrderByItem(comprobante.getId()));
    }

    private ComprobanteDTO armar(Comprobante c, List<ComprobanteLinea> lineas) {
        ComprobanteCliente cl = c.getCliente();

        return new ComprobanteDTO(
                c.getId(),
                c.getCuenta().getId(),
                c.getSede().getId(),
                c.getTipo(),
                c.getSerie(),
                c.getCorrelativo(),
                numero(c.getSerie(), c.getCorrelativo()),
                c.getFechaEmision(),
                c.getEmitidoEn(),
                c.getEmitidoPorNombre(),
                c.getEstadoSunat(),
                c.getRucEmisor(),
                c.getRazonSocialEmisor(),
                c.getRegimen(),
                c.getIgvPorcentaje(),
                c.getIpmPorcentaje(),
                new ComprobanteDTO.Cliente(cl.getTipoDocumento(), cl.getNumeroDocumento(), cl.getNombre(),
                        cl.getDireccion(), cl.getCorreo(), cl.getTelefono()),
                c.getOpGravada(),
                c.getIgv(),
                c.getIpm(),
                c.getTotal(),
                lineas.stream()
                        .map(l -> new ComprobanteDTO.Linea(l.getItem(), l.getDescripcion(), l.getUnidadMedida(),
                                l.getCantidad(), l.getPrecioUnitario(), l.getDescuento(), l.getTotal(),
                                l.getBaseImponible(), l.getIgv(), l.getIpm()))
                        .toList());
    }

    private String numero(String serie, long correlativo) {
        return serie + "-" + String.format("%08d", correlativo);
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private ResponseStatusException noExiste(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}