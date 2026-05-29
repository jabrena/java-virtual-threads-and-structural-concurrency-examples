package org.acme.service;

import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.dto.FruitDTO;
import org.acme.dto.AddressDTO;
import org.acme.dto.StoreDTO;
import org.acme.dto.StoreFruitPriceDTO;
import org.acme.repository.FruitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultFruitService implements FruitService {

    private final FruitRepository fruitRepository;

    public DefaultFruitService(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Observed(name = "FruitService.getAllFruits")
    public List<FruitDTO> getAllFruits() {
        return fruitRepository.findAll().stream()
                .map(DefaultFruitService::map)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Observed(name = "FruitService.getFruitByName")
    public Optional<FruitDTO> getFruitByName(String name) {
        return fruitRepository.findByName(name).map(DefaultFruitService::map);
    }

    @Override
    @Transactional
    @Observed(name = "FruitService.createFruit")
    public FruitDTO createFruit(FruitDTO fruit) {
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
