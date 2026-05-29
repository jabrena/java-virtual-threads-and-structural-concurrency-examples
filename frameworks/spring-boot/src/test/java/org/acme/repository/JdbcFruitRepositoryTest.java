package org.acme.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.acme.domain.Fruit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Transactional
class JdbcFruitRepositoryTest {

    private static final PostgreSQLContainer POSTGRESQL =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    static {
        POSTGRESQL.start();
    }

    @Autowired
    private JdbcFruitRepository repository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
    }

    @AfterAll
    static void stopPostgresql() {
        POSTGRESQL.stop();
    }

    @Test
    void findsAllSeededFruitsWithStorePrices() {
        var fruits = repository.findAll();

        assertEquals(10, fruits.size());
        Fruit apple = fruits.getFirst();
        assertEquals("Apple", apple.getName());
        assertEquals(7, apple.getStorePrices().size());
    }

    @Test
    void findsFruitByName() {
        var fruit = repository.findByName("Kiwi");

        assertTrue(fruit.isPresent());
        assertEquals("Small fuzzy green fruit", fruit.get().getDescription());
        assertFalse(fruit.get().getStorePrices().isEmpty());
    }

    @Test
    void savesFruitInPostgresql() {
        Fruit saved = repository.save(new Fruit(null, "Grapefruit", "Summer fruit"));

        assertNotNull(saved.getId());
        assertEquals("Grapefruit", saved.getName());
        assertEquals("Summer fruit", saved.getDescription());
        assertTrue(saved.getStorePrices().isEmpty());
        assertTrue(repository.findByName("Grapefruit").isPresent());
    }
}
