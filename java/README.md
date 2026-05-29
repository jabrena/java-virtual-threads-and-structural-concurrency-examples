# Java Concurrency Building Blocks

This module demonstrates the evolution of Java concurrency building blocks by solving the same problem three ways:

1. `ExecutorService` with `Future`
2. `CompletableFuture`
3. `StructuredTaskScope` with structured concurrency

The example is implemented in `info.jab.demo.GodAnalysis`. It calls three remote mythology endpoints, filters gods whose names match the requested starting letter, converts each matching name into a decimal representation, and returns the sum as a `BigInteger`.

## Problem

The module consumes these sources:

- Greek gods: `https://my-json-server.typicode.com/jabrena/latency-problems/greek`
- Roman gods: `https://my-json-server.typicode.com/jabrena/latency-problems/roman`
- Nordic gods: `https://my-json-server.typicode.com/jabrena/latency-problems/nordic`

For each source, the code:

1. Calls the endpoint with Java `HttpClient`.
2. Parses the returned JSON string array.
3. Filters names by prefix.
4. Converts every matching name into a decimal representation by concatenating character code points.
5. Adds all matching values across the three sources.

For example:

```text
Nike -> N(78)i(105)k(107)e(101) -> 78105107101
```

The three implementations all produce the same expected result:

```text
78179288397447443426
```

## 1. ExecutorService And Future

Method:

```java
GodAnalysis.sumWithExecutorServiceAndFuture("n")
```

This is the classic Java concurrency model:

- Create an `ExecutorService`.
- Submit one `Callable` per source.
- Receive one `Future` per task.
- Block on each `Future#get()`.
- Add each completed result.

This style gives explicit control over task submission and thread pool ownership. It is simple and familiar, but composition is manual. The caller must loop over futures, handle checked exceptions, preserve interruption, and decide what to do when one task fails.

In this module, failures from individual source calls are treated as partial results: a failed source contributes `0`, while successful sources still count.

## 2. CompletableFuture

Method:

```java
GodAnalysis.sumWithCompletableFuture("n")
```

`CompletableFuture` moves from blocking task handles toward asynchronous pipelines.

Instead of submitting a task and later calling `get()`, each endpoint call becomes a pipeline:

```text
sendAsync -> validate response -> parse JSON -> sum source -> recover failures
```

This style is better when computations need to be transformed, combined, or recovered asynchronously. Error handling can be attached directly to the pipeline with `exceptionally`, and the final aggregation joins only after the async work has been started.

The trade-off is readability: complex `CompletableFuture` chains can become hard to follow, especially when cancellation, timeouts, and nested composition enter the picture.

## 3. Structured Concurrency

Method:

```java
GodAnalysis.sumWithStructuredConcurrency("n")
```

Structured concurrency treats related concurrent tasks as a single operation with a clear lifetime.

The implementation opens a `StructuredTaskScope`, forks one subtask per source, joins the scope, and aggregates only successful subtasks:

```text
open scope -> fork subtasks -> join scope -> collect successful results -> close scope
```

This model makes the relationship between parent and child tasks explicit. The concurrent work starts and finishes inside a bounded lexical scope, which makes cancellation, failure handling, and resource cleanup easier to reason about than free-floating tasks.

In Java 25, `StructuredTaskScope` is still a preview API, so this module enables preview features in Maven.

## Evolution Summary

| Building block | Main idea | Strength | Cost |
| --- | --- | --- | --- |
| `Future` | A handle to a result that may complete later | Simple, explicit, widely known | Blocking and manual composition |
| `CompletableFuture` | Async stages that can transform and combine results | Good pipeline composition | Chains can become complex |
| Structured concurrency | A bounded scope for related concurrent tasks | Clear task lifetime and failure model | Requires newer Java and preview flags in Java 25 |

## Build And Test

This module requires Java 25.

Run tests from the repository root:

```bash
mvn -f java/pom.xml test
```

Or from the `java` directory:

```bash
./mvnw test
```

The Maven build enables preview features because the structured concurrency implementation uses `StructuredTaskScope`.

## Notes

- The HTTP client uses a 5-second request timeout.
- Source failures produce partial results instead of failing the whole aggregation.
- Tests call the live endpoints, so they require network access.
