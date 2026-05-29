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
import org.acme.observability.FruitMetrics;
import org.acme.repository.FruitRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultFruitService implements FruitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFruitService.class);

    private final FruitRepository fruitRepository;
    private final FruitMetrics fruitMetrics;
    private final MeterRegistry meterRegistry;

    public DefaultFruitService(FruitRepository fruitRepository, FruitMetrics fruitMetrics, MeterRegistry meterRegistry) {
        this.fruitRepository = fruitRepository;
        this.fruitMetrics = fruitMetrics;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Observed(name = "FruitService.getAllFruits", lowCardinalityKeyValues = {"fruit.operation", "list"})
    public List<FruitDTO> getAllFruits() {
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            List<FruitDTO> fruits = fruitRepository.findAll().stream()
                    .map(DefaultFruitService::map)
                    .toList();
            fruitMetrics.recordListSuccess(sample);
            LOGGER.debug("Listed {} fruits", fruits.size());
            return fruits;
        } catch (RuntimeException ex) {
            fruitMetrics.recordListError(sample);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    @Observed(name = "FruitService.getFruitByName", lowCardinalityKeyValues = {"fruit.operation", "lookup"})
    public Optional<FruitDTO> getFruitByName(String name) {
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            Optional<FruitDTO> fruit = fruitRepository.findByName(name).map(DefaultFruitService::map);
            fruitMetrics.recordLookup(sample, fruit.isPresent());
            LOGGER.debug("Fruit lookup completed for name={} found={}", name, fruit.isPresent());
            return fruit;
        } catch (RuntimeException ex) {
            fruitMetrics.recordLookupError(sample);
            throw ex;
        }
    }

    @Override
    @Transactional
    @Observed(name = "FruitService.createFruit", lowCardinalityKeyValues = {"fruit.operation", "create"})
    public FruitDTO createFruit(FruitDTO fruit) {
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            FruitDTO created = map(fruitRepository.save(map(fruit)));
            fruitMetrics.recordCreateSuccess(sample);
            LOGGER.info("Created fruit id={} name={}", created.id(), created.name());
            return created;
        } catch (RuntimeException ex) {
            fruitMetrics.recordCreateError(sample);
            throw ex;
        }
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
