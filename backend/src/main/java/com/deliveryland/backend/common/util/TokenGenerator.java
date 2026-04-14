package com.deliveryland.backend.common.util;

import java.util.UUID;

public final class TokenGenerator {

    private TokenGenerator() {}

    public static String generateShortToken() {
        return UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8);
    }
}