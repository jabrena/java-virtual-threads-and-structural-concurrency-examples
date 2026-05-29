package org.acme.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.context.ApplicationContext;
import io.micronaut.transaction.TransactionOperations;
import java.util.Map;
import org.acme.domain.Fruit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FruitRepositoryTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("fruits")
        .withUsername("fruits")
        .withPassword("fruits");

    static ApplicationContext context;
    static FruitRepository fruitRepository;
    static TransactionOperations<?> transactionOperations;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run(Map.of(
            "datasources.default.url", postgres.getJdbcUrl(),
            "datasources.default.username", postgres.getUsername(),
            "datasources.default.password", postgres.getPassword(),
            "datasources.default.driver-class-name", "org.postgresql.Driver",
            "otel.sdk.disabled", "true"
        ), "test");
        fruitRepository = context.getBean(FruitRepository.class);
        transactionOperations = context.getBean(TransactionOperations.class);
    }

    @AfterAll
    static void stopContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void findByName() {
        transactionOperations.executeWrite(status -> {
            Fruit fruit = new Fruit(null, "Grapefruit", "Summer fruit");
            fruitRepository.save(fruit);

            assertThat(fruitRepository.findByName("Grapefruit"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isNotNull().isGreaterThan(2L);
                    assertThat(found.getName()).isEqualTo("Grapefruit");
                    assertThat(found.getDescription()).isEqualTo("Summer fruit");
                });
            return null;
        });
    }
}
