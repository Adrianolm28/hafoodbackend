package com.hafood.sistema.service;

import com.hafood.sistema.constant.TipoImagen;
import com.hafood.sistema.storage.ImageStorage;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELES = 40_000_000L;

    private final ImageStorage storage;

    public String procesarYGuardar(MultipartFile archivo, TipoImagen tipo) {
        if (archivo == null || archivo.isEmpty()) {
            throw invalida("Selecciona una imagen");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw invalida("La imagen supera el tamaño máximo permitido (5 MB)");
        }
        byte[] original;
        try {
            original = archivo.getBytes();
        } catch (IOException e) {
            throw invalida("No se pudo leer la imagen");
        }
        validar(original);
        return storage.guardar(tipo.getCarpeta(), convertir(original, tipo));
    }

    public Optional<byte[]> leer(String carpeta, String archivo) {
        return storage.leer(carpeta, archivo);
    }

    public String copiar(String ruta) {
        return storage.copiar(ruta);
    }

    public void eliminarTrasCommit(String ruta) {
        if (ruta == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storage.eliminar(ruta);
                }
            });
            return;
        }
        storage.eliminar(ruta);
    }

    private void validar(byte[] bytes) {
        if (!esJpeg(bytes) && !esPng(bytes) && !esGif(bytes)) {
            throw invalida("Formato no admitido. Usa una imagen JPG, PNG o GIF");
        }
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (entrada == null) {
                throw invalida("No se pudo leer la imagen");
            }
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw invalida("No se pudo leer la imagen");
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles > MAX_PIXELES) {
                    throw invalida("La imagen es demasiado grande. El máximo es de 40 megapíxeles");
                }
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw invalida("No se pudo leer la imagen");
        }
    }

    private byte[] convertir(byte[] original, TipoImagen tipo) {
        try {
            ImmutableImage imagen = ImmutableImage.loader()
                    .fromBytes(original)
                    .bound(tipo.getAnchoMax(), tipo.getAltoMax());
            return imagen.bytes(new WebpWriter().withQ(tipo.getCalidad()));
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo convertir la imagen a WebP", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar la imagen. Intenta con otro archivo");
        }
    }

    private boolean esJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private boolean esPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private boolean esGif(byte[] b) {
        return b.length > 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
    }

    private ResponseStatusException invalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
