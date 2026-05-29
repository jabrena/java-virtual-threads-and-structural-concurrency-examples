# Framework Services

This folder contains the Fruit Store implementations for Spring Boot, Quarkus, and Micronaut, plus a Docker Compose setup for running them together with PostgreSQL.

## Ports

| Service | URL |
| --- | --- |
| Spring Boot | `http://localhost:8080` |
| Quarkus | `http://localhost:8081` |
| Micronaut | `http://localhost:8082` |
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |
| Tempo | `http://localhost:3200` |
| Pyroscope | `http://localhost:4040` |
| Grafana Alloy | `http://localhost:12345` |
| OpenTelemetry Collector OTLP/gRPC | `localhost:4317` |
| OpenTelemetry Collector OTLP/HTTP | `localhost:4318` |
| Spring Boot PostgreSQL | `localhost:5432` |
| Quarkus PostgreSQL | `localhost:5433` |
| Micronaut PostgreSQL | `localhost:5434` |

Each framework uses its own PostgreSQL container and database:

| Framework | Container | Database |
| --- | --- | --- |
| Spring Boot | `fruit-store-spring-boot-postgresql` | `fruits_spring_boot` |
| Quarkus | `fruit-store-quarkus-postgresql` | `fruits_quarkus` |
| Micronaut | `fruit-store-micronaut-postgresql` | `fruits_micronaut` |

All PostgreSQL databases use user `fruits` and password `fruits`.

## Run

From the repository root:

```bash
docker compose -f frameworks/docker-compose.yml up --build
```

Run in the background:

```bash
docker compose -f frameworks/docker-compose.yml up -d
```

Stop everything:

```bash
docker compose -f frameworks/docker-compose.yml down
```

Remove the PostgreSQL and Maven cache volumes:

```bash
docker compose -f frameworks/docker-compose.yml down -v
```

## Curl Checks

List fruits:

```bash
curl http://localhost:8080/fruits
curl http://localhost:8081/fruits
curl http://localhost:8082/fruits
```

Get one fruit:

```bash
curl http://localhost:8080/fruits/Apple
curl http://localhost:8081/fruits/Apple
curl http://localhost:8082/fruits/Apple
```

Create a fruit:

```bash
curl -i -X POST http://localhost:8080/fruits \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grapefruit","description":"Summer fruit"}'

curl -i -X POST http://localhost:8081/fruits \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grapefruit","description":"Summer fruit"}'

curl -i -X POST http://localhost:8082/fruits \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grapefruit","description":"Summer fruit"}'
```

## Observability

Docker Compose starts Grafana, Prometheus, Tempo, Pyroscope, Grafana Alloy, and an OpenTelemetry Collector alongside the framework services.

| Signal | Destination | Notes |
| --- | --- | --- |
| Spring Boot traces | OpenTelemetry Collector -> Tempo | OTLP/HTTP through `MANAGEMENT_OTLP_TRACING_ENDPOINT` |
| Quarkus traces | OpenTelemetry Collector -> Tempo | OTLP/gRPC through `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT` |
| Micronaut traces | OpenTelemetry Collector -> Tempo | OTLP/gRPC through `OTEL_EXPORTER_OTLP_ENDPOINT` |
| Spring Boot profiles | Grafana Alloy -> Pyroscope | async-profiler Java profiling from the `fruit-store-spring-boot` container |
| Quarkus profiles | Grafana Alloy -> Pyroscope | async-profiler Java profiling from the `fruit-store-quarkus` container |
| Micronaut profiles | Grafana Alloy -> Pyroscope | async-profiler Java profiling from the `fruit-store-micronaut` container |
| Spring Boot metrics | Prometheus scrape | `/actuator/prometheus` |
| Quarkus metrics | Prometheus scrape | `/q/metrics` |
| Micronaut metrics | Prometheus scrape | `/prometheus` |

