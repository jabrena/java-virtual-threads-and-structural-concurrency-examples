package org.acme.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
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
import org.acme.observability.FruitMetrics;
import org.acme.repository.FruitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FruitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FruitService.class);

    private final FruitRepository fruitRepository;
    private final FruitMetrics fruitMetrics;
    private final MeterRegistry meterRegistry;

    @Inject
    public FruitService(FruitRepository fruitRepository, FruitMetrics fruitMetrics, MeterRegistry meterRegistry) {
        this.fruitRepository = fruitRepository;
        this.fruitMetrics = fruitMetrics;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(TxType.SUPPORTS)
    @WithSpan("FruitService.getAllFruits")
    public List<FruitDTO> getAllFruits() {
        Span.current().setAttribute("fruit.operation", "list");
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            List<FruitDTO> fruits = fruitRepository.listFruits().stream()
                    .map(FruitService::map)
                    .toList();
            fruitMetrics.recordListSuccess(sample);
            LOGGER.debug("Listed {} fruits", fruits.size());
            return fruits;
        } catch (RuntimeException ex) {
            fruitMetrics.recordListError(sample);
            throw ex;
        }
    }

    @Transactional(TxType.SUPPORTS)
    @WithSpan("FruitService.getFruitByName")
    public Optional<FruitDTO> getFruitByName(String name) {
        Span.current()
                .setAttribute("fruit.operation", "lookup")
                .setAttribute("fruit.lookup.name.present", name != null);
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            Optional<FruitDTO> fruit = fruitRepository.findByName(name).map(FruitService::map);
            fruitMetrics.recordLookup(sample, fruit.isPresent());
            LOGGER.debug("Fruit lookup completed for name={} found={}", name, fruit.isPresent());
            return fruit;
        } catch (RuntimeException ex) {
            fruitMetrics.recordLookupError(sample);
            throw ex;
        }
    }

    @Transactional
    @WithSpan("FruitService.createFruit")
    public FruitDTO createFruit(FruitDTO fruit) {
        Span.current()
                .setAttribute("fruit.operation", "create")
                .setAttribute("fruit.payload.name.present", fruit.name() != null);
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
