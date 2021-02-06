package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SimpleCircuitBreakerTest {

    @Test
    public void callFunctionSuccessfully() {
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
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(name -> {
            if (name != null) {
                return String.format("Hello %s!", name);
            }
            throw new IllegalArgumentException("Invalid argument: name cannot be null");
        },
        failureThreshold);

        int failureCounter = 0;
        while(failureCounter < circuitBreaker.getFailureThreshold() - 1) {
            try {
                circuitBreaker.call(null);
            } catch (IllegalArgumentException) {
                failureCounter++;
            }
        }
        assertEquals(circuitBreaker.getFailureThreshold() - 1, failureCounter);

    }
}
