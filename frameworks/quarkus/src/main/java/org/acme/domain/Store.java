package org.acme.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.StringJoiner;

public class Store {

    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Currency is mandatory")
    private String currency;

    private Address address;

    public Store() {
    }

    public Store(Long id, String name, Address address, String currency) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Store.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("currency='" + currency + "'")
                .add("address=" + address)
                .toString();
    }
}
