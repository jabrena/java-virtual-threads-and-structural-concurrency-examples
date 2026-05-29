package info.jab.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class GodAnalysisTest {

    private static final BigInteger EXPECTED_SUM = new BigInteger("78179288397447443426");

    @Test
    public void shouldCalculateGodSumWithExecutorServiceAndFuture() {
        assertEquals(EXPECTED_SUM, GodAnalysis.sumWithExecutorServiceAndFuture("n"));
    }

    @Test
    public void shouldCalculateGodSumWithCompletableFuture() {
        assertEquals(EXPECTED_SUM, GodAnalysis.sumWithCompletableFuture("n"));
    }

    @Test
    public void shouldCalculateGodSumWithStructuredConcurrency() {
        assertEquals(EXPECTED_SUM, GodAnalysis.sumWithStructuredConcurrency("n"));
    }
}
