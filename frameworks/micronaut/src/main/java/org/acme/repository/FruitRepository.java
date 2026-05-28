package org.acme.repository;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Fruit;

@Singleton
public class FruitRepository {

    private final EntityManager entityManager;

    public FruitRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Fruit> findByName(String name) {
        return entityManager.createQuery("""
                select distinct f
                from Fruit f
                left join fetch f.storePrices prices
                left join fetch prices.store
                where f.name = :name
                """, Fruit.class)
            .setParameter("name", name)
            .getResultStream()
            .findFirst();
    }

    public List<Fruit> listFruits() {
        return entityManager.createQuery("""
                select distinct f
                from Fruit f
                left join fetch f.storePrices prices
                left join fetch prices.store
                order by f.id
                """, Fruit.class)
            .getResultList();
    }

    @Transactional
    public Fruit save(Fruit fruit) {
        entityManager.persist(fruit);
        return fruit;
    }
}
