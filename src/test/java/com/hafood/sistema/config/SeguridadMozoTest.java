package com.hafood.sistema.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@WebMvcTest(controllers = SeguridadMozoTest.Sonda.class)
@Import({SecurityConfig.class, SeguridadMozoTest.Sonda.class})
class SeguridadMozoTest {

    @RestController
    static class Sonda {

        @RequestMapping("/**")
        ResponseEntity<Void> responder() {
            return ResponseEntity.ok().build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TenantProvisioningService tenantProvisioningService;

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "GET,/api/v1/pos/cartas,ok,ok,ok",
            "GET,/api/v1/pos/cartas/1/menu,ok,ok,ok",
            "GET,/api/v1/pos/cuentas/1,ok,ok,ok",
            "GET,/api/v1/pos/cuentas/1/desglose,ok,ok,ok",
            "POST,/api/v1/pos/cuentas,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/lineas,ok,ok,ok",
            "PUT,/api/v1/pos/cuentas/1/lineas/2,ok,ok,ok",
            "DELETE,/api/v1/pos/cuentas/1/lineas/2,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/lineas/2/anular,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/cambiar-mesa,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/juntar-mesa,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/comandas,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/precuenta,ok,ok,ok",
            "GET,/api/v1/mesas,ok,ok,ok",
            "GET,/api/v1/personal/activos,ok,ok,ok",
            "GET,/api/sedes,ok,ok,ok",
            "GET,/api/secciones,ok,ok,ok",
            "POST,/api/v1/pos/cuentas/1/pagos,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/propinas,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/descuentos,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/descuentos/3/quitar,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/anular,403,ok,ok",
            "PUT,/api/v1/pos/cuentas/1/mozo,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/cerrar,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/separar,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/reunir,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/reabrir,403,ok,ok",
            "GET,/api/v1/pos/cuentas/1/vuelto-opciones,403,ok,ok",
            "GET,/api/v1/pos/cuentas/1/comprobante,403,ok,ok",
            "POST,/api/v1/pos/cuentas/1/comprobante,403,ok,ok",
            "GET,/api/v1/pos/comprobantes,403,ok,ok",
            "GET,/api/v1/pos/comprobantes/1/cdr,403,ok,ok",
            "POST,/api/v1/pos/comprobantes/1/enviar,403,403,ok",
            "POST,/api/v1/pos/comprobantes/enviar-pendientes,403,403,ok",
            "GET,/api/v1/pos/estaciones/BARRA/lineas,403,ok,ok",
            "PUT,/api/v1/pos/lineas/1/estado,403,ok,ok",
            "POST,/api/v1/pos/lineas/1/visto,403,ok,ok",
            "GET,/api/v1/pos/alertas,403,403,ok",
            "POST,/api/v1/pos/alertas/1/revisar,403,403,ok",
            "GET,/api/v1/caja/sesiones/1,403,ok,ok",
            "GET,/api/v1/cajas,403,403,ok",
            "GET,/api/v1/sunat/config,403,403,403",
            "GET,/api/v1/sunat/sedes/1,403,403,ok",
            "GET,/api/usuarios,403,403,403",
            "GET,/api/v1/cartas,403,403,403",
            "GET,/api/platos,403,ok,ok",
            "GET,/api/bebidas,403,ok,ok",
            "GET,/api/recetas-platos,403,ok,ok",
            "GET,/api/insumos,403,ok,ok",
            "GET,/api/v1/inventario/stock,403,ok,ok",
            "POST,/api/v1/inventario/compras,403,403,ok",
            "POST,/api/platos,403,403,403",
            "POST,/api/v1/mesas,403,403,ok",
            "POST,/api/v1/personal,403,403,ok"
    })
    void matrizPorRol(String metodo, String ruta, String mozo, String cajero, String encargado) throws Exception {
        verificar(metodo, ruta, "MOZO", mozo);
        verificar(metodo, ruta, "CAJERO", cajero);
        verificar(metodo, ruta, "ENCARGADO_SEDE", encargado);
    }

    @Test
    void sinSesionDevuelve401() throws Exception {
        int estado = mockMvc.perform(request(HttpMethod.GET, "/api/v1/pos/cuentas/1")).andReturn().getResponse().getStatus();
        assertEquals(401, estado);
    }

    @Test
    void laCartaPublicaNoPideSesion() throws Exception {
        int estado = mockMvc.perform(request(HttpMethod.GET, "/api/v1/publico/cartas/abc")).andReturn().getResponse().getStatus();
        assertEquals(200, estado);
    }

    private void verificar(String metodo, String ruta, String rol, String esperado) throws Exception {
        int estado = mockMvc.perform(request(HttpMethod.valueOf(metodo), ruta).with(user("prueba").roles(rol)))
                .andReturn().getResponse().getStatus();
        assertEquals("ok".equals(esperado) ? 200 : 403, estado, rol + " " + metodo + " " + ruta);
    }
}