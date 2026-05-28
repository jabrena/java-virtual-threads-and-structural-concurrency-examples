package org.acme.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.domain.Fruit;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FruitRepositoryTests {

    @Inject
    FruitRepository fruitRepository;

    @Test
    @TestTransaction
    void findByName() {
        Fruit fruit = new Fruit(null, "Grapefruit", "Summer fruit");
        fruitRepository.save(fruit);

        assertThat(fruitRepository.findByName("Grapefruit"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isNotNull().isGreaterThan(2L);
                    assertThat(found.getName()).isEqualTo("Grapefruit");
                    assertThat(found.getDescription()).isEqualTo("Summer fruit");
                });
    }
}
