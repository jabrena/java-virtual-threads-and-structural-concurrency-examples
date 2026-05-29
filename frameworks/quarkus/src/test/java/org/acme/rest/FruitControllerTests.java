package org.acme.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.acme.repository.FruitRepository;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FruitControllerTests {

    @Inject
    MeterRegistry registry;

    @InjectMock
    FruitRepository fruitRepository;

    @Test
    void getAll() {
        when(fruitRepository.listFruits()).thenReturn(List.of(createFruit()));
        double before = requests("list", "success");

        given()
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(1))
                .body("[0].name", equalTo("Apple"))
                .body("[0].description", equalTo("Hearty Fruit"))
                .body("[0].storePrices", hasSize(1))
                .body("[0].storePrices[0].price", equalTo(1.29F))
                .body("[0].storePrices[0].store.id", equalTo(1))
                .body("[0].storePrices[0].store.name", equalTo("Some Store"))
                .body("[0].storePrices[0].store.currency", equalTo("USD"))
                .body("[0].storePrices[0].store.address.address", equalTo("123 Some St"))
                .body("[0].storePrices[0].store.address.city", equalTo("Some City"))
                .body("[0].storePrices[0].store.address.country", equalTo("USA"));

        assertThat(requests("list", "success")).isEqualTo(before + 1.0d);
        assertThat(timerCount("list")).isGreaterThan(0L);
        verify(fruitRepository).listFruits();
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(createFruit()));
        double before = requests("lookup", "success");

        given()
                .when().get("/fruits/Apple")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("name", equalTo("Apple"))
                .body("description", equalTo("Hearty Fruit"))
                .body("storePrices", hasSize(1))
                .body("storePrices[0].price", equalTo(1.29F))
                .body("storePrices[0].store.id", equalTo(1))
                .body("storePrices[0].store.name", equalTo("Some Store"))
                .body("storePrices[0].store.currency", equalTo("USD"))
                .body("storePrices[0].store.address.address", equalTo("123 Some St"))
                .body("storePrices[0].store.address.city", equalTo("Some City"))
                .body("storePrices[0].store.address.country", equalTo("USA"));

        assertThat(requests("lookup", "success")).isEqualTo(before + 1.0d);
        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitNotFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.empty());
        double before = requests("lookup", "not_found");

        given()
                .when().get("/fruits/Apple")
                .then()
                .statusCode(404);

        assertThat(requests("lookup", "not_found")).isEqualTo(before + 1.0d);
        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void addFruit() {
        Fruit grapefruit = new Fruit(2L, "Grapefruit", "Summer fruit");
        when(fruitRepository.save(org.mockito.ArgumentMatchers.any(Fruit.class))).thenReturn(grapefruit);
        double before = requests("create", "success");

        given()
                .contentType("application/json")
                .body("{\"name\":\"Grapefruit\",\"description\":\"Summer fruit\"}")
                .when().post("/fruits")
                .then()
                .statusCode(200)
                .body("id", equalTo(2))
                .body("name", equalTo("Grapefruit"))
                .body("description", equalTo("Summer fruit"));

        assertThat(requests("create", "success")).isEqualTo(before + 1.0d);
        assertThat(timerCount("create")).isGreaterThan(0L);
        verify(fruitRepository).save(org.mockito.ArgumentMatchers.argThat(fruit ->
                fruit.getId() == null
                        && "Grapefruit".equals(fruit.getName())
                        && "Summer fruit".equals(fruit.getDescription())));
        verifyNoMoreInteractions(fruitRepository);
    }

    private static Fruit createFruit() {
        Fruit fruit = new Fruit(1L, "Apple", "Hearty Fruit");
        Store store = new Store(1L, "Some Store", new Address("123 Some St", "Some City", "USA"), "USD");
        fruit.setStorePrices(List.of(new StoreFruitPrice(store, fruit, BigDecimal.valueOf(1.29))));
        return fruit;
    }

    private double requests(String operation, String outcome) {
        var counter = registry.find("fruit.store.requests")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .counter();
        return counter == null ? 0.0d : counter.count();
    }

    private long timerCount(String operation) {
        var timer = registry.find("fruit.store.request.duration")
                .tag("operation", operation)
                .timer();
        return timer == null ? 0L : timer.count();
    }
}
