package org.acme.service;

import java.util.List;
import java.util.Optional;
import org.acme.dto.FruitDTO;

public interface FruitService {

    List<FruitDTO> getAllFruits();

    Optional<FruitDTO> getFruitByName(String name);

    FruitDTO createFruit(FruitDTO fruit);
}
