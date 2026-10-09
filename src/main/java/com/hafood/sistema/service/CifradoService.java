package com.hafood.sistema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CifradoService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKeySpec clave;
    private final SecureRandom random = new SecureRandom();

    public CifradoService(@Value("${hafood.seguridad.clave-secretos}") String claveBase64) {
        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(claveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La clave de cifrado no es Base64 válido.");
        }

        if (bytes.length != KEY_BYTES) {
            throw new IllegalStateException("La clave de cifrado debe tener 32 bytes.");
        }

        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public byte[] cifrar(byte[] plano) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(plano);
            return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger el dato.");
        }
    }

    public byte[] descifrar(byte[] cifrado) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(cifrado);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] datos = new byte[buffer.remaining()];
            buffer.get(datos);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(datos);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new IllegalStateException("No se pudo leer el dato protegido.");
        }
    }

    public String cifrarTexto(String texto) {
        return Base64.getEncoder().encodeToString(cifrar(texto.getBytes(StandardCharsets.UTF_8)));
    }

    public String descifrarTexto(String cifrado) {
        return new String(descifrar(Base64.getDecoder().decode(cifrado)), StandardCharsets.UTF_8);
    }
}