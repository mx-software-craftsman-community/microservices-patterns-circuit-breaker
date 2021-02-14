package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleCircuitBreakerTest {

    private Function<String, String> remoteCall;
    private Function<String, String> fallbackMethod;

    @BeforeEach
    public void setup() {
        remoteCall = name -> {
            if (name != null) {
                return String.format("Hello %s!", name);
            }
            throw new IllegalArgumentException("Invalid argument: name cannot be null");
        };

        fallbackMethod = arg -> {
            return "Hello World!";
        };
    }

    @Test
    public void callSuccessfully() throws Exception {
        System.out.println("Test - Call successfully [Closed state]");
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, fallbackMethod);
        assertEquals("Hello Gerardo!", circuitBreaker.call("Gerardo"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void callFailedAndFailureCountIsUnderFailureThreshold() {
        System.out.println("Test - Call failed and failure count is under the failure threshold [Closed state]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                fallbackMethod);

        callFailedUntilBeingUnderThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold() - 1, circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void callFailedAndFailureCountReachesThreshold() {
        System.out.println("Test - Call failed and failure count reaches threshold [Goes to Open state]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                fallbackMethod);

        callFailedUntilReachesThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold(), circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void callInOpenStateUnderRetryTimeout() throws Exception {
        System.out.println("Test - Call in Open state under retry timeout [Calls the fallback method]");
        int failureThreshold = 3;
        long retryTimeout = 5000L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);

        callFailedUntilReachesThreshold(circuitBreaker);
        assertEquals("Hello World!", circuitBreaker.call(null));
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void callInHalfOpenStateSuccessfully() throws Exception {
        System.out.println("Test - Call in Half-Open state successfully [Goes to Closed state]");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);

        callFailedUntilReachesThreshold(circuitBreaker);
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals(SimpleCircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        assertEquals("Hello Gerardo!", circuitBreaker.call("Gerardo"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(0L, circuitBreaker.getLastFailureTime());
    }

    @Test
    public void callInHalfOpenStateFailed() throws Exception {
        System.out.println("Test - Call in Half-Open state failed [Goes to Open state again]");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);

        callFailedUntilReachesThreshold(circuitBreaker);
        long timeFailureReachesThreshold = circuitBreaker.getLastFailureTime();
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals(SimpleCircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        assertEquals("Hello World!", circuitBreaker.call(null));
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(failureThreshold + 1, circuitBreaker.getFailureCount());
        assertTrue(circuitBreaker.getLastFailureTime() > timeFailureReachesThreshold);
    }

    private void callFailedUntilBeingUnderThreshold(SimpleCircuitBreaker<String, String> circuitBreaker) {
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold() - 1) {
            try {
                circuitBreaker.call(null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void callFailedUntilReachesThreshold(SimpleCircuitBreaker<String, String> circuitBreaker) {
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold()) {
            try {
                circuitBreaker.call(null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
