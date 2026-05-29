package org.acme.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Fruit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaFruitRepository implements FruitRepository {

    private final EntityManager entityManager;

    public JpaFruitRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fruit> findAll() {
        return entityManager.createQuery("""
                select distinct f
                from Fruit f
                left join fetch f.storePrices prices
                left join fetch prices.store
                order by f.id
                """, Fruit.class)
            .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public Fruit save(Fruit fruit) {
        entityManager.persist(fruit);
        return fruit;
    }
}
