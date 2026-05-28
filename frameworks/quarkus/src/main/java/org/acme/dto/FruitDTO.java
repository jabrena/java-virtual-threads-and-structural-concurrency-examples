package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public record FruitDTO(
        Long id,
        @NotBlank(message = "Name is mandatory") String name,
        String description,
        List<StoreFruitPriceDTO> storePrices) {

    public FruitDTO {
        if (name == null) {
            throw new IllegalArgumentException("Name is mandatory");
        }
        if (storePrices == null) {
            storePrices = new ArrayList<>();
        }
    }
}
