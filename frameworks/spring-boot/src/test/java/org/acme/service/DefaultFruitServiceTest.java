package org.acme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.acme.domain.Fruit;
import org.acme.dto.FruitDTO;
import org.acme.repository.FruitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultFruitServiceTest {

    @Mock
    private FruitRepository fruitRepository;

    @InjectMocks
    private DefaultFruitService fruitService;

    @Test
    void findByNameMapsFruitWhenPresent() {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(new Fruit(1L, "Apple", "Hearty fruit")));

        Optional<FruitDTO> result = fruitService.getFruitByName("Apple");

        assertThat(result).contains(new FruitDTO(1L, "Apple", "Hearty fruit", null));
        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void addStoresFruitThroughRepository() {
        when(fruitRepository.save(any(Fruit.class))).thenReturn(new Fruit(3L, "Pear", "Green fruit"));

        FruitDTO result = fruitService.createFruit(new FruitDTO(null, "Pear", "Green fruit", null));

        assertThat(result).isEqualTo(new FruitDTO(3L, "Pear", "Green fruit", null));
        verify(fruitRepository).save(any(Fruit.class));
        verifyNoMoreInteractions(fruitRepository);
    }
}
