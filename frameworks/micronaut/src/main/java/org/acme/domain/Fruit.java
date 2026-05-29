package org.acme.domain;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Fruit {

    private Long id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    private String description;

    private List<StoreFruitPrice> storePrices = new ArrayList<>();

    public Fruit() {
    }

    public Fruit(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<StoreFruitPrice> getStorePrices() {
        return storePrices;
    }

    public void setStorePrices(List<StoreFruitPrice> storePrices) {
        this.storePrices = storePrices;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Fruit.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .toString();
    }
}
