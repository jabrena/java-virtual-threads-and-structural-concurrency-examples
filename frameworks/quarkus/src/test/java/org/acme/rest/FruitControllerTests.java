package org.acme.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

    @InjectMock
    FruitRepository fruitRepository;

    @Test
    void getAll() {
        when(fruitRepository.listFruits()).thenReturn(List.of(createFruit()));

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

        verify(fruitRepository).listFruits();
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(createFruit()));

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

        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitNotFound() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.empty());

        given()
                .when().get("/fruits/Apple")
                .then()
                .statusCode(404);

        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void addFruit() {
        Fruit grapefruit = new Fruit(2L, "Grapefruit", "Summer fruit");
        when(fruitRepository.save(org.mockito.ArgumentMatchers.any(Fruit.class))).thenReturn(grapefruit);

        given()
                .contentType("application/json")
                .body("{\"name\":\"Grapefruit\",\"description\":\"Summer fruit\"}")
                .when().post("/fruits")
                .then()
                .statusCode(200)
                .body("id", equalTo(2))
                .body("name", equalTo("Grapefruit"))
                .body("description", equalTo("Summer fruit"));

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
}
