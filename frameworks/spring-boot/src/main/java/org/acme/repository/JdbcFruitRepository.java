package org.acme.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcFruitRepository implements FruitRepository {

    private static final String SELECT_FRUITS = """
            select f.id fruit_id, f.name fruit_name, f.description fruit_description,
                   s.id store_id, s.name store_name, s.currency, s.address, s.city, s.country,
                   p.price
            from fruits f
            left join store_fruit_prices p on p.fruit_id = f.id
            left join stores s on s.id = p.store_id
            """;

    private final JdbcClient jdbcClient;

    public JdbcFruitRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fruit> findAll() {
        Map<Long, Fruit> fruits = new LinkedHashMap<>();
        jdbcClient.sql(SELECT_FRUITS + " order by f.id, s.id")
                .query((rs, rowNum) -> {
                    Fruit fruit = fruits.computeIfAbsent(rs.getLong("fruit_id"), id -> mapFruit(rs));
                    addStorePrice(rs, fruit);
                    return fruit;
                })
                .list();
        return new ArrayList<>(fruits.values());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fruit> findByName(String name) {
        Map<Long, Fruit> fruits = new LinkedHashMap<>();
        jdbcClient.sql(SELECT_FRUITS + " where f.name = :name order by s.id")
                .param("name", name)
                .query((rs, rowNum) -> {
                    Fruit fruit = fruits.computeIfAbsent(rs.getLong("fruit_id"), id -> mapFruit(rs));
                    addStorePrice(rs, fruit);
                    return fruit;
                })
                .list();
        return fruits.values().stream().findFirst();
    }

    @Override
    @Transactional
    public Fruit save(Fruit fruit) {
        Long id = jdbcClient.sql("""
                insert into fruits(name, description)
                values (:name, :description)
                returning id
                """)
                .param("name", fruit.getName())
                .param("description", fruit.getDescription())
                .query(Long.class)
                .single();
        fruit.setId(id);
        return fruit;
    }

    private static Fruit mapFruit(java.sql.ResultSet rs) {
        try {
            return new Fruit(rs.getLong("fruit_id"), rs.getString("fruit_name"), rs.getString("fruit_description"));
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Unable to map fruit row", ex);
        }
    }

    private static void addStorePrice(java.sql.ResultSet rs, Fruit fruit) {
        try {
            long storeId = rs.getLong("store_id");
            if (rs.wasNull()) {
                return;
            }
            Store store = new Store(
                    storeId,
                    rs.getString("store_name"),
                    new Address(rs.getString("address"), rs.getString("city"), rs.getString("country")),
                    rs.getString("currency"));
            BigDecimal price = rs.getBigDecimal("price");
            fruit.getStorePrices().add(new StoreFruitPrice(store, fruit, price));
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Unable to map store price row", ex);
        }
    }
}
