package org.acme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.acme.domain.Fruit;
import org.acme.dto.FruitDTO;
import org.acme.observability.FruitMetrics;
import org.acme.repository.FruitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultFruitServiceTest {

    @Mock
    private FruitRepository fruitRepository;

    private SimpleMeterRegistry registry;
    private DefaultFruitService fruitService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        fruitService = new DefaultFruitService(fruitRepository, new FruitMetrics(registry), registry);
    }

    @Test
    void findByNameMapsFruitWhenPresent() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(new Fruit(1L, "Apple", "Hearty fruit")));

        Optional<FruitDTO> result = fruitService.getFruitByName("Apple");

        assertThat(result).contains(new FruitDTO(1L, "Apple", "Hearty fruit", null));
        assertThat(requests("lookup", "success")).isEqualTo(1.0d);
        assertThat(timerCount("lookup")).isEqualTo(1L);
        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void addStoresFruitThroughRepository() {
        when(fruitRepository.save(any(Fruit.class))).thenReturn(new Fruit(3L, "Pear", "Green fruit"));

        FruitDTO result = fruitService.createFruit(new FruitDTO(null, "Pear", "Green fruit", null));

        assertThat(result).isEqualTo(new FruitDTO(3L, "Pear", "Green fruit", null));
        assertThat(requests("create", "success")).isEqualTo(1.0d);
        assertThat(timerCount("create")).isEqualTo(1L);
        verify(fruitRepository).save(any(Fruit.class));
        verifyNoMoreInteractions(fruitRepository);
    }

    private double requests(String operation, String outcome) {
        return registry.get("fruit.store.requests")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .counter()
                .count();
    }

    private long timerCount(String operation) {
        return registry.get("fruit.store.request.duration")
                .tag("operation", operation)
                .timer()
                .count();
    }
}
