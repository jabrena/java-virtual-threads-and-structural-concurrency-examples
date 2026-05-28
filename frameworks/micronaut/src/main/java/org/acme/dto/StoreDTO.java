package org.acme.dto;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record StoreDTO(Long id, String name, String currency, AddressDTO address) {

    public StoreDTO {
        requireText(name, "Name is mandatory");
        requireText(currency, "Currency is mandatory");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
