package org.acme.rest;

import jakarta.validation.Valid;
import java.util.List;
import org.acme.dto.FruitDTO;
import org.acme.service.FruitService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fruits")
public class FruitController {

    private final FruitService fruitService;

    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @GetMapping
    public List<FruitDTO> getAll() {
        return fruitService.getAllFruits();
    }

    @GetMapping("/{name}")
    public ResponseEntity<FruitDTO> getFruit(@PathVariable String name) {
        return fruitService.getFruitByName(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public FruitDTO addFruit(@Valid @RequestBody FruitDTO fruit) {
        return fruitService.createFruit(fruit);
    }
}
