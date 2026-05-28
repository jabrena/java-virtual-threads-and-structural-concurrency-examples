package org.acme.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
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

@Repository
public class JdbcFruitRepository implements FruitRepository {

    private final JdbcClient jdbcClient;

    public JdbcFruitRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Fruit> findAll() {
        List<FruitStorePriceRow> rows = jdbcClient.sql("""
                select f.id fruit_id,
                       f.name fruit_name,
                       f.description fruit_description,
                       s.id store_id,
                       s.name store_name,
                       s.currency store_currency,
                       s.address store_address,
                       s.city store_city,
                       s.country store_country,
                       sfp.price price
                from fruits f
                left join store_fruit_prices sfp on sfp.fruit_id = f.id
                left join stores s on s.id = sfp.store_id
                order by f.id, s.id
                """)
            .query(this::mapRow)
            .list();

        return aggregate(rows);
    }

    @Override
    public Optional<Fruit> findByName(String name) {
        List<FruitStorePriceRow> rows = jdbcClient.sql("""
                select f.id fruit_id,
                       f.name fruit_name,
                       f.description fruit_description,
                       s.id store_id,
                       s.name store_name,
                       s.currency store_currency,
                       s.address store_address,
                       s.city store_city,
                       s.country store_country,
                       sfp.price price
                from fruits f
                left join store_fruit_prices sfp on sfp.fruit_id = f.id
                left join stores s on s.id = sfp.store_id
                where f.name = :name
                order by s.id
                """)
            .param("name", name)
            .query(this::mapRow)
            .list();

        return aggregate(rows).stream().findFirst();
    }

    @Override
    public Fruit save(Fruit fruit) {
        Fruit saved = jdbcClient.sql("""
                insert into fruits (name, description)
                values (:name, :description)
                returning id, name, description
                """)
            .param("name", fruit.getName())
            .param("description", fruit.getDescription())
            .query((rs, rowNum) -> new Fruit(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description")))
            .single();
        saved.setStorePrices(new ArrayList<>());
        return saved;
    }

    private FruitStorePriceRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long storeId = rs.getObject("store_id", Long.class);
        Store store = null;
        BigDecimal price = null;
        if (storeId != null) {
            store = new Store(
                storeId,
                rs.getString("store_name"),
                new Address(rs.getString("store_address"), rs.getString("store_city"), rs.getString("store_country")),
                rs.getString("store_currency"));
            price = rs.getBigDecimal("price");
        }
        return new FruitStorePriceRow(
            rs.getLong("fruit_id"),
            rs.getString("fruit_name"),
            rs.getString("fruit_description"),
            store,
            price);
    }

    private static List<Fruit> aggregate(List<FruitStorePriceRow> rows) {
        Map<Long, Fruit> fruits = new LinkedHashMap<>();
        for (FruitStorePriceRow row : rows) {
            Fruit fruit = fruits.computeIfAbsent(row.fruitId(), id -> {
                Fruit value = new Fruit(id, row.fruitName(), row.fruitDescription());
                value.setStorePrices(new ArrayList<>());
                return value;
            });
            if (row.store() != null) {
                fruit.getStorePrices().add(new StoreFruitPrice(row.store(), fruit, row.price()));
            }
        }
        return new ArrayList<>(fruits.values());
    }

    private record FruitStorePriceRow(
        Long fruitId,
        String fruitName,
        String fruitDescription,
        Store store,
        BigDecimal price
    ) {
    }
}
