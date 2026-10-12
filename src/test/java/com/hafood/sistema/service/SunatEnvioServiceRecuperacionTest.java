package com.hafood.sistema.service;

import com.hafood.sistema.config.SunatProperties;
import com.hafood.sistema.constant.AmbienteSunat;
import com.hafood.sistema.constant.EstadoSunat;
import com.hafood.sistema.constant.TipoComprobante;
import com.hafood.sistema.domain.estructura.Sede;
import com.hafood.sistema.domain.pos.Cuenta;
import com.hafood.sistema.domain.sunat.Comprobante;
import com.hafood.sistema.domain.sunat.ComprobanteArchivo;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.EnvioResultadoDTO;
import com.hafood.sistema.repository.ComprobanteArchivoRepository;
import com.hafood.sistema.repository.ComprobanteRepository;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SunatEnvioServiceRecuperacionTest {

    private static final String RUC = "20000000001";

    private ComprobanteRepository comprobanteRepository;
    private ComprobanteArchivoRepository archivoRepository;
    private SunatGateway gateway;
    private Comprobante comprobante;
    private ComprobanteArchivo archivo;
    private SunatEnvioService servicio;
    private Usuario actor;

    @BeforeEach
    void preparar() {
        comprobanteRepository = mock(ComprobanteRepository.class);
        archivoRepository = mock(ComprobanteArchivoRepository.class);
        ConfiguracionSunatRepository configRepository = mock(ConfiguracionSunatRepository.class);
        CifradoService cifrado = mock(CifradoService.class);
        gateway = mock(SunatGateway.class);
        UblComprobanteGenerador generador = mock(UblComprobanteGenerador.class);
        SedeAccesoService sedeAcceso = mock(SedeAccesoService.class);
        AuditoriaService auditoria = mock(AuditoriaService.class);
        PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);

        Sede sede = mock(Sede.class);
        Cuenta cuenta = mock(Cuenta.class);
        ConfiguracionSunat config = mock(ConfiguracionSunat.class);
        comprobante = mock(Comprobante.class);
        actor = mock(Usuario.class);

        when(sede.getId()).thenReturn(5L);
        when(cuenta.getId()).thenReturn(9L);
        when(config.isActiva()).thenReturn(true);
        when(config.getRuc()).thenReturn(RUC);
        when(config.getAmbiente()).thenReturn(AmbienteSunat.BETA);
        when(comprobante.getSede()).thenReturn(sede);
        when(comprobante.getCuenta()).thenReturn(cuenta);
        when(comprobante.getEstadoSunat()).thenReturn(EstadoSunat.ERROR_ENVIO);
        when(comprobante.getRucEmisor()).thenReturn(RUC);
        when(comprobante.getAmbiente()).thenReturn(AmbienteSunat.BETA);
        when(comprobante.getTipo()).thenReturn(TipoComprobante.BOLETA);
        when(comprobante.getSerie()).thenReturn("B001");
        when(comprobante.getCorrelativo()).thenReturn(4L);
        when(generador.nombreBase(comprobante)).thenReturn("BASE");
        when(generador.numero(comprobante)).thenReturn("B001-00000004");
        when(comprobanteRepository.findById(1L)).thenReturn(Optional.of(comprobante));
        when(configRepository.findByClaveUnica(any())).thenReturn(Optional.of(config));

        archivo = new ComprobanteArchivo();
        archivo.setFirmado(true);
        archivo.setXml(new byte[]{1, 2, 3});
        when(archivoRepository.findByComprobanteId(1L)).thenReturn(Optional.of(archivo));

        servicio = new SunatEnvioService(comprobanteRepository, archivoRepository, configRepository, cifrado,
                gateway, generador, sedeAcceso, auditoria, transacciones, new SunatProperties());
    }

    private void sunatDice1033() {
        when(gateway.enviarComprobante(any(), any(), any(), any())).thenReturn(
                new SunatGateway.RespuestaSunat(EstadoSunat.ERROR_ENVIO, "1033",
                        "El comprobante fue registrado previamente con otros datos", null));
    }

    @Test
    void recuperaLaConstanciaCuandoYaHuboUnIntento() {
        byte[] cdr = new byte[]{9, 9, 9};
        archivo.setIntentos(1);
        sunatDice1033();
        when(gateway.consultarConstancia(any(), any(), eq(RUC), eq("03"), eq("B001"), eq(4L))).thenReturn(
                new SunatGateway.RespuestaSunat(EstadoSunat.ACEPTADO, "0", "La Boleta B001-4 fue aceptada", cdr));

        EnvioResultadoDTO resultado = servicio.enviar(1L, actor);

        assertEquals(EstadoSunat.ACEPTADO, resultado.estado());
        assertArrayEquals(cdr, archivo.getCdr());
        assertEquals(2, archivo.getIntentos());
        verify(comprobante).setEstadoSunat(EstadoSunat.ACEPTADO);
    }

    @Test
    void siLaConsultaFallaQuedaEnErrorConUnMensajeClaro() {
        archivo.setIntentos(2);
        sunatDice1033();
        when(gateway.consultarConstancia(any(), any(), any(), any(), any(), eq(4L))).thenReturn(
                new SunatGateway.RespuestaSunat(EstadoSunat.ERROR_ENVIO, "0001", "El comprobante existe", null));

        EnvioResultadoDTO resultado = servicio.enviar(1L, actor);

        assertEquals(EstadoSunat.ERROR_ENVIO, resultado.estado());
        assertEquals("1033", resultado.codigo());
        verify(comprobante).setEstadoSunat(EstadoSunat.ERROR_ENVIO);
    }

    @Test
    void enElPrimerEnvioNoAdoptaUnaConstanciaAjena() {
        sunatDice1033();

        EnvioResultadoDTO resultado = servicio.enviar(1L, actor);

        assertEquals(EstadoSunat.ERROR_ENVIO, resultado.estado());
        verify(gateway, never()).consultarConstancia(any(), any(), any(), any(), any(), eq(4L));
    }
}