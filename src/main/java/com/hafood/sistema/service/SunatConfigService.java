package com.hafood.sistema.service;

import com.hafood.sistema.constant.AccionAuditoria;
import com.hafood.sistema.domain.sunat.ConfiguracionSunat;
import com.hafood.sistema.domain.user.Usuario;
import com.hafood.sistema.dto.ConfiguracionSunatDTO;
import com.hafood.sistema.dto.request.SunatRequests;
import com.hafood.sistema.repository.ConfiguracionSunatRepository;
import com.hafood.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SunatConfigService {

    private static final long MAX_CERTIFICADO_BYTES = 1_048_576L;
    private static final int[] PESOS_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final List<String> PREFIJOS_RUC = List.of("10", "15", "16", "17", "20");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ConfiguracionSunatRepository configRepository;
    private final UsuarioRepository usuarioRepository;
    private final CifradoService cifradoService;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public Optional<ConfiguracionSunatDTO> obtener() {
        return configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA).map(this::aDto);
    }

    @Transactional
    public ConfiguracionSunatDTO guardar(SunatRequests.Config request, Usuario actor) {
        String ruc = request.ruc().trim();

        if (!rucValido(ruc)) {
            throw invalida("El RUC no es válido");
        }

        ConfiguracionSunat config = configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA)
                .orElseGet(ConfiguracionSunat::new);
        boolean nueva = config.getId() == null;

        if (!nueva && config.isActiva() && !ruc.equals(config.getRuc())) {
            throw conflicto("No se puede cambiar el RUC con la facturación activa. Desactívala primero");
        }

        String antes = nueva ? null : resumen(config);

        config.setRuc(ruc);
        config.setRazonSocial(request.razonSocial().trim());
        config.setNombreComercial(textoOpcional(request.nombreComercial()));
        config.setUsuarioSol(textoOpcional(request.usuarioSol()));
        config.setAmbiente(request.ambiente());

        if (request.claveSol() != null && !request.claveSol().isBlank()) {
            config.setClaveSolCifrada(cifradoService.cifrarTexto(request.claveSol().trim()));
        }

        sellar(config, actor);

        try {
            configRepository.saveAndFlush(config);
        } catch (DataIntegrityViolationException e) {
            throw conflicto("La configuración ya fue creada por otra persona. Actualiza la pantalla");
        }

        auditoriaService.registrar(actor, null, null, AccionAuditoria.SUNAT_CONFIG_ACTUALIZADA,
                antes, resumen(config), null);

        return aDto(config);
    }

    @Transactional
    public ConfiguracionSunatDTO cargarCertificado(MultipartFile archivo, String password, Usuario actor) {
        ConfiguracionSunat config = exigirConfig();

        if (archivo == null || archivo.isEmpty()) {
            throw invalida("Selecciona el archivo del certificado");
        }
        if (archivo.getSize() > MAX_CERTIFICADO_BYTES) {
            throw invalida("El certificado es demasiado grande");
        }
        if (password == null || password.isBlank()) {
            throw invalida("Indica la contraseña del certificado");
        }

        String nombre = nombreSeguro(archivo.getOriginalFilename());
        String minusculas = nombre.toLowerCase();

        if (!minusculas.endsWith(".pfx") && !minusculas.endsWith(".p12")) {
            throw invalida("El archivo debe ser un certificado .pfx o .p12");
        }

        byte[] bytes;

        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw invalida("No se pudo leer el archivo");
        }

        LocalDate vence = validarCertificado(bytes, password);
        String antes = resumen(config);

        config.setCertificadoCifrado(cifradoService.cifrar(bytes));
        config.setCertificadoPasswordCifrada(cifradoService.cifrarTexto(password));
        config.setNombreCertificado(nombre);
        config.setCertificadoVence(vence);
        sellar(config, actor);
        configRepository.save(config);

        auditoriaService.registrar(actor, null, null, AccionAuditoria.SUNAT_CERTIFICADO_CARGADO,
                antes, resumen(config), null);

        return aDto(config);
    }

    @Transactional
    public ConfiguracionSunatDTO cambiarEstado(boolean activa, Usuario actor) {
        ConfiguracionSunat config = exigirConfig();

        if (activa) {
            List<String> faltantes = new ArrayList<>();

            if (config.getUsuarioSol() == null || config.getUsuarioSol().isBlank()) {
                faltantes.add("usuario SOL");
            }
            if (config.getClaveSolCifrada() == null) {
                faltantes.add("clave SOL");
            }
            if (config.getCertificadoCifrado() == null) {
                faltantes.add("certificado digital");
            } else if (config.getCertificadoVence() != null && config.getCertificadoVence().isBefore(LocalDate.now(LIMA))) {
                faltantes.add("certificado vigente");
            }
            if (!faltantes.isEmpty()) {
                throw conflicto("Falta completar: " + String.join(", ", faltantes));
            }
        }

        String antes = resumen(config);
        config.setActiva(activa);
        sellar(config, actor);
        configRepository.save(config);

        auditoriaService.registrar(actor, null, null,
                activa ? AccionAuditoria.SUNAT_ACTIVADA : AccionAuditoria.SUNAT_DESACTIVADA,
                antes, resumen(config), null);

        return aDto(config);
    }

    private LocalDate validarCertificado(byte[] bytes, String password) {
        char[] clave = password.toCharArray();

        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(bytes), clave);
            Enumeration<String> alias = almacen.aliases();

            while (alias.hasMoreElements()) {
                String nombre = alias.nextElement();

                if (!almacen.isKeyEntry(nombre)) {
                    continue;
                }

                Key llave = almacen.getKey(nombre, clave);
                Certificate certificado = almacen.getCertificate(nombre);

                if (llave instanceof PrivateKey && certificado instanceof X509Certificate x509) {
                    x509.checkValidity();
                    return x509.getNotAfter().toInstant().atZone(LIMA).toLocalDate();
                }
            }

            throw invalida("El archivo no contiene un certificado con llave privada");
        } catch (CertificateExpiredException e) {
            throw invalida("El certificado está vencido");
        } catch (CertificateNotYetValidException e) {
            throw invalida("El certificado todavía no es válido");
        } catch (IOException | GeneralSecurityException e) {
            throw invalida("No se pudo abrir el certificado. Verifica el archivo y la contraseña");
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    private boolean rucValido(String ruc) {
        if (!ruc.matches("\\d{11}") || !PREFIJOS_RUC.contains(ruc.substring(0, 2))) {
            return false;
        }

        int suma = 0;

        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS_RUC[i];
        }

        int resto = 11 - (suma % 11);
        int digito = resto == 10 ? 0 : resto == 11 ? 1 : resto;
        return digito == ruc.charAt(10) - '0';
    }

    private ConfiguracionSunat exigirConfig() {
        return configRepository.findByClaveUnica(ConfiguracionSunat.CLAVE_UNICA)
                .orElseThrow(() -> conflicto("Primero guarda los datos de la empresa"));
    }

    private void sellar(ConfiguracionSunat config, Usuario actor) {
        config.setActualizadoPor(usuarioRepository.getReferenceById(actor.getId()));
        config.setActualizadoEn(Instant.now());
    }

    private String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String nombreSeguro(String original) {
        String nombre = original == null ? "" : original;
        int corte = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        nombre = nombre.substring(corte + 1).trim();
        return nombre.length() > 200 ? nombre.substring(nombre.length() - 200) : nombre;
    }

    private String resumen(ConfiguracionSunat c) {
        return "ruc=" + c.getRuc()
                + ";razonSocial=" + c.getRazonSocial()
                + ";usuarioSol=" + c.getUsuarioSol()
                + ";claveSol=" + (c.getClaveSolCifrada() == null ? "no" : "registrada")
                + ";certificado=" + (c.getCertificadoCifrado() == null ? "no" : c.getNombreCertificado())
                + ";vence=" + c.getCertificadoVence()
                + ";ambiente=" + c.getAmbiente()
                + ";activa=" + c.isActiva();
    }

    private ConfiguracionSunatDTO aDto(ConfiguracionSunat c) {
        return new ConfiguracionSunatDTO(
                c.getId(),
                c.getRuc(),
                c.getRazonSocial(),
                c.getNombreComercial(),
                c.getUsuarioSol(),
                c.getClaveSolCifrada() != null,
                c.getCertificadoCifrado() != null,
                c.getNombreCertificado(),
                c.getCertificadoVence(),
                c.getAmbiente(),
                c.isActiva());
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}