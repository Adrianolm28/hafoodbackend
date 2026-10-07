package com.hafood.sistema.mapper;

import com.hafood.sistema.constant.AreaInsumo;
import com.hafood.sistema.domain.barra.Bebida;
import com.hafood.sistema.domain.barra.RecetaBebida;
import com.hafood.sistema.domain.catalogo.Categoria;
import com.hafood.sistema.domain.cocina.Plato;
import com.hafood.sistema.domain.cocina.RecetaPlato;
import com.hafood.sistema.domain.estructura.Seccion;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.inventario.Insumo;
import com.hafood.sistema.domain.inventario.MovimientoInsumo;
import com.hafood.sistema.domain.inventario.StockSede;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.*;
import com.hafood.sistema.domain.estructura.Ubicacion;
import com.hafood.sistema.domain.inventario.StockUbicacion;
import com.hafood.sistema.domain.inventario.Compra;
import com.hafood.sistema.domain.inventario.CompraDetalle;
import com.hafood.sistema.domain.inventario.InsumoRendimiento;
import com.hafood.sistema.domain.inventario.Transformacion;
import com.hafood.sistema.domain.inventario.Traspaso;
import com.hafood.sistema.domain.inventario.TraspasoDetalle;
import com.hafood.sistema.constant.EstadoPrestamo;
import com.hafood.sistema.constant.TipoTraspaso;
import com.hafood.sistema.domain.inventario.Traspaso;
import com.hafood.sistema.domain.inventario.Prestamo;
import com.hafood.sistema.domain.inventario.PrestamoDetalle;
import com.hafood.sistema.domain.inventario.Proveedor;
import java.time.LocalDate;

import java.math.BigDecimal;

public class Mapper {

    private Mapper() {}

    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;

        UsuarioDTO dto = UsuarioDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .build();

        if (usuario.getSede() != null) {
            dto.setSedeId(usuario.getSede().getId());
            dto.setSedeNombre(usuario.getSede().getNombre());
        }

        if (usuario.getSeccion() != null) {
            dto.setSeccionId(usuario.getSeccion().getId());
            dto.setSeccionNombre(usuario.getSeccion().getNombre());
        }

