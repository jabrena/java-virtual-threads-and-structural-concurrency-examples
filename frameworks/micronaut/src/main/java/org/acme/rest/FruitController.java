package org.acme.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import java.util.List;
import org.acme.dto.FruitDTO;
import org.acme.service.FruitService;

@Controller("/fruits")
@ExecuteOn(TaskExecutors.VIRTUAL)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitController {

    private final FruitService fruitService;

    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @Get
    @WithSpan("FruitController.getAll")
    public List<FruitDTO> getAll() {
        Span.current().setAttribute("fruit.operation", "list");
        return fruitService.getAllFruits();
    }

    @Get("/{name}")
    @WithSpan("FruitController.getFruit")
    public HttpResponse<FruitDTO> getFruit(String name) {
        Span.current()
            .setAttribute("fruit.operation", "lookup")
            .setAttribute("fruit.lookup.name.present", name != null);
        return fruitService.getFruitByName(name)
            .map(HttpResponse::ok)
            .orElseGet(HttpResponse::notFound);
    }

    @Post
    @WithSpan("FruitController.addFruit")
    public FruitDTO addFruit(@Body @Valid FruitDTO fruit) {
        Span.current()
            .setAttribute("fruit.operation", "create")
            .setAttribute("fruit.payload.name.present", fruit.name() != null);
        return fruitService.createFruit(fruit);
    }
}
