package org.acme.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.http.HttpStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.acme.dto.FruitDTO;
import org.acme.observability.FruitMetrics;
import org.acme.repository.FruitRepository;
import org.acme.service.FruitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FruitControllerTests {

    FruitRepository fruitRepository;
    FruitController fruitController;
    SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        fruitRepository = mock(FruitRepository.class);
        registry = new SimpleMeterRegistry();
        fruitController = new FruitController(new FruitService(fruitRepository, new FruitMetrics(registry), registry));
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getAll() {
        when(fruitRepository.listFruits()).thenReturn(List.of(createFruit()));

        List<FruitDTO> response = fruitController.getAll();

        assertThat(response).hasSize(1);
        assertApple(response.getFirst());
        assertThat(requests("list", "success")).isEqualTo(1.0d);
        assertThat(timerCount("list")).isEqualTo(1L);
        verify(fruitRepository).listFruits();
    }

    @Test
    void getFruitFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(createFruit()));

        var response = fruitController.getFruit("Apple");

        assertThat((Object) response.status()).isEqualTo(HttpStatus.OK);
        assertApple(response.body());
        assertThat(requests("lookup", "success")).isEqualTo(1.0d);
        verify(fruitRepository).findByName("Apple");
    }

    @Test
    void getFruitNotFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.empty());

        var response = fruitController.getFruit("Apple");

        assertThat((Object) response.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((Object) response.body()).isNull();
        assertThat(requests("lookup", "not_found")).isEqualTo(1.0d);
        verify(fruitRepository).findByName("Apple");
    }

    @Test
    void addFruit() {
        Fruit grapefruit = new Fruit(11L, "Grapefruit", "Summer fruit");
        when(fruitRepository.save(any(Fruit.class))).thenReturn(grapefruit);

        FruitDTO response = fruitController.addFruit(new FruitDTO(null, "Grapefruit", "Summer fruit", null));

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.name()).isEqualTo("Grapefruit");
        assertThat(response.description()).isEqualTo("Summer fruit");
        assertThat(requests("create", "success")).isEqualTo(1.0d);
        assertThat(timerCount("create")).isEqualTo(1L);
        verify(fruitRepository).save(argThat(fruit ->
            fruit.getId() == null
                && "Grapefruit".equals(fruit.getName())
                && "Summer fruit".equals(fruit.getDescription())));
    }

    private static Fruit createFruit() {
        Fruit fruit = new Fruit(1L, "Apple", "Hearty Fruit");
        Store store = new Store(1L, "Some Store", new Address("123 Some St", "Some City", "USA"), "USD");
        fruit.setStorePrices(List.of(new StoreFruitPrice(store, fruit, BigDecimal.valueOf(1.29))));
        return fruit;
    }

    private static void assertApple(FruitDTO apple) {
        assertThat(apple.id()).isEqualTo(1L);
        assertThat(apple.name()).isEqualTo("Apple");
        assertThat(apple.description()).isEqualTo("Hearty Fruit");
        assertThat(apple.storePrices()).hasSize(1);
        var price = apple.storePrices().getFirst();
        assertThat(price.price()).isEqualTo(1.29F);
        assertThat(price.store().id()).isEqualTo(1L);
        assertThat(price.store().name()).isEqualTo("Some Store");
        assertThat(price.store().currency()).isEqualTo("USD");
        assertThat(price.store().address().address()).isEqualTo("123 Some St");
        assertThat(price.store().address().city()).isEqualTo("Some City");
        assertThat(price.store().address().country()).isEqualTo("USA");
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
