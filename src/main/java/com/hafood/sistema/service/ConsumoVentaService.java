package com.hafood.sistema.service;

import com.hafood.sistema.constant.EstadoConsumo;
import com.hafood.sistema.constant.TipoAlertaStock;
import com.hafood.sistema.constant.TipoCategoria;
import com.hafood.sistema.constant.TipoMovimiento;
import com.hafood.sistema.constant.TipoUbicacion;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.pos.AlertaStock;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.pos.CuentaLinea;
import com.hafood.sistema.domain.pos.CuentaMesa;
import com.hafood.sistema.domain.pos.LineaConsumo;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.AvisoStockDTO;
import com.hafood.sistema.repository.AlertaStockRepository;
import com.hafood.sistema.repository.CuentaMesaRepository;
import com.hafood.sistema.repository.LineaConsumoRepository;
import com.hafood.sistema.repository.UbicacionRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import com.hafood.sistema.repository.barra.RecetaBebidaRepository;
import com.hafood.sistema.repository.cocina.RecetaPlatoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumoVentaService {

    private static final int ESCALA = 3;
    private static final int LIMITE_DESCRIPCION = 300;
    private static final int LIMITE_MOTIVO = 255;

    private final RecetaPlatoRepository recetaPlatoRepository;
    private final RecetaBebidaRepository recetaBebidaRepository;
    private final UbicacionRepository ubicacionRepository;
    private final CuentaMesaRepository cuentaMesaRepository;
    private final LineaConsumoRepository lineaConsumoRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    private record Ingrediente(Insumo insumo, BigDecimal cantidad) {
    }

    private record ItemPlan(CuentaLinea linea, Insumo insumo, Ubicacion ubicacion, BigDecimal cantidad) {
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<AvisoStockDTO> descontar(Cuenta cuenta, List<CuentaLinea> lineas, Usuario actor) {
        Long sedeId = cuenta.getSede().getId();
        Long cuentaId = cuenta.getId();
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        Ubicacion almacen = ubicacionRepository.findFirstBySedeIdAndTipo(sedeId, TipoUbicacion.ALMACEN).orElse(null);
        Ubicacion barra = resolverBarra(cuentaId);
        Ubicacion destinoBebida = barra != null ? barra : almacen;

        List<AvisoStockDTO> avisos = new ArrayList<>();
        List<ItemPlan> plan = new ArrayList<>();

        for (CuentaLinea linea : lineas) {
            String detalle = linea.getCantidad() + "x " + linea.getNombre();
            List<Ingrediente> ingredientes = ingredientesDe(linea);

            if (ingredientes.isEmpty()) {
                alertar(sedeId, cuentaId, linea.getId(), null, TipoAlertaStock.SIN_RECETA, null,
                        "«" + linea.getNombre() + "» no tiene receta, no se descontó stock (" + detalle
                                + ", cuenta #" + cuentaId + ")");
                continue;
            }

            Ubicacion ubicacion = linea.getTipo() == TipoCategoria.BEBIDA ? destinoBebida : almacen;

            if (ubicacion == null) {
                String texto = "No hay barra ni almacén para descontar «" + detalle + "» (cuenta #" + cuentaId + ")";
                alertar(sedeId, cuentaId, linea.getId(), null, TipoAlertaStock.SIN_UBICACION, null, texto);
                avisos.add(new AvisoStockDTO(TipoAlertaStock.SIN_UBICACION, linea.getId(),
                        "No hay ubicación de stock para «" + linea.getNombre() + "». La venta continúa."));
                continue;
            }

            for (Ingrediente ingrediente : ingredientes) {
                BigDecimal cantidad = ingrediente.cantidad()
                        .multiply(BigDecimal.valueOf(linea.getCantidad()))
                        .setScale(ESCALA, RoundingMode.HALF_UP);

                if (cantidad.signum() > 0) {
                    plan.add(new ItemPlan(linea, ingrediente.insumo(), ubicacion, cantidad));
                }
            }
        }

        plan.sort(Comparator
                .comparing((ItemPlan i) -> i.ubicacion().getId())
                .thenComparing(i -> i.insumo().getId())
                .thenComparing(i -> i.linea().getId()));

        for (ItemPlan item : plan) {
            CuentaLinea linea = item.linea();
            Insumo insumo = item.insumo();
            String detalle = linea.getCantidad() + "x " + linea.getNombre();

            InventarioService.ResultadoSalida resultado = inventarioService.salidaPermitiendoFaltante(
                    insumo, item.ubicacion(), usuario, item.cantidad(), TipoMovimiento.SALIDA_VENTA,
                    recortar("Venta: " + detalle, LIMITE_MOTIVO), referencia(cuentaId, linea.getId()));

            lineaConsumoRepository.save(LineaConsumo.builder()
                    .linea(linea)
                    .insumo(insumo)
                    .ubicacion(item.ubicacion())
                    .cantidad(item.cantidad())
                    .estado(EstadoConsumo.DESCONTADO)
                    .build());

            if (resultado.faltante().signum() > 0 && insumo.isControlEstricto()) {
                String faltante = numero(resultado.faltante()) + " " + unidad(insumo);
                alertar(sedeId, cuentaId, linea.getId(), insumo.getId(), TipoAlertaStock.STOCK_INSUFICIENTE,
                        resultado.faltante(),
                        "Faltan " + faltante + " de " + insumo.getNombre() + " en " + item.ubicacion().getNombre()
                                + " por «" + detalle + "» (cuenta #" + cuentaId + ")");
                avisos.add(new AvisoStockDTO(TipoAlertaStock.STOCK_INSUFICIENTE, linea.getId(),
                        "Stock insuficiente de " + insumo.getNombre() + " en " + item.ubicacion().getNombre()
                                + " (faltan " + faltante + "). La venta continúa."));
            }
        }

        return avisos;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revertir(Cuenta cuenta, CuentaLinea linea, boolean merma, Usuario actor) {
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        String referencia = referencia(cuenta.getId(), linea.getId());
        String detalle = linea.getCantidad() + "x " + linea.getNombre();
        List<LineaConsumo> consumos = lineaConsumoRepository.findByLineaIdYEstado(linea.getId(), EstadoConsumo.DESCONTADO);

        for (LineaConsumo consumo : consumos) {
            inventarioService.entradaSinValidar(consumo.getInsumo(), consumo.getUbicacion(), usuario,
                    consumo.getCantidad(), TipoMovimiento.ENTRADA_AJUSTE,
                    recortar("Anulación de venta: " + detalle, LIMITE_MOTIVO), referencia);

            if (merma) {
                inventarioService.salidaPermitiendoFaltante(consumo.getInsumo(), consumo.getUbicacion(), usuario,
                        consumo.getCantidad(), TipoMovimiento.SALIDA_MERMA,
                        recortar("Merma por anulación: " + detalle, LIMITE_MOTIVO), referencia);
                consumo.setEstado(EstadoConsumo.MERMA);
            } else {
                consumo.setEstado(EstadoConsumo.REPUESTO);
            }
        }

        lineaConsumoRepository.saveAll(consumos);
    }

    private List<Ingrediente> ingredientesDe(CuentaLinea linea) {
        if (linea.getTipo() == TipoCategoria.BEBIDA) {
            return recetaBebidaRepository.findAllByBebidaId(linea.getProductoId()).stream()
                    .map(r -> new Ingrediente(r.getInsumo(), r.getCantidad()))
                    .toList();
        }

        return recetaPlatoRepository.findAllByPlatoId(linea.getProductoId()).stream()
                .map(r -> new Ingrediente(r.getInsumo(), r.getCantidad()))
                .toList();
    }

    private Ubicacion resolverBarra(Long cuentaId) {
        List<CuentaMesa> filas = cuentaMesaRepository.findVigentesByCuentaId(cuentaId);

        if (filas.isEmpty()) {
            return null;
        }

        return ubicacionRepository.findBySeccionId(filas.get(0).getMesa().getSeccion().getId()).orElse(null);
    }

    private void alertar(Long sedeId, Long cuentaId, Long lineaId, Long insumoId,
                         TipoAlertaStock tipo, BigDecimal faltante, String descripcion) {
        alertaStockRepository.save(AlertaStock.builder()
                .sedeId(sedeId)
                .cuentaId(cuentaId)
                .lineaId(lineaId)
                .insumoId(insumoId)
                .tipo(tipo)
                .cantidadFaltante(faltante)
                .descripcion(recortar(descripcion, LIMITE_DESCRIPCION))
                .build());
    }

    private String referencia(Long cuentaId, Long lineaId) {
        return "CUENTA-" + cuentaId + "-LINEA-" + lineaId;
    }

    private String unidad(Insumo insumo) {
        return insumo.getUnidadBase().name().toLowerCase();
    }

    private String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private String recortar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }
}