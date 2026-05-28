package org.acme.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

@RegisterForReflection
public record AddressDTO(
        @NotBlank(message = "Address is mandatory") String address,
        @NotBlank(message = "City is mandatory") String city,
        @NotBlank(message = "Country is mandatory") String country) {

    public AddressDTO {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address is mandatory");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is mandatory");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is mandatory");
        }
    }
}
