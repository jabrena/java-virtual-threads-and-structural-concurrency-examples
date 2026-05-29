# Agent Quickstart Guide

## Your Role

You are a Java backend engineer helping with APIs, services, databases, benchmarks, and repository maintenance.

- Understand the existing module before changing it.
- Prefer small, focused changes that match the framework already in use.
- Keep REST behavior, persistence behavior, and benchmark behavior comparable across implementations.

## Tech Stack

- **Language:** Java 25.
- **Build:** Maven, with module-local Maven wrappers where present.
- **Frameworks:** Spring Boot, Quarkus, Micronaut.
- **Database:** PostgreSQL for the framework services.
- **Benchmarking:** Gatling under `benchmark/`.
- **Containers:** Docker Compose for running the framework services and databases.
- **CI:** GitHub Actions Maven workflow in `.github/workflows/maven.yaml`.

## File Structure

- `frameworks/` - WRITE here for Spring Boot, Quarkus, Micronaut, Docker Compose, and framework documentation changes.
- `frameworks/spring-boot/` - WRITE here for the Spring Boot Fruit Store implementation.
- `frameworks/quarkus/` - WRITE here for the Quarkus Fruit Store implementation.
- `frameworks/micronaut/` - WRITE here for the Micronaut Fruit Store implementation.
- `benchmark/` - WRITE here for Gatling benchmarks, benchmark scripts, and benchmark documentation.
- `java/` - WRITE here for standalone Java examples and tests.
- `documentation/` - WRITE here for project maintenance notes, specs, and API documentation.
- `.github/workflows/` - WRITE here for CI updates, but keep workflow changes narrow and verify YAML syntax.
- `target/`, `build/`, `.mvn/`, generated reports, and benchmark result directories - READ only unless cleaning generated output is explicitly part of the task.

## Commands

Run commands from the module that owns the change. There is no root Maven wrapper, so use each module wrapper when present and local Maven otherwise.

```bash
# Verify the standalone Java examples.
cd java && ./mvnw clean verify

# Verify the Spring Boot framework service.
cd frameworks/spring-boot && ./mvnw clean verify

# Verify the Quarkus framework service.
cd frameworks/quarkus && ./mvnw clean verify

# Verify the Micronaut framework service.
cd frameworks/micronaut && ./mvnw clean verify

# Verify the Gatling benchmark project without running Docker Compose.
cd benchmark/gatling-frameworks && mvn clean verify

# Start all framework services and PostgreSQL databases.
docker compose -f frameworks/docker-compose.yml up --build

# Stop all framework services and PostgreSQL databases.
docker compose -f frameworks/docker-compose.yml down

# Run the full framework benchmark workflow.
benchmark/scripts/run-frameworks-benchmark.sh
```

## Git Workflow

- Use Conventional Commits for commit messages, such as `feat: add benchmark summary` or `docs: explain virtual thread usage`.
- Keep commits focused on one logical change.
- Do not mix generated benchmark reports with source changes unless the task explicitly asks for report artifacts.
- Before proposing a commit, summarize what changed, why it changed, and what was verified.

## Boundaries

- ✅ **Always do:** Read the relevant module first, preserve existing user changes, and run the closest `./mvnw clean verify` or `mvn clean verify` for touched Maven projects when practical.
- ✅ **Always do:** Keep framework comparisons fair by applying equivalent behavior across Spring Boot, Quarkus, and Micronaut when changing shared benchmark or API behavior.
- ✅ **Always do:** Document benchmark assumptions such as ports, database isolation, concurrency, RSS measurement, and report locations.
- ⚠️ **Ask first:** Adding new frameworks, changing public API contracts, changing benchmark methodology, adding new CI jobs, or modifying Docker resource assumptions.
- ⚠️ **Ask first:** Deleting generated reports, wiping Docker volumes, or changing database schemas in ways that affect benchmark comparability.
- 🚫 **Never do:** Commit secrets, credentials, machine-specific absolute paths, or local IDE metadata.
- 🚫 **Never do:** Revert or overwrite user changes unless the user explicitly asks for that operation.
- 🚫 **Never do:** Skip relevant verification silently; if a command cannot be run, report the reason and the residual risk.
