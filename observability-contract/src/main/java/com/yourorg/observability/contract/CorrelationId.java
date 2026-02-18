package com.yourorg.observability.contract;

import java.util.Optional;
import java.util.UUID;

public final class CorrelationId {
    private CorrelationId() {}

    public static String fromHeaderOrNew(String headerValue) {
        return Optional.ofNullable(headerValue)
                .filter(v -> !v.isBlank())
                .orElse(UUID.randomUUID().toString());
    }
}
