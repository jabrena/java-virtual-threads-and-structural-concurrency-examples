package org.acme.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.acme.domain.Address;
import org.acme.domain.Fruit;
import org.acme.domain.Store;
import org.acme.domain.StoreFruitPrice;
import org.acme.repository.FruitRepository;
import org.acme.service.DefaultFruitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FruitController.class)
@Import(DefaultFruitService.class)
class FruitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FruitRepository fruitRepository;

    @Test
    void getAllReturnsFruits() throws Exception {
        when(fruitRepository.findAll()).thenReturn(List.of(createFruit()));

        mockMvc.perform(get("/fruits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].description").value("Hearty Fruit"))
                .andExpect(jsonPath("$[0].storePrices", hasSize(1)))
                .andExpect(jsonPath("$[0].storePrices[0].store.id").value(1))
                .andExpect(jsonPath("$[0].storePrices[0].store.name").value("Some Store"))
                .andExpect(jsonPath("$[0].storePrices[0].store.currency").value("USD"))
                .andExpect(jsonPath("$[0].storePrices[0].store.address.address").value("123 Some St"))
                .andExpect(jsonPath("$[0].storePrices[0].store.address.city").value("Some City"))
                .andExpect(jsonPath("$[0].storePrices[0].store.address.country").value("USA"))
                .andExpect(jsonPath("$[0].storePrices[0].price").value(1.29));

        verify(fruitRepository).findAll();
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitReturnsFruitWhenFound() throws Exception {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.of(createFruit()));

        mockMvc.perform(get("/fruits/Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.description").value("Hearty Fruit"))
                .andExpect(jsonPath("$.storePrices[0].store.name").value("Some Store"))
                .andExpect(jsonPath("$.storePrices[0].price").value(1.29));

        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void getFruitReturnsEmptyNotFoundWhenMissing() throws Exception {
        when(fruitRepository.findByName("Apple")).thenReturn(Optional.empty());

        mockMvc.perform(get("/fruits/Apple"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(fruitRepository).findByName("Apple");
        verifyNoMoreInteractions(fruitRepository);
    }

    @Test
    void addFruitValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/fruits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Missing name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addFruitReturnsCreatedFruit() throws Exception {
        when(fruitRepository.save(any(Fruit.class)))
                .thenReturn(new Fruit(3L, "Grapefruit", "Summer fruit"));

        mockMvc.perform(post("/fruits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grapefruit\",\"description\":\"Summer fruit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Grapefruit"))
                .andExpect(jsonPath("$.description").value("Summer fruit"))
                .andExpect(jsonPath("$.storePrices").doesNotExist());

        verify(fruitRepository).save(any(Fruit.class));
        verifyNoMoreInteractions(fruitRepository);
    }

    private static Fruit createFruit() {
        Fruit fruit = new Fruit(1L, "Apple", "Hearty Fruit");
        Store store = new Store(1L, "Some Store", new Address("123 Some St", "Some City", "USA"), "USD");
        fruit.setStorePrices(List.of(new StoreFruitPrice(store, fruit, BigDecimal.valueOf(1.29))));
        return fruit;
    }
}