        return dto;
    }

    public static SedeDTO toDTO(Sede sede) {
        if (sede == null) return null;

        return SedeDTO.builder()
                .id(sede.getId())
                .nombre(sede.getNombre())
                .direccion(sede.getDireccion())
                .build();
    }

    public static SeccionDTO toDTO(Seccion seccion) {
        if (seccion == null) return null;

        SeccionDTO dto = SeccionDTO.builder()
                .id(seccion.getId())
                .nombre(seccion.getNombre())
                .build();

        if (seccion.getSede() != null) {
            dto.setSedeId(seccion.getSede().getId());
            dto.setSedeNombre(seccion.getSede().getNombre());
        }

        if (seccion.getJefeMozo() != null) {
            dto.setJefeMozoId(seccion.getJefeMozo().getId());
            dto.setJefeMozoNombre(seccion.getJefeMozo().getNombre());
        }

        if (seccion.getJefeBartender() != null) {
            dto.setJefeBartenderId(seccion.getJefeBartender().getId());
            dto.setJefeBartenderNombre(seccion.getJefeBartender().getNombre());
        }
        dto.setActivo(!Boolean.FALSE.equals(seccion.getActivo()));
        dto.setOrden(seccion.getOrden() == null ? 0 : seccion.getOrden());

        return dto;
    }

    public static InsumoDTO toDTO(Insumo insumo) {
        if (insumo == null) return null;

        return InsumoDTO.builder()
                .id(insumo.getId())
                .nombre(insumo.getNombre())
                .unidadMedida(insumo.getUnidadMedida())
                .unidadBase(insumo.getUnidadBase())
                .presentacionNombre(insumo.getPresentacionNombre())
                .presentacionCantidad(insumo.getPresentacionCantidad())
                .costoUnitario(insumo.getCostoUnitario())
                .controlEstricto(insumo.isControlEstricto())
                .area(insumo.getArea() == null ? AreaInsumo.AMBAS : insumo.getArea())
                .activo(insumo.getActivo())
                .build();
    }

    public static PlatoDTO toDTO(Plato plato) {
        if (plato == null) return null;

        PlatoDTO dto = PlatoDTO.builder()
                .id(plato.getId())
                .nombre(plato.getNombre())
                .descripcion(plato.getDescripcion())
                .precioVenta(plato.getPrecioVenta())
                .imagenUrl(ImagenUrl.de(plato.getImagen()))
                .activo(plato.getActivo())
                .build();

        if (plato.getCategoria() != null) {
            dto.setCategoriaId(plato.getCategoria().getId());
            dto.setCategoriaNombre(plato.getCategoria().getNombre());
        }

        return dto;
    }

    public static BebidaDTO toDTO(Bebida bebida) {
        if (bebida == null) return null;

        BebidaDTO dto = BebidaDTO.builder()
                .id(bebida.getId())
                .nombre(bebida.getNombre())
                .descripcion(bebida.getDescripcion())
                .precioVenta(bebida.getPrecioVenta())
                .imagenUrl(ImagenUrl.de(bebida.getImagen()))
                .activo(bebida.getActivo())
                .build();

        if (bebida.getCategoria() != null) {
            dto.setCategoriaId(bebida.getCategoria().getId());
            dto.setCategoriaNombre(bebida.getCategoria().getNombre());
        }

        return dto;
    }

    public static RecetaPlatoDTO toDTO(RecetaPlato receta) {
        if (receta == null) return null;

        RecetaPlatoDTO dto = RecetaPlatoDTO.builder()
                .id(receta.getId())
                .cantidad(receta.getCantidad())
                .unidadMedida(receta.getUnidadMedida())
                .build();

        if (receta.getPlato() != null) {
            dto.setPlatoId(receta.getPlato().getId());
            dto.setPlatoNombre(receta.getPlato().getNombre());
        }

        if (receta.getInsumo() != null) {
            dto.setInsumoId(receta.getInsumo().getId());
            dto.setInsumoNombre(receta.getInsumo().getNombre());
        }

        return dto;
    }

    public static RecetaBebidaDTO toDTO(RecetaBebida receta) {
        if (receta == null) return null;

        RecetaBebidaDTO dto = RecetaBebidaDTO.builder()
                .id(receta.getId())
                .cantidad(receta.getCantidad())
                .unidadMedida(receta.getUnidadMedida())
                .build();

        if (receta.getBebida() != null) {
            dto.setBebidaId(receta.getBebida().getId());
            dto.setBebidaNombre(receta.getBebida().getNombre());
        }

        if (receta.getInsumo() != null) {
            dto.setInsumoId(receta.getInsumo().getId());
            dto.setInsumoNombre(receta.getInsumo().getNombre());
        }

        return dto;
    }

    public static StockSedeDTO toDTO(StockSede stockSede) {
        if (stockSede == null) return null;

        StockSedeDTO dto = StockSedeDTO.builder()
                .id(stockSede.getId())
                .cantidadActual(stockSede.getCantidadActual())
                .build();

        if (stockSede.getInsumo() != null) {
            dto.setInsumoId(stockSede.getInsumo().getId());
            dto.setInsumoNombre(stockSede.getInsumo().getNombre());
            dto.setUnidadMedida(stockSede.getInsumo().getUnidadMedida());
        }

        if (stockSede.getSede() != null) {
            dto.setSedeId(stockSede.getSede().getId());
            dto.setSedeNombre(stockSede.getSede().getNombre());
        }

        return dto;
    }

    public static MovimientoInsumoDTO toDTO(MovimientoInsumo movimiento) {
        if (movimiento == null) return null;

        MovimientoInsumoDTO dto = MovimientoInsumoDTO.builder()
                .id(movimiento.getId())
                .tipoMovimiento(movimiento.getTipoMovimiento())
                .cantidad(movimiento.getCantidad())
                .stockResultante(movimiento.getStockResultante())
                .motivo(movimiento.getMotivo())
                .fechaMovimiento(movimiento.getFechaMovimiento())
                .referencia(movimiento.getReferencia())
                .build();

        if (movimiento.getInsumo() != null) {
            dto.setPresentacionNombre(movimiento.getInsumo().getPresentacionNombre());
            dto.setPresentacionCantidad(movimiento.getInsumo().getPresentacionCantidad());
            dto.setInsumoId(movimiento.getInsumo().getId());
            dto.setInsumoNombre(movimiento.getInsumo().getNombre());
            dto.setUnidadMedida(movimiento.getInsumo().getUnidadMedida());
        }
        if (movimiento.getSede() != null) {
            dto.setSedeId(movimiento.getSede().getId());
            dto.setSedeNombre(movimiento.getSede().getNombre());
        }
        if (movimiento.getUbicacion() != null) {
            dto.setUbicacionId(movimiento.getUbicacion().getId());
            dto.setUbicacionNombre(movimiento.getUbicacion().getNombre());
        }
        if (movimiento.getUsuario() != null) {
            dto.setUsuarioId(movimiento.getUsuario().getId());
            dto.setUsuarioNombre(movimiento.getUsuario().getUsername());
        }

        return dto;
    }

    public static UbicacionDTO toDTO(Ubicacion ubicacion) {
        if (ubicacion == null) return null;

        UbicacionDTO dto = UbicacionDTO.builder()
                .id(ubicacion.getId())
                .tipo(ubicacion.getTipo())
                .nombre(ubicacion.getNombre())
                .activo(ubicacion.getActivo())
                .build();

        if (ubicacion.getSede() != null) {
            dto.setSedeId(ubicacion.getSede().getId());
            dto.setSedeNombre(ubicacion.getSede().getNombre());
        }
        if (ubicacion.getSeccion() != null) {
            dto.setSeccionId(ubicacion.getSeccion().getId());
        }

        return dto;
    }

    public static StockUbicacionDTO toDTO(StockUbicacion stock) {
        if (stock == null) return null;

        StockUbicacionDTO dto = StockUbicacionDTO.builder()
                .id(stock.getId())
                .cantidadActual(stock.getCantidadActual())
                .build();

        if (stock.getInsumo() != null) {
            dto.setInsumoId(stock.getInsumo().getId());
            dto.setInsumoNombre(stock.getInsumo().getNombre());
            dto.setUnidadMedida(stock.getInsumo().getUnidadMedida());
        }
        if (stock.getUbicacion() != null) {
            dto.setUbicacionId(stock.getUbicacion().getId());
            dto.setUbicacionNombre(stock.getUbicacion().getNombre());
            dto.setTipoUbicacion(stock.getUbicacion().getTipo());
            if (stock.getUbicacion().getSede() != null) {
                dto.setSedeId(stock.getUbicacion().getSede().getId());
                dto.setSedeNombre(stock.getUbicacion().getSede().getNombre());
            }
        }

        return dto;
    }

    public static CategoriaDTO toDTO(Categoria categoria) {
        if (categoria == null) return null;

        return CategoriaDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .tipo(categoria.getTipo())
                .activo(categoria.getActivo())
                .build();
    }

    public static CompraDetalleDTO toDTO(CompraDetalle detalle) {
        if (detalle == null) return null;

        BigDecimal diferencia = detalle.getCantidadEsperada() == null
                ? null
                : detalle.getCantidadEsperada().subtract(detalle.getCantidadRecibida());

        CompraDetalleDTO dto = CompraDetalleDTO.builder()
                .id(detalle.getId())
                .cantidadIngresada(detalle.getCantidadIngresada())
                .unidadIngreso(detalle.getUnidadIngreso())
                .cantidadRecibida(detalle.getCantidadRecibida())
                .cantidadEsperada(detalle.getCantidadEsperada())
                .diferencia(diferencia)
                .montoPagado(detalle.getMontoPagado())
                .costoUnitario(detalle.getCostoUnitario())
                .build();

        if (detalle.getInsumo() != null) {
            dto.setInsumoId(detalle.getInsumo().getId());
            dto.setInsumoNombre(detalle.getInsumo().getNombre());
            dto.setUnidadMedida(detalle.getInsumo().getUnidadMedida());
        }

        return dto;
    }

    public static CompraDTO toDTO(Compra compra) {
        if (compra == null) return null;

        CompraDTO dto = CompraDTO.builder()
                .id(compra.getId())
                .fechaCompra(compra.getFechaCompra())
                .compradoPor(compra.getCompradoPor())
                .observacion(compra.getObservacion())
                .totalPagado(compra.getTotalPagado())
                .fechaRegistro(compra.getFechaRegistro())
                .detalles(compra.getDetalles().stream().map(Mapper::toDTO).toList())
                .build();

        if (compra.getUbicacion() != null) {
            dto.setUbicacionId(compra.getUbicacion().getId());
            dto.setUbicacionNombre(compra.getUbicacion().getNombre());
            if (compra.getProveedor() != null) {
                dto.setProveedorId(compra.getProveedor().getId());
                dto.setProveedorNombre(compra.getProveedor().getNombre());
            }
            if (compra.getUbicacion().getSede() != null) {
                dto.setSedeId(compra.getUbicacion().getSede().getId());
                dto.setSedeNombre(compra.getUbicacion().getSede().getNombre());
            }
        }
        if (compra.getFirmante() != null) {
            dto.setFirmanteId(compra.getFirmante().getId());
            dto.setFirmanteNombre(compra.getFirmante().getUsername());
        }
        if (compra.getRegistradoPor() != null) {
            dto.setRegistradoPorId(compra.getRegistradoPor().getId());
            dto.setRegistradoPorNombre(compra.getRegistradoPor().getUsername());
        }

        return dto;
    }

    public static TraspasoDetalleDTO toDTO(TraspasoDetalle detalle) {
        if (detalle == null) return null;

        TraspasoDetalleDTO dto = TraspasoDetalleDTO.builder()
                .id(detalle.getId())
                .cantidad(detalle.getCantidad())

                .build();

        if (detalle.getInsumo() != null) {
            dto.setInsumoId(detalle.getInsumo().getId());
            dto.setInsumoNombre(detalle.getInsumo().getNombre());
            dto.setUnidadMedida(detalle.getInsumo().getUnidadMedida());
        }

        return dto;
    }

    public static TraspasoDTO toDTO(Traspaso traspaso) {
        if (traspaso == null) return null;

        TraspasoDTO dto = TraspasoDTO.builder()
                .id(traspaso.getId())
                .observacion(traspaso.getObservacion())
                .fecha(traspaso.getFecha())
                .tipo(traspaso.getTipo() == null ? TipoTraspaso.NORMAL : traspaso.getTipo())
                .prestamoId(traspaso.getPrestamo() == null ? null : traspaso.getPrestamo().getId())
                .detalles(traspaso.getDetalles().stream().map(Mapper::toDTO).toList())
                .build();

        if (traspaso.getOrigen() != null) {
            dto.setOrigenId(traspaso.getOrigen().getId());
            dto.setOrigenNombre(traspaso.getOrigen().getNombre());
            if (traspaso.getOrigen().getSede() != null) {
                dto.setOrigenSedeId(traspaso.getOrigen().getSede().getId());
                dto.setOrigenSedeNombre(traspaso.getOrigen().getSede().getNombre());
            }
        }
        if (traspaso.getDestino() != null) {
            dto.setDestinoId(traspaso.getDestino().getId());
            dto.setDestinoNombre(traspaso.getDestino().getNombre());
            if (traspaso.getDestino().getSede() != null) {
                dto.setDestinoSedeId(traspaso.getDestino().getSede().getId());
                dto.setDestinoSedeNombre(traspaso.getDestino().getSede().getNombre());
            }
        }
        if (traspaso.getUsuario() != null) {
            dto.setUsuarioId(traspaso.getUsuario().getId());
            dto.setUsuarioNombre(traspaso.getUsuario().getUsername());
        }

        return dto;
    }

    public static RendimientoDTO toDTO(InsumoRendimiento rendimiento) {
        if (rendimiento == null) return null;

        RendimientoDTO dto = RendimientoDTO.builder()
                .id(rendimiento.getId())
                .rendimiento(rendimiento.getRendimiento())
                .build();

        if (rendimiento.getInsumoOrigen() != null) {
            dto.setInsumoOrigenId(rendimiento.getInsumoOrigen().getId());
            dto.setInsumoOrigenNombre(rendimiento.getInsumoOrigen().getNombre());
            dto.setUnidadOrigen(rendimiento.getInsumoOrigen().getUnidadMedida());
        }
        if (rendimiento.getInsumoDestino() != null) {
            dto.setInsumoDestinoId(rendimiento.getInsumoDestino().getId());
            dto.setInsumoDestinoNombre(rendimiento.getInsumoDestino().getNombre());
            dto.setUnidadDestino(rendimiento.getInsumoDestino().getUnidadMedida());
        }

        return dto;
    }

    public static TransformacionDTO toDTO(Transformacion transformacion) {
        if (transformacion == null) return null;

        TransformacionDTO dto = TransformacionDTO.builder()
                .id(transformacion.getId())
                .cantidadOrigen(transformacion.getCantidadOrigen())
                .cantidadDestino(transformacion.getCantidadDestino())
                .estimada(transformacion.isEstimada())
                .observacion(transformacion.getObservacion())
                .fecha(transformacion.getFecha())
                .build();

        if (transformacion.getUbicacion() != null) {
            dto.setUbicacionId(transformacion.getUbicacion().getId());
            dto.setUbicacionNombre(transformacion.getUbicacion().getNombre());
            if (transformacion.getUbicacion().getSede() != null) {
                dto.setSedeId(transformacion.getUbicacion().getSede().getId());
                dto.setSedeNombre(transformacion.getUbicacion().getSede().getNombre());
            }
        }
        if (transformacion.getInsumoOrigen() != null) {
            dto.setInsumoOrigenId(transformacion.getInsumoOrigen().getId());
            dto.setInsumoOrigenNombre(transformacion.getInsumoOrigen().getNombre());
            dto.setUnidadOrigen(transformacion.getInsumoOrigen().getUnidadMedida());
        }
        if (transformacion.getInsumoDestino() != null) {
            dto.setInsumoDestinoId(transformacion.getInsumoDestino().getId());
            dto.setInsumoDestinoNombre(transformacion.getInsumoDestino().getNombre());
            dto.setUnidadDestino(transformacion.getInsumoDestino().getUnidadMedida());
        }
        if (transformacion.getUsuario() != null) {
            dto.setUsuarioId(transformacion.getUsuario().getId());
            dto.setUsuarioNombre(transformacion.getUsuario().getUsername());
        }

        return dto;
    }

    public static ProveedorDTO toDTO(Proveedor proveedor) {
        if (proveedor == null) return null;

        return ProveedorDTO.builder()
                .id(proveedor.getId())
                .nombre(proveedor.getNombre())
                .ruc(proveedor.getRuc())
                .telefono(proveedor.getTelefono())
                .contacto(proveedor.getContacto())
                .activo(proveedor.getActivo())
                .build();
    }

    public static PrestamoDetalleDTO toDTO(PrestamoDetalle detalle) {
        if (detalle == null) return null;

        PrestamoDetalleDTO dto = PrestamoDetalleDTO.builder()
                .id(detalle.getId())
                .cantidadPrestada(detalle.getCantidadPrestada())
                .cantidadDevuelta(detalle.getCantidadDevuelta())
                .pendiente(detalle.getCantidadPrestada().subtract(detalle.getCantidadDevuelta()))
                .build();

        if (detalle.getInsumo() != null) {
            dto.setInsumoId(detalle.getInsumo().getId());
            dto.setInsumoNombre(detalle.getInsumo().getNombre());
            dto.setUnidadMedida(detalle.getInsumo().getUnidadMedida());
            dto.setPresentacionNombre(detalle.getInsumo().getPresentacionNombre());
            dto.setPresentacionCantidad(detalle.getInsumo().getPresentacionCantidad());
        }

        return dto;
    }

    public static PrestamoDTO toDTO(Prestamo prestamo) {
        if (prestamo == null) return null;

        boolean vencido = prestamo.getFechaLimite() != null
                && prestamo.getEstado() != EstadoPrestamo.DEVUELTO
                && prestamo.getFechaLimite().isBefore(LocalDate.now());

        PrestamoDTO dto = PrestamoDTO.builder()
                .id(prestamo.getId())
                .estado(prestamo.getEstado())
                .fechaLimite(prestamo.getFechaLimite())
                .vencido(vencido)
                .fecha(prestamo.getFecha())
                .detalles(prestamo.getDetalles().stream().map(Mapper::toDTO).toList())
                .build();

        Traspaso traspaso = prestamo.getTraspaso();

        if (traspaso != null) {
            dto.setTraspasoId(traspaso.getId());
            dto.setObservacion(traspaso.getObservacion());

            if (traspaso.getOrigen() != null) {
                dto.setOrigenId(traspaso.getOrigen().getId());
                dto.setOrigenNombre(traspaso.getOrigen().getNombre());
                if (traspaso.getOrigen().getSede() != null) {
                    dto.setPrestamistaSedeId(traspaso.getOrigen().getSede().getId());
                    dto.setPrestamistaSedeNombre(traspaso.getOrigen().getSede().getNombre());
                }
            }
            if (traspaso.getDestino() != null) {
                dto.setDestinoId(traspaso.getDestino().getId());
                dto.setDestinoNombre(traspaso.getDestino().getNombre());
                if (traspaso.getDestino().getSede() != null) {
                    dto.setPrestatarioSedeId(traspaso.getDestino().getSede().getId());
                    dto.setPrestatarioSedeNombre(traspaso.getDestino().getSede().getNombre());
                }
            }
            if (traspaso.getUsuario() != null) {
                dto.setUsuarioNombre(traspaso.getUsuario().getUsername());
            }
        }

        return dto;
    }

}