package org.acme.benchmark;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

public class FrameworksSimulation extends Simulation {

  private static final int WARMUP_USERS = integerProperty("warmupUsers", 3);
  private static final double USERS_PER_SECOND = doubleProperty("usersPerSecond", 10.0);
  private static final int DURATION_SECONDS = integerProperty("durationSeconds", 30);

  private static final HttpProtocolBuilder HTTP_PROTOCOL = http
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")
      .userAgentHeader("gatling-frameworks-benchmark");

  private static final OpenInjectionStep WARMUP = atOnceUsers(WARMUP_USERS);
  private static final OpenInjectionStep LOAD = constantUsersPerSec(USERS_PER_SECOND)
      .during(Duration.ofSeconds(DURATION_SECONDS));

  public FrameworksSimulation() {
    setUp(
        frameworkScenario("01 Spring Boot", "http://localhost:8080").injectOpen(WARMUP, LOAD),
        frameworkScenario("02 Quarkus", "http://localhost:8081").injectOpen(WARMUP, LOAD),
        frameworkScenario("03 Micronaut", "http://localhost:8082").injectOpen(WARMUP, LOAD))
        .protocols(HTTP_PROTOCOL)
        .assertions(global().failedRequests().count().is(0L));
  }

  private static ScenarioBuilder frameworkScenario(String framework, String baseUrl) {
    return scenario(framework)
        .exec(group(framework).on(
            listFruits(baseUrl)
                .pause(Duration.ofMillis(250))
                .exec(getApple(baseUrl))));
  }

  private static ChainBuilder listFruits(String baseUrl) {
    return exec(http("GET /fruits")
        .get(baseUrl + "/fruits")
        .check(status().is(200)));
  }

  private static ChainBuilder getApple(String baseUrl) {
    return exec(http("GET /fruits/{name}")
        .get(baseUrl + "/fruits/Apple")
        .check(status().is(200)));
  }

  private static int integerProperty(String name, int defaultValue) {
    return Integer.getInteger(name, defaultValue);
  }

  private static double doubleProperty(String name, double defaultValue) {
    String value = System.getProperty(name);
    return value == null || value.isBlank() ? defaultValue : Double.parseDouble(value);
  }
}
