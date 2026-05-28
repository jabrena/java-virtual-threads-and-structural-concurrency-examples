package org.acme.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.acme.domain.Fruit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class JdbcFruitRepositoryTest {

    private static final PostgreSQLContainer POSTGRESQL =
        new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    private JdbcFruitRepository repository;

    @BeforeAll
    static void startPostgresql() {
        POSTGRESQL.start();
    }

    @AfterAll
    static void stopPostgresql() {
        POSTGRESQL.stop();
    }

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRESQL.getJdbcUrl());
        dataSource.setUsername(POSTGRESQL.getUsername());
        dataSource.setPassword(POSTGRESQL.getPassword());

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("schema.sql"),
            new ClassPathResource("data.sql"));
        populator.execute(dataSource);

        repository = new JdbcFruitRepository(JdbcClient.create(dataSource));
    }

    @Test
    void findsAllSeededFruitsWithStorePrices() {
        var fruits = repository.findAll();

        assertEquals(10, fruits.size());
        Fruit apple = fruits.getFirst();
        assertEquals("Apple", apple.getName());
        assertEquals(4, apple.getStorePrices().size());
        assertEquals("Store 1", apple.getStorePrices().getFirst().getStore().getName());
    }

    @Test
    void findsFruitByName() {
        var fruit = repository.findByName("Kiwi");

        assertTrue(fruit.isPresent());
        assertEquals("Tart green fruit", fruit.get().getDescription());
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
