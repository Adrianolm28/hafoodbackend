package com.hafood.sistema.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hafood.sunat")
public class SunatProperties {

    private int plazoFacturaDias = 3;
    private int plazoBoletaDias = 7;
    private long envioPausaMs = 1000;
    private int automaticoMaxPorCiclo = 60;
    private int automaticoMaxIntentos = 8;
}