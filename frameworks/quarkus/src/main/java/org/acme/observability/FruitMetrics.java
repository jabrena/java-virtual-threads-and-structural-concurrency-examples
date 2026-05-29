package org.acme.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;

@ApplicationScoped
public class FruitMetrics {

    private final Counter listSuccess;
    private final Counter listError;
    private final Counter lookupSuccess;
    private final Counter lookupNotFound;
    private final Counter lookupError;
    private final Counter createSuccess;
    private final Counter createError;
    private final Timer listDuration;
    private final Timer lookupDuration;
    private final Timer createDuration;

    public FruitMetrics(MeterRegistry registry) {
        this.listSuccess = requestCounter(registry, "list", "success");
        this.listError = requestCounter(registry, "list", "error");
        this.lookupSuccess = requestCounter(registry, "lookup", "success");
        this.lookupNotFound = requestCounter(registry, "lookup", "not_found");
        this.lookupError = requestCounter(registry, "lookup", "error");
        this.createSuccess = requestCounter(registry, "create", "success");
        this.createError = requestCounter(registry, "create", "error");
        this.listDuration = requestTimer(registry, "list");
        this.lookupDuration = requestTimer(registry, "lookup");
        this.createDuration = requestTimer(registry, "create");
    }

    public Timer.Sample start(MeterRegistry registry) {
        return Timer.start(registry);
    }

    public void recordListSuccess(Timer.Sample sample) {
        listSuccess.increment();
        sample.stop(listDuration);
    }

    public void recordListError(Timer.Sample sample) {
        listError.increment();
        sample.stop(listDuration);
    }

    public void recordLookup(Timer.Sample sample, boolean found) {
        if (found) {
            lookupSuccess.increment();
        } else {
            lookupNotFound.increment();
        }
        sample.stop(lookupDuration);
    }

    public void recordLookupError(Timer.Sample sample) {
        lookupError.increment();
        sample.stop(lookupDuration);
    }

    public void recordCreateSuccess(Timer.Sample sample) {
        createSuccess.increment();
        sample.stop(createDuration);
    }

    public void recordCreateError(Timer.Sample sample) {
        createError.increment();
        sample.stop(createDuration);
    }

    private static Counter requestCounter(MeterRegistry registry, String operation, String outcome) {
        return Counter.builder("fruit.store.requests")
                .description("Fruit store service requests")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry);
    }

    private static Timer requestTimer(MeterRegistry registry, String operation) {
        return Timer.builder("fruit.store.request.duration")
                .description("Fruit store service request duration")
                .tag("operation", operation)
                .serviceLevelObjectives(Duration.ofMillis(10), Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(250))
                .publishPercentileHistogram()
                .register(registry);
    }
}
