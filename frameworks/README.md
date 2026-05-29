# Framework Services

This folder contains the Fruit Store implementations for Spring Boot, Quarkus, and Micronaut, plus a Docker Compose setup for running them together with PostgreSQL.

## Ports

| Service | URL |
| --- | --- |
| Spring Boot | `http://localhost:8080` |
| Quarkus | `http://localhost:8081` |
| Micronaut | `http://localhost:8082` |
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

## Notes

Each service uses an isolated PostgreSQL container when started through Docker Compose. The service containers build the application with Maven and then replace the shell with the packaged JVM application using `exec java -jar ...`, so benchmark RSS measurements do not include Maven run goals or framework dev-mode processes. Spring Boot and Micronaut initialize their schemas from their module resources; Quarkus initializes its schema through Hibernate and `import.sql`.
