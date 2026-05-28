package org.acme.repository;

import java.util.List;
import java.util.Optional;
import org.acme.domain.Fruit;

public interface FruitRepository {

    List<Fruit> findAll();

    Optional<Fruit> findByName(String name);

    Fruit save(Fruit fruit);
}
