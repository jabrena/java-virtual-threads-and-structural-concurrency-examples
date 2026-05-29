package info.jab.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

public final class GodAnalysis {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private static final List<URI> SOURCES = List.of(
            URI.create("https://my-json-server.typicode.com/jabrena/latency-problems/greek"),
            URI.create("https://my-json-server.typicode.com/jabrena/latency-problems/roman"),
            URI.create("https://my-json-server.typicode.com/jabrena/latency-problems/nordic"));

    private GodAnalysis() {
    }

    public static BigInteger sumWithExecutorServiceAndFuture(String filter) {
        try (ExecutorService executor = Executors.newFixedThreadPool(SOURCES.size())) {
            List<Future<BigInteger>> futures = SOURCES.stream()
                    .map(source -> executor.submit(() -> sumSource(source, filter)))
                    .toList();

            BigInteger result = BigInteger.ZERO;
            for (Future<BigInteger> future : futures) {
                try {
                    result = result.add(future.get());
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("God analysis was interrupted", e);
                    }
                }
            }
            return result;
        }
    }

    public static BigInteger sumWithCompletableFuture(String filter) {
        List<CompletableFuture<BigInteger>> futures = SOURCES.stream()
                .map(source -> HTTP_CLIENT.sendAsync(request(source), HttpResponse.BodyHandlers.ofString())
                        .thenApply(GodAnalysis::successfulBody)
                        .thenApply(GodAnalysis::parseJsonStringArray)
                        .thenApply(gods -> sumGods(gods, filter))
                        .exceptionally(_ -> BigInteger.ZERO))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public static BigInteger sumWithStructuredConcurrency(String filter) {
        try (var scope = StructuredTaskScope
                .<BigInteger, Void>open(Joiner.<BigInteger>awaitAll())) {
            List<StructuredTaskScope.Subtask<BigInteger>> subtasks = SOURCES.stream()
                    .map(source -> scope.fork(() -> sumSource(source, filter)))
                    .toList();

            scope.join();
            return subtasks.stream()
                    .filter(subtask -> subtask.state() == StructuredTaskScope.Subtask.State.SUCCESS)
                    .map(StructuredTaskScope.Subtask::get)
                    .reduce(BigInteger.ZERO, BigInteger::add);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("God analysis was interrupted", e);
        }
    }

    private static BigInteger sumSource(URI source, String filter) throws Exception {
        HttpResponse<String> response = HTTP_CLIENT.send(request(source), HttpResponse.BodyHandlers.ofString());
        return sumGods(parseJsonStringArray(successfulBody(response)), filter);
    }

    private static BigInteger sumGods(List<String> gods, String filter) {
        String normalizedFilter = filter.toUpperCase(Locale.ROOT);
        return gods.stream()
                .filter(god -> god.startsWith(normalizedFilter))
                .map(GodAnalysis::toDecimalRepresentation)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private static HttpRequest request(URI source) {
        return HttpRequest.newBuilder(source)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
    }

    private static String successfulBody(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Unexpected response status: " + response.statusCode());
        }
        return response.body();
    }

    private static List<String> parseJsonStringArray(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Expected a JSON string array", e);
        }
    }

    private static BigInteger toDecimalRepresentation(String name) {
        StringBuilder decimalRepresentation = new StringBuilder();
        name.codePoints().forEach(decimalRepresentation::append);
        return new BigInteger(decimalRepresentation.toString());
    }
}