Grafana is available without a login at [http://localhost:3000](http://localhost:3000), with Prometheus, Tempo, and Pyroscope provisioned as data sources:

- [Grafana home](http://localhost:3000)
- [Grafana data sources](http://localhost:3000/connections/datasources)
- [Grafana explore](http://localhost:3000/explore)
- [Grafana profiles drilldown](http://localhost:3000/a/grafana-pyroscope-app/profiles-explorer)

The three services use a 10% trace sampling ratio to keep observability overhead comparable during benchmarks. Grafana Alloy continuously profiles the three Java service containers with async-profiler and forwards CPU and allocation samples to Pyroscope. This keeps profiling in the OpenTelemetry/Grafana collector layer without adding profiling code to the Spring Boot, Quarkus, or Micronaut applications.

Health and metrics checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/q/health
curl http://localhost:8081/q/metrics
curl http://localhost:8082/health
curl http://localhost:8082/prometheus
curl http://localhost:4040/ready
curl http://localhost:12345/-/ready
```

Profile queries in Grafana Explore:

- Select the `Pyroscope` data source.
- Select the `process_cpu:cpu:nanoseconds:cpu:nanoseconds` profile type.
- Use `{service_name="spring-boot-fruit-store"}`, `{service_name="quarkus-fruit-store"}`, or `{service_name="micronaut-fruit-store"}`.
- If profiles are empty, generate load first with the `/fruits` curl checks and wait at least one Alloy profiling interval.

## Virtual Threads

The three services use Java virtual threads at different integration points. The endpoints are intentionally similar, but each framework exposes virtual-thread support through its own execution model.

| Framework | HTTP server model | Current configuration | Scope |
| --- | --- | --- | --- |
| Spring Boot | Spring WebMVC on an embedded Servlet container, Tomcat by default | `spring.threads.virtual.enabled=true` in `application.yml` | Framework-level switch for request handling and supported async executors |
| Quarkus | Quarkus HTTP on Vert.x, backed by Netty event loops | `@RunOnVirtualThread` on the REST controller | Endpoint/class-level opt-in for blocking REST work |
| Micronaut | Micronaut HTTP Server Netty | `@ExecuteOn(TaskExecutors.VIRTUAL)` on the REST controller | Controller/method-level dispatch to Micronaut's virtual-thread executor |

Virtual threads help when a request spends most of its time waiting on blocking operations, such as JDBC or an external HTTP call. A platform thread is expensive to park while it waits, so traditional blocking servers usually need careful thread-pool sizing. A virtual thread is much cheaper to park, which lets the application keep more blocking requests in flight without needing the same number of operating-system threads.

They do not make the CPU faster, and they do not remove downstream limits. In these services, PostgreSQL connections, Hikari/Agroal pool settings, transaction duration, and lock contention still define the real throughput ceiling once concurrency rises.

### Spring Boot

Spring Boot enables virtual threads through configuration:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Pros:

- Minimal code changes.
- Good fit for traditional blocking Servlet and JDBC applications.
- Easy to apply consistently across the application.
- Helps Tomcat handle blocking controller work with fewer platform threads.

Cons:

- Broad switch, so the execution policy is less explicit at individual endpoint level.
- Virtual threads improve blocking concurrency, but they do not remove database pool limits.
- Pinning or blocking in synchronized sections and native calls can still reduce scalability.

### Quarkus

Quarkus uses `@RunOnVirtualThread`:

```java
@RunOnVirtualThread
@Path("/fruits")
public class FruitController {
}
```

Pros:

- Explicit opt-in where blocking work is expected.
- Keeps the default event-loop model intact for non-blocking endpoints.
- Useful for REST endpoints that call blocking persistence APIs.
- Protects Vert.x/Netty event-loop threads from being blocked by JDBC work.

Cons:

- Blocking endpoints and classes need to be annotated deliberately.
- Mixing event-loop, worker-thread, and virtual-thread execution requires care.
- It is not a replacement for tuning Agroal or JDBC pool size.

### Micronaut

Micronaut uses `@ExecuteOn(TaskExecutors.VIRTUAL)`:

```java
@ExecuteOn(TaskExecutors.VIRTUAL)
@Controller("/fruits")
public class FruitController {
}
```

Pros:

- Explicit and readable execution policy.
- Can be applied at class or method level.
- Works well when the Netty HTTP server delegates blocking controller work away from event-loop threads.
- Protects Netty event-loop threads while keeping blocking controller code simple.

Cons:

- Blocking paths need annotation discipline.
- Too-broad use can hide where blocking actually happens.
- JDBC pool and transaction boundaries remain the real throughput constraints once request concurrency rises.

### Practical Guidance

Use virtual threads for blocking request/response code such as JDBC, file I/O, or calls to blocking clients. Keep event-loop threads free in Quarkus and Micronaut; event loops should accept connections, parse requests, and dispatch work quickly, not wait for database calls. For fair benchmarking, compare virtual-thread settings together with database pool size, RSS, p95/p99 latency, and failed requests. A virtual thread can make waiting cheaper, but it cannot make PostgreSQL accept more concurrent work than the configured pools and database can handle.

## Notes

Each service uses an isolated PostgreSQL container when started through Docker Compose. The service containers build the application with Maven and then replace the shell with the packaged JVM application using `exec java -jar ...`, so benchmark RSS measurements do not include Maven run goals or framework dev-mode processes. Spring Boot initializes its schema through Boot SQL initialization; Quarkus and Micronaut initialize their schemas with small JDBC startup initializers that run `schema.sql` and `import.sql`.
