package org.acme.mapping;

import java.util.List;
import org.acme.domain.Fruit;
import org.acme.dto.FruitDTO;
import org.acme.dto.StoreFruitPriceDTO;

public final class FruitMapper {

    private FruitMapper() {
    }

    public static FruitDTO map(Fruit fruit) {
        if (fruit == null) {
            return null;
        }
        List<StoreFruitPriceDTO> prices = fruit.getStorePrices().stream()
            .map(StoreFruitPriceMapper::map)
            .toList();
        return new FruitDTO(fruit.getId(), fruit.getName(), fruit.getDescription(), prices);
    }

    public static Fruit map(FruitDTO fruit) {
        if (fruit == null) {
            return null;
        }
        return new Fruit(fruit.id(), fruit.name(), fruit.description());
    }
}
