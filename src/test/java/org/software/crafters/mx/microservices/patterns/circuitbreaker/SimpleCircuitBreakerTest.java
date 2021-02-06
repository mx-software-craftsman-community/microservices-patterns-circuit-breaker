package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleCircuitBreakerTest {

    @Test
    public void executeFunctionCallSuccessfully() {
        SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker();
        circuitBreaker.execute();
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }
}
