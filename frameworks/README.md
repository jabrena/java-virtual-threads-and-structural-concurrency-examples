# Framework Services

This folder contains the Fruit Store implementations for Spring Boot, Quarkus, and Micronaut, plus a Docker Compose setup for running them together with PostgreSQL.

## Ports

| Service | URL |
| --- | --- |
| Spring Boot | `http://localhost:8080` |
| Quarkus | `http://localhost:8081` |
| Micronaut | `http://localhost:8082` |
| PostgreSQL | `localhost:5432` |

PostgreSQL uses database `fruits`, user `fruits`, and password `fruits`.

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

All three services use the shared PostgreSQL container when started through Docker Compose. Spring Boot and Micronaut initialize their schemas from their module resources; Quarkus initializes its schema through Hibernate and `import.sql`.
