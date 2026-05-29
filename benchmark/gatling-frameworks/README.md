# Gatling Framework Benchmark

This Maven project benchmarks the three Fruit Store framework implementations:

| Framework | URL |
| --- | --- |
| Spring Boot | `http://localhost:8080` |
| Quarkus | `http://localhost:8081` |
| Micronaut | `http://localhost:8082` |

Run the baseline benchmark from the repository root:

```bash
./benchmark/scripts/run-frameworks-benchmark.sh
```

Select a benchmark profile:

```bash
./benchmark/scripts/run-frameworks-benchmark.sh smoke
./benchmark/scripts/run-frameworks-benchmark.sh baseline
./benchmark/scripts/run-frameworks-benchmark.sh stress
./benchmark/scripts/run-frameworks-benchmark.sh heavy
./benchmark/scripts/run-frameworks-benchmark.sh soak
```

| Profile | Users/sec per framework | Total users/sec | Duration | Warmup users per framework |
| --- | ---: | ---: | ---: | ---: |
| `smoke` | 5 | 15 | 30s | 3 |
| `baseline` | 25 | 75 | 120s | 10 |
| `stress` | 75 | 225 | 180s | 20 |
| `heavy` | 150 | 450 | 300s | 30 |
| `soak` | 50 | 150 | 900s | 20 |

The default profile is `baseline`. For the first serious stress run, use:

```bash
./benchmark/scripts/run-frameworks-benchmark.sh stress
```

Optional load settings still work for custom experiments:

```bash
USERS_PER_SECOND=100 DURATION_SECONDS=120 WARMUP_USERS=20 ./benchmark/scripts/run-frameworks-benchmark.sh custom
```

`USERS_PER_SECOND` is applied to each framework scenario. For example, `stress` runs `75` users/sec against Spring Boot, `75` users/sec against Quarkus, and `75` users/sec against Micronaut, for `225` users/sec total.

The script starts `frameworks/docker-compose.yml`, waits until all three `/fruits` endpoints respond, runs Gatling, stops Docker Compose, generates `framework-summary.html`, and opens that summary report.

During the Gatling run, the script also samples RSS from the three framework containers every 2 seconds and writes the samples to `rss-samples.csv` in the generated report directory. You can change the sampling interval:

```bash
RSS_SAMPLE_INTERVAL_SECONDS=5 ./benchmark/scripts/run-frameworks-benchmark.sh baseline
```

## Report Layout

The simulation creates one Gatling group per framework:

| Gatling group | Service |
| --- | --- |
| `01 Spring Boot` | `http://localhost:8080` |
| `02 Quarkus` | `http://localhost:8081` |
| `03 Micronaut` | `http://localhost:8082` |

In the HTML report, use the framework group rows or group pages to compare total response times by implementation. The child request rows below each group show the endpoint-level details for `GET /fruits` and `GET /fruits/{name}`.

The generated `framework-summary.html` page is a companion report with framework-colored rows:

| Framework | Color |
| --- | --- |
| `01 Spring Boot` | red |
| `02 Quarkus` | green |
| `03 Micronaut` | blue |

Use this page for the quick comparison of request count, errors, throughput, p95, p99, max, mean, standard deviation, average RSS, and max RSS. Use Gatling's original `index.html` for the full charts and endpoint drill-downs.
