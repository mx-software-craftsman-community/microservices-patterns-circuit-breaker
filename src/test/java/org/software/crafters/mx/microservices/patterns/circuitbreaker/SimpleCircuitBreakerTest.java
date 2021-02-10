package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleCircuitBreakerTest {

    private Function<String, String> protectedFunction;
    private Function<String, String> fallbackFunction;

    @BeforeEach
    public void setup() {
        protectedFunction = name -> {
            if (name != null) {
                return String.format("Hello %s!", name);
            }
            throw new IllegalArgumentException("Invalid argument: name cannot be null");
        };

        fallbackFunction = arg -> {
            return "Hello World!";
        };
    }

    @Test
    public void callFunctionSuccessfully() throws Exception {
        System.out.println("Test - Call function successfully [Closed status]");
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                fallbackFunction);
        assertEquals("Hello Gerardo!", circuitBreaker.call("Gerardo"));
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }

    @Test
    public void functionCallFailedAndIsUnderFailureThreshold() {
        System.out.println("Test - Function call failed and is under the failure threshold [Closed status]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, fallbackFunction);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilUnderThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold() - 1, circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }

    @Test
    public void functionCallFailedAndFailureCountReachesThreshold() {
        System.out.println("Test - Function call failed and is failure count reaches threshold [Goes to Open status]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, fallbackFunction);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilReachesThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold(), circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.Status.OPEN, circuitBreaker.getStatus());
    }

    @Test
    public void callFunctionOnOpenStatusUnderRetryTimeout() throws Exception {
        System.out.println("Test - Call function on Open status under retry timeout");
        int failureThreshold = 3;
        long retryTimeout = 5000L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, retryTimeout, fallbackFunction);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilReachesThreshold(circuitBreaker);
        assertEquals("Hello World!", circuitBreaker.call(null));
        assertEquals(SimpleCircuitBreaker.Status.OPEN, circuitBreaker.getStatus());
    }

    @Test
    public void functionCallOnHalfOpenStatusSuccessfully() throws Exception {
        System.out.println("Test - Function call on Half-Open status successfully");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, retryTimeout, fallbackFunction);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilReachesThreshold(circuitBreaker);
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals("Hello Gerardo!", circuitBreaker.call("Gerardo"));
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(0L, circuitBreaker.getLastFailureTime());
    }

    @Test
    public void functionCallOnHalfOpenStatusFailed() throws Exception {
        System.out.println("Test - Function call on Half-Open status failed");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, retryTimeout, fallbackFunction);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilReachesThreshold(circuitBreaker);
        long timeFailureReachesThreshold = circuitBreaker.getLastFailureTime();
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals("Hello World!", circuitBreaker.call(null));
        assertEquals(SimpleCircuitBreaker.Status.OPEN, circuitBreaker.getStatus());
        assertEquals(failureThreshold + 1, circuitBreaker.getFailureCount());
        assertTrue(circuitBreaker.getLastFailureTime() > timeFailureReachesThreshold);
    }

    private void callFailedFunctionUntilUnderThreshold(SimpleCircuitBreaker<String, String> circuitBreaker) {
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold() - 1) {
            try {
                circuitBreaker.call(null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void callFailedFunctionUntilReachesThreshold(SimpleCircuitBreaker<String, String> circuitBreaker) {
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold()) {
            try {
                circuitBreaker.call(null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
