package org.acme.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.transaction.annotation.ReadOnly;
import io.micronaut.transaction.annotation.Transactional;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.acme.dto.FruitDTO;
import org.acme.mapping.FruitMapper;
import org.acme.observability.FruitMetrics;
import org.acme.repository.FruitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FruitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FruitService.class);

    private final FruitRepository fruitRepository;
    private final FruitMetrics fruitMetrics;
    private final MeterRegistry meterRegistry;

    public FruitService(FruitRepository fruitRepository, FruitMetrics fruitMetrics, MeterRegistry meterRegistry) {
        this.fruitRepository = fruitRepository;
        this.fruitMetrics = fruitMetrics;
        this.meterRegistry = meterRegistry;
    }

    @ReadOnly
    @WithSpan("FruitService.getAllFruits")
    public List<FruitDTO> getAllFruits() {
        Span.current().setAttribute("fruit.operation", "list");
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            List<FruitDTO> fruits = fruitRepository.listFruits().stream()
                .map(FruitMapper::map)
                .toList();
            fruitMetrics.recordListSuccess(sample);
            LOGGER.debug("Listed {} fruits", fruits.size());
            return fruits;
        } catch (RuntimeException ex) {
            fruitMetrics.recordListError(sample);
            throw ex;
        }
    }

    @ReadOnly
    @WithSpan("FruitService.getFruitByName")
    public Optional<FruitDTO> getFruitByName(String name) {
        Span.current()
            .setAttribute("fruit.operation", "lookup")
            .setAttribute("fruit.lookup.name.present", name != null);
        Timer.Sample sample = fruitMetrics.start(meterRegistry);
        try {
            Optional<FruitDTO> fruit = fruitRepository.findByName(name).map(FruitMapper::map);
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
            FruitDTO created = FruitMapper.map(fruitRepository.save(FruitMapper.map(fruit)));
            fruitMetrics.recordCreateSuccess(sample);
            LOGGER.info("Created fruit id={} name={}", created.id(), created.name());
            return created;
        } catch (RuntimeException ex) {
            fruitMetrics.recordCreateError(sample);
            throw ex;
        }
    }
}
