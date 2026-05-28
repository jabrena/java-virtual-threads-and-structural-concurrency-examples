package org.acme.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record StoreFruitPriceId(
    @Column(nullable = false) Long storeId,
    @Column(nullable = false) Long fruitId
) implements Serializable {

    public StoreFruitPriceId(Store store, Fruit fruit) {
        this(store == null ? null : store.getId(), fruit == null ? null : fruit.getId());
    }
}
