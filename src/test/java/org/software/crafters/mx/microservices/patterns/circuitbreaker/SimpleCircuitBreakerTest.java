package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleCircuitBreakerTest {

    @Test
    public void executeFunctionCallSuccessfully() {
        SimpleCircuitBreaker<String, String> circuitBreaker =
                new SimpleCircuitBreaker(name -> { return String.format("Hello %s!", name);});
        assertEquals("Hello Gerardo!", circuitBreaker.execute("Gerardo"));
        assertEquals(SimpleCircuitBreaker.Status.CLOSED, circuitBreaker.getStatus());
    }
}
