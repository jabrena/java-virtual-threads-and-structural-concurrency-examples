package org.acme.rest;

import io.opentelemetry.api.trace.Span;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.acme.dto.FruitDTO;
import org.acme.service.FruitService;

@Path("/fruits")
@RunOnVirtualThread
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitController {

    private final FruitService fruitService;

    @Inject
    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @GET
    @WithSpan("FruitController.getAll")
    public List<FruitDTO> getAll() {
        Span.current().setAttribute("fruit.operation", "list");
        return fruitService.getAllFruits();
    }

    @GET
    @Path("/{name}")
    @WithSpan("FruitController.getFruit")
    public Response getFruit(@PathParam("name") String name) {
        Span.current()
                .setAttribute("fruit.operation", "lookup")
                .setAttribute("fruit.lookup.name.present", name != null);
        return fruitService.getFruitByName(name)
                .map(fruit -> Response.ok(fruit).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @WithSpan("FruitController.addFruit")
    public FruitDTO addFruit(@Valid FruitDTO fruit) {
        Span.current()
                .setAttribute("fruit.operation", "create")
                .setAttribute("fruit.payload.name.present", fruit.name() != null);
        return fruitService.createFruit(fruit);
    }
}
