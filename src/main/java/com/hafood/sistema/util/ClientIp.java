package com.hafood.sistema.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {
    }

    public static String de(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded == null || forwarded.isEmpty()) {
            return request.getRemoteAddr();
        }

        return forwarded.split(",")[0].trim();
    }
}