package org.acme.repository;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;

@Singleton
public class FruitRepository {

    private static final String SELECT_FRUITS = """
            select f.id fruit_id, f.name fruit_name, f.description fruit_description,
                   s.id store_id, s.name store_name, s.currency, s.address, s.city, s.country,
                   p.price
            from fruits f
            left join store_fruit_prices p on p.fruit_id = f.id
            left join stores s on s.id = p.store_id
            """;

    private final DataSource dataSource;

    public FruitRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Fruit> findByName(String name) {
        Map<Long, Fruit> fruits = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_FRUITS + " where f.name = ? order by s.id")) {
            statement.setString(1, name);
            statement.setFetchSize(16);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Fruit fruit = fruits.computeIfAbsent(rs.getLong("fruit_id"), id -> mapFruit(rs));
                    addStorePrice(rs, fruit);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find fruit by name", ex);
        }
        return fruits.values().stream().findFirst();
    }

    public List<Fruit> listFruits() {
        Map<Long, Fruit> fruits = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setFetchSize(16);
            try (ResultSet rs = statement.executeQuery(SELECT_FRUITS + " order by f.id, s.id")) {
                while (rs.next()) {
                    Fruit fruit = fruits.computeIfAbsent(rs.getLong("fruit_id"), id -> mapFruit(rs));
                    addStorePrice(rs, fruit);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to list fruits", ex);
        }
        return new ArrayList<>(fruits.values());
    }

    @Transactional
    public Fruit save(Fruit fruit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into fruits(name, description)
                     values (?, ?)
                     returning id
                     """)) {
            statement.setString(1, fruit.getName());
            statement.setString(2, fruit.getDescription());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    fruit.setId(rs.getLong("id"));
                }
            }
            return fruit;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to save fruit", ex);
        }
    }

    private static Fruit mapFruit(ResultSet rs) {
        try {
            return new Fruit(rs.getLong("fruit_id"), rs.getString("fruit_name"), rs.getString("fruit_description"));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to map fruit row", ex);
        }
    }

    private static void addStorePrice(ResultSet rs, Fruit fruit) {
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
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to map store price row", ex);
        }
    }
}
