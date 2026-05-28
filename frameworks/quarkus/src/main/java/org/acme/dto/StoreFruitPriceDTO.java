package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record StoreFruitPriceDTO(StoreDTO store, float price) {

    public StoreFruitPriceDTO {
        if (price < 0) {
            throw new IllegalArgumentException("Price must be >= 0");
        }
    }
}
