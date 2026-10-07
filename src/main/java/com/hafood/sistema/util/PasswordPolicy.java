package com.hafood.sistema.util;

import com.hafood.sistema.exception.ValidationException;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern PATRON =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$");

    private PasswordPolicy() {}

    public static void validar(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Debe proporcionar una contraseña");
        }
        if (!PATRON.matcher(password).matches()) {
            throw new ValidationException(
                    "La contraseña debe tener mínimo 8 caracteres, con al menos una mayúscula, " +
                            "una minúscula, un número y un carácter especial"
            );
        }
    }
}