package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleCircuitBreakerTest {

    @Test
    public void callFunctionSuccessfully() throws Exception {
        System.out.println("Test - Call function successfully [Closed status]");
        SimpleCircuitBreaker<String, String> circuitBreaker =
                new SimpleCircuitBreaker(name -> {
                    return String.format("Hello %s!", name);
                });
        assertEquals("Hello Gerardo!", circuitBreaker.call("Gerardo"));
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }

    @Test
    public void functionCallFailedAndIsUnderFailureThreshold() {
        System.out.println("Test - Function call failed and is under the failure threshold [Closed status]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(
                name -> {
                    if (name != null) {
                        return String.format("Hello %s!", name);
                    }
                    throw new IllegalArgumentException("Invalid argument: name cannot be null");
                },
                failureThreshold);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilUnderThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold() - 1, circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }

    @Test
    public void functionCallFailedAndFailureCountReachesThreshold() {
        System.out.println("Test - Function call failed and is failure count reaches threshold [Goes to Open status]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(
                name -> {
                    if (name != null) {
                        return String.format("Hello %s!", name);
                    }
                    throw new IllegalArgumentException("Invalid argument: name cannot be null");
                },
                failureThreshold);

        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
        callFailedFunctionUntilReachesThreshold(circuitBreaker);
        assertEquals(circuitBreaker.getFailureThreshold(), circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.Status.OPEN, circuitBreaker.getStatus());
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
