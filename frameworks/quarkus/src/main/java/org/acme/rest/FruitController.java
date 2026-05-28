package org.acme.rest;

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
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FruitController {

    private final FruitService fruitService;

    @Inject
    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @GET
    public List<FruitDTO> getAll() {
        return fruitService.getAllFruits();
    }

    @GET
    @Path("/{name}")
    public Response getFruit(@PathParam("name") String name) {
        return fruitService.getFruitByName(name)
                .map(fruit -> Response.ok(fruit).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public FruitDTO addFruit(@Valid FruitDTO fruit) {
        return fruitService.createFruit(fruit);
    }
}
