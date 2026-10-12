package com.hafood.sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        String[] rolesSala = {"SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE", "CAJERO", "MOZO"};

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/publico/**").permitAll()

                        .requestMatchers("/api/v1/cartas/**", "/api/v1/catalogo/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/v1/personal/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/mesas/**").authenticated()
                        .requestMatchers("/api/v1/pos/estaciones/**", "/api/v1/pos/lineas/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE", "CAJERO", "BARTENDER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pos/cuentas/*/descuentos/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE", "CAJERO")
                        .requestMatchers("/api/v1/pos/alertas/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/sunat/sedes/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")
                        .requestMatchers("/api/v1/sunat/config/**", "/api/v1/sunat/sedes/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/pos/comprobantes/enviar-pendientes",
                                "/api/v1/pos/comprobantes/*/enviar",
                                "/api/v1/pos/comprobantes/*/xml/regenerar")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/pos/cartas",
                                "/api/v1/pos/cartas/*/menu",
                                "/api/v1/pos/cuentas/*",
                                "/api/v1/pos/cuentas/*/desglose")
                        .hasAnyRole(rolesSala)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/pos/cuentas",
                                "/api/v1/pos/cuentas/*/lineas",
                                "/api/v1/pos/cuentas/*/lineas/*/anular",
                                "/api/v1/pos/cuentas/*/cambiar-mesa",
                                "/api/v1/pos/cuentas/*/juntar-mesa",
                                "/api/v1/pos/cuentas/*/comandas",
                                "/api/v1/pos/cuentas/*/precuenta")
                        .hasAnyRole(rolesSala)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pos/cuentas/*/lineas/*")
                        .hasAnyRole(rolesSala)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pos/cuentas/*/lineas/*")
                        .hasAnyRole(rolesSala)

                        .requestMatchers("/api/v1/pos/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE", "CAJERO")

                        .requestMatchers("/api/v1/cajas/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")
                        .requestMatchers("/api/v1/caja/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE", "CAJERO")
                        .requestMatchers("/api/v1/mesas/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")
                        .requestMatchers("/api/v1/personal/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")
                        .requestMatchers(HttpMethod.GET,
                                "/api/sedes/**",
                                "/api/secciones/**",
                                "/api/categorias/**",
                                "/api/insumos/**",
                                "/api/platos/**",
                                "/api/bebidas/**",
                                "/api/recetas-platos/**",
                                "/api/recetas-bebidas/**",
                                "/api/v1/inventario/**"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/v1/inventario/**")
                        .hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR", "ENCARGADO_SEDE")

                        .requestMatchers(HttpMethod.POST,
                                "/api/sedes/**",
                                "/api/secciones/**",
                                "/api/categorias/**",
                                "/api/insumos/**",
                                "/api/platos/**",
                                "/api/bebidas/**",
                                "/api/recetas-platos/**",
                                "/api/recetas-bebidas/**"
                        ).hasRole("SUPERADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/sedes/**",
                                "/api/secciones/**",
                                "/api/categorias/**",
                                "/api/insumos/**",
                                "/api/platos/**",
                                "/api/bebidas/**",
                                "/api/recetas-platos/**",
                                "/api/recetas-bebidas/**",
                                "/api/v1/inventario/**"
                        ).hasRole("SUPERADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/sedes/**",
                                "/api/secciones/**",
                                "/api/categorias/**",
                                "/api/insumos/**",
                                "/api/platos/**",
                                "/api/bebidas/**",
                                "/api/recetas-platos/**",
                                "/api/recetas-bebidas/**",
                                "/api/v1/inventario/**"
                        ).hasRole("SUPERADMIN")

                        .requestMatchers("/api/usuarios/**").hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")
                        .requestMatchers("/api/admin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/auth/register").hasAnyRole("SUPERADMIN", "ADMIN", "ADMINISTRADOR")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://*.localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}