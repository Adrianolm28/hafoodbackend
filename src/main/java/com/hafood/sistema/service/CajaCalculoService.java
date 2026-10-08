package com.hafood.sistema.service;

import com.hafood.sistema.constant.MarcaTarjeta;
import com.hafood.sistema.constant.MetodoPago;
import com.hafood.sistema.constant.Moneda;
import com.hafood.sistema.constant.TipoMovimientoCaja;
import com.hafood.sistema.dto.request.CajaRequests;
import com.hafood.sistema.repository.CajaMovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CajaCalculoService {

    public record Clave(MetodoPago metodo, MarcaTarjeta marca, Moneda moneda) {
    }

    public record Saldo(Clave clave, BigDecimal apertura, BigDecimal cobros, BigDecimal propinas,
                        BigDecimal ingresos, BigDecimal egresos, BigDecimal devoluciones, BigDecimal esperado) {
    }

    public record Contado(BigDecimal total, String detalle) {
    }

    public record LineaEvaluada(Clave clave, BigDecimal esperado, BigDecimal contado,
                                BigDecimal diferencia, String detalle) {
    }

    public record Evaluacion(List<LineaEvaluada> lineas, boolean hayDiferencia,
                             int lineasConDiferencia, BigDecimal diferenciaPen) {
    }

    public static final List<Clave> PLANTILLA = List.of(
            new Clave(MetodoPago.EFECTIVO, null, Moneda.PEN),
            new Clave(MetodoPago.EFECTIVO, null, Moneda.USD),
            new Clave(MetodoPago.TARJETA, MarcaTarjeta.VISA, Moneda.PEN),
            new Clave(MetodoPago.TARJETA, MarcaTarjeta.MASTERCARD, Moneda.PEN),
            new Clave(MetodoPago.TARJETA, MarcaTarjeta.AMEX, Moneda.PEN),
            new Clave(MetodoPago.TARJETA, MarcaTarjeta.OTRA, Moneda.PEN),
            new Clave(MetodoPago.YAPE, null, Moneda.PEN),
            new Clave(MetodoPago.PLIN, null, Moneda.PEN),
            new Clave(MetodoPago.TRANSFERENCIA, null, Moneda.PEN));

    private static final Map<Moneda, List<BigDecimal>> DENOMINACIONES = Map.of(
            Moneda.PEN, valores("200", "100", "50", "20", "10", "5", "2", "1", "0.5", "0.2", "0.1"),
            Moneda.USD, valores("100", "50", "20", "10", "5", "2", "1"));

    private final CajaMovimientoRepository cajaMovimientoRepository;

    public static String etiqueta(Clave clave) {
        return switch (clave.metodo()) {
            case EFECTIVO -> clave.moneda() == Moneda.USD ? "Efectivo en dólares" : "Efectivo en soles";
            case TARJETA -> "Tarjeta " + marca(clave.marca());
            case YAPE -> "Yape";
            case PLIN -> "Plin";
            case TRANSFERENCIA -> "Transferencia";
        };
    }

    public List<Saldo> saldos(Long sesionId) {
        return acumular(sesionId).entrySet().stream()
                .map(e -> aSaldo(e.getKey(), e.getValue()))
                .toList();
    }

    public BigDecimal saldo(Long sesionId, MetodoPago metodo, MarcaTarjeta marca, Moneda moneda) {
        Clave clave = new Clave(metodo, marca, moneda);
        Map<TipoMovimientoCaja, BigDecimal> valores = acumular(sesionId).get(clave);
        return aSaldo(clave, valores == null ? new EnumMap<>(TipoMovimientoCaja.class) : valores).esperado();
    }

    public Contado contar(Clave clave, CajaRequests.LineaConteo linea) {
        List<CajaRequests.Denominacion> detalle = linea.denominaciones() == null
                ? List.of()
                : linea.denominaciones().stream().filter(d -> d.cantidad() > 0).toList();

        if (clave.metodo() != MetodoPago.EFECTIVO && !detalle.isEmpty()) {
            throw invalida("Solo el efectivo se cuenta por billetes y monedas");
        }

        if (detalle.isEmpty()) {
            if (linea.contado() == null) {
                throw invalida("Indica lo contado en " + etiqueta(clave) + " (0 si no hay)");
            }
            return new Contado(linea.contado().setScale(2, RoundingMode.HALF_UP), null);
        }

        List<BigDecimal> validas = DENOMINACIONES.get(clave.moneda());
        BigDecimal total = BigDecimal.ZERO;
        List<BigDecimal> usadas = new ArrayList<>();
        StringBuilder texto = new StringBuilder();

        for (CajaRequests.Denominacion d : detalle) {
            BigDecimal valor = validas.stream().filter(v -> v.compareTo(d.valor()) == 0).findFirst()
                    .orElseThrow(() -> invalida("Una denominación no es válida para " + etiqueta(clave)));

            if (usadas.stream().anyMatch(u -> u.compareTo(valor) == 0)) {
                throw invalida("Hay una denominación repetida en " + etiqueta(clave));
            }

            usadas.add(valor);
            total = total.add(valor.multiply(BigDecimal.valueOf(d.cantidad())));

            if (!texto.isEmpty()) {
                texto.append(',');
            }
            texto.append(valor.stripTrailingZeros().toPlainString()).append(':').append(d.cantidad());
        }

        return new Contado(total.setScale(2, RoundingMode.HALF_UP), texto.toString());
    }

    public Evaluacion evaluar(Long sesionId, List<CajaRequests.LineaConteo> conteo) {
        Map<Clave, CajaRequests.LineaConteo> porClave = new LinkedHashMap<>();

        for (CajaRequests.LineaConteo linea : conteo) {
            Clave clave = new Clave(linea.metodo(), linea.marca(), linea.moneda());

            if (!PLANTILLA.contains(clave)) {
                throw invalida("El conteo incluye una línea que no corresponde a la caja");
            }
            if (porClave.put(clave, linea) != null) {
                throw invalida("Hay una línea repetida en el conteo: " + etiqueta(clave));
            }
        }

        for (Clave clave : PLANTILLA) {
            if (!porClave.containsKey(clave)) {
                throw invalida("Falta contar: " + etiqueta(clave));
            }
        }

        Map<Clave, Map<TipoMovimientoCaja, BigDecimal>> acumulado = acumular(sesionId);
        List<LineaEvaluada> lineas = new ArrayList<>();
        BigDecimal diferenciaPen = BigDecimal.ZERO;
        int conDiferencia = 0;

        for (Clave clave : PLANTILLA) {
            Contado contado = contar(clave, porClave.get(clave));
            BigDecimal esperado = aSaldo(clave, acumulado.get(clave)).esperado();
            BigDecimal diferencia = contado.total().subtract(esperado).setScale(2, RoundingMode.HALF_UP);

            if (diferencia.signum() != 0) {
                conDiferencia++;
                diferenciaPen = diferenciaPen.add(clave.moneda().aSoles(diferencia));
            }

            lineas.add(new LineaEvaluada(clave, esperado, contado.total(), diferencia, contado.detalle()));
        }

        return new Evaluacion(lineas, conDiferencia > 0, conDiferencia, diferenciaPen);
    }

    private Map<Clave, Map<TipoMovimientoCaja, BigDecimal>> acumular(Long sesionId) {
        Map<Clave, Map<TipoMovimientoCaja, BigDecimal>> mapa = new LinkedHashMap<>();
        PLANTILLA.forEach(c -> mapa.put(c, new EnumMap<>(TipoMovimientoCaja.class)));

        for (Object[] fila : cajaMovimientoRepository.sumarPorSesion(sesionId)) {
            Clave clave = new Clave((MetodoPago) fila[0], (MarcaTarjeta) fila[1], (Moneda) fila[2]);
            mapa.computeIfAbsent(clave, k -> new EnumMap<>(TipoMovimientoCaja.class))
                    .merge((TipoMovimientoCaja) fila[3], (BigDecimal) fila[4], BigDecimal::add);
        }

        return mapa;
    }

    private Saldo aSaldo(Clave clave, Map<TipoMovimientoCaja, BigDecimal> valores) {
        BigDecimal esperado = BigDecimal.ZERO;

        for (Map.Entry<TipoMovimientoCaja, BigDecimal> e : valores.entrySet()) {
            esperado = esperado.add(e.getValue().multiply(BigDecimal.valueOf(e.getKey().signo())));
        }

        return new Saldo(clave,
                valor(valores, TipoMovimientoCaja.APERTURA),
                valor(valores, TipoMovimientoCaja.COBRO),
                valor(valores, TipoMovimientoCaja.PROPINA),
                valor(valores, TipoMovimientoCaja.INGRESO),
                valor(valores, TipoMovimientoCaja.EGRESO),
                valor(valores, TipoMovimientoCaja.DEVOLUCION),
                esperado.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal valor(Map<TipoMovimientoCaja, BigDecimal> valores, TipoMovimientoCaja tipo) {
        return valores.getOrDefault(tipo, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static String marca(MarcaTarjeta marca) {
        return switch (marca) {
            case VISA -> "Visa";
            case MASTERCARD -> "Mastercard";
            case AMEX -> "American Express";
            case OTRA -> "otra marca";
        };
    }

    private static List<BigDecimal> valores(String... textos) {
        return java.util.Arrays.stream(textos).map(BigDecimal::new).collect(Collectors.toUnmodifiableList());
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}