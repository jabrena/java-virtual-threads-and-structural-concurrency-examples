package org.acme.dto;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Introspected
public record FruitDTO(
    Long id,
    @NotBlank(message = "Name is mandatory") String name,
    String description,
    List<StoreFruitPriceDTO> storePrices
) {

    public FruitDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is mandatory");
        }
        storePrices = storePrices == null ? new ArrayList<>() : storePrices;
    }
}
