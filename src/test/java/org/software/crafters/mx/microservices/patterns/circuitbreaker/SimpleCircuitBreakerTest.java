package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.Test;

public class SimpleCircuitBreakerTest {

    @Test
    public void executeFunctionCallSuccessfully() {
        SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker();
        circuitBreaker.execute();
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }
}
