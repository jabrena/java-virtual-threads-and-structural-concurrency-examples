package org.acme.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Fruit;

@ApplicationScoped
public class FruitRepository implements PanacheRepository<Fruit> {

    @Transactional(TxType.SUPPORTS)
    public Optional<Fruit> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    @Transactional(TxType.SUPPORTS)
    public List<Fruit> listFruits() {
        return listAll();
    }

    public Fruit save(Fruit fruit) {
        persist(fruit);
        return fruit;
    }
}
