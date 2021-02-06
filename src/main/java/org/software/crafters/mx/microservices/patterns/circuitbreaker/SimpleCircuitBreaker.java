package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private final Function<T, R> protectedFunction;

    public SimpleCircuitBreaker(Function<T, R> protectedFunction) {
        this.protectedFunction = protectedFunction;
    }

    public R call(T arg) {
        return protectedFunction.apply(arg);
    }

    public Status getStatus() {
        return Status.CLOSED;
    }

    public enum Status {CLOSED}
}
