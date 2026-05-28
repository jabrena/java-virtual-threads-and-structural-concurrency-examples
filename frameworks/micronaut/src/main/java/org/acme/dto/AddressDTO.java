package org.acme.dto;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;

@Introspected
public record AddressDTO(
    @NotBlank(message = "Address is mandatory") String address,
    @NotBlank(message = "City is mandatory") String city,
    @NotBlank(message = "Country is mandatory") String country
) {

    public AddressDTO {
        requireText(address, "Address is mandatory");
        requireText(city, "City is mandatory");
        requireText(country, "Country is mandatory");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
