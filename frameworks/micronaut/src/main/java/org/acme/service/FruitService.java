package org.acme.service;

import io.micronaut.transaction.annotation.ReadOnly;
import io.micronaut.transaction.annotation.Transactional;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.acme.dto.FruitDTO;
import org.acme.mapping.FruitMapper;
import org.acme.repository.FruitRepository;

@Singleton
public class FruitService {

    private final FruitRepository fruitRepository;

    public FruitService(FruitRepository fruitRepository) {
        this.fruitRepository = fruitRepository;
    }

    @ReadOnly
    @WithSpan("FruitService.getAllFruits")
    public List<FruitDTO> getAllFruits() {
        return fruitRepository.listFruits().stream()
            .map(FruitMapper::map)
            .toList();
    }

    @ReadOnly
    @WithSpan("FruitService.getFruitByName")
    public Optional<FruitDTO> getFruitByName(@SpanAttribute("arg.name") String name) {
        return fruitRepository.findByName(name).map(FruitMapper::map);
    }

    @Transactional
    @WithSpan("FruitService.createFruit")
    public FruitDTO createFruit(@SpanAttribute("arg.fruit") FruitDTO fruit) {
        return FruitMapper.map(fruitRepository.save(FruitMapper.map(fruit)));
    }
}
