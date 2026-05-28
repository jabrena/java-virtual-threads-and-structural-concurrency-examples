package org.acme.mapping;

import org.acme.domain.Address;
import org.acme.dto.AddressDTO;

public final class AddressMapper {

    private AddressMapper() {
    }

    public static AddressDTO map(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDTO(address.address(), address.city(), address.country());
    }

    public static Address map(AddressDTO address) {
        if (address == null) {
            return null;
        }
        return new Address(address.address(), address.city(), address.country());
    }
}
