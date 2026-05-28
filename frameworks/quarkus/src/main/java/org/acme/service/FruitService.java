package org.acme.service;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.dto.AddressDTO;
import org.acme.dto.FruitDTO;
import org.acme.dto.StoreDTO;
import org.acme.dto.StoreFruitPriceDTO;
import org.acme.repository.FruitRepository;

@ApplicationScoped
public class FruitService {

    private final FruitRepository fruitRepository;

    @Inject
    public FruitService(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    @Transactional(TxType.SUPPORTS)
    @WithSpan("FruitService.getAllFruits")
    public List<FruitDTO> getAllFruits() {
        return fruitRepository.listFruits().stream()
                .map(FruitService::map)
                .toList();
    }

    @Transactional(TxType.SUPPORTS)
    @WithSpan("FruitService.getFruitByName")
    public Optional<FruitDTO> getFruitByName(@SpanAttribute("arg.name") String name) {
        return fruitRepository.findByName(name).map(FruitService::map);
    }

    @Transactional
    @WithSpan("FruitService.createFruit")
    public FruitDTO createFruit(@SpanAttribute("arg.fruit") FruitDTO fruit) {
        return map(fruitRepository.save(map(fruit)));
    }

    private static FruitDTO map(Fruit fruit) {
        List<StoreFruitPriceDTO> prices = fruit.getStorePrices().stream()
                .map(storeFruitPrice -> new StoreFruitPriceDTO(
                        map(storeFruitPrice.getStore()),
                        storeFruitPrice.getPrice().floatValue()))
                .toList();
        return new FruitDTO(fruit.getId(), fruit.getName(), fruit.getDescription(), prices);
    }

    private static Fruit map(FruitDTO fruit) {
        return new Fruit(fruit.id(), fruit.name(), fruit.description());
    }

    private static StoreDTO map(Store store) {
        return new StoreDTO(store.getId(), store.getName(), store.getCurrency(), map(store.getAddress()));
    }

    private static AddressDTO map(Address address) {
        return new AddressDTO(address.address(), address.city(), address.country());
    }
}
