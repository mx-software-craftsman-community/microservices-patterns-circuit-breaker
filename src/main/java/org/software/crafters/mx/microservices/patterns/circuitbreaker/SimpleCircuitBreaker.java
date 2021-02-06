package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private final Function<T, R> protectedFunction;
    private final int failureThreshold;

    public SimpleCircuitBreaker(Function<T, R> protectedFunction) {
        this.protectedFunction = protectedFunction;
        this.failureThreshold = 1;
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold) {
        this.protectedFunction = protectedFunction;
        this.failureThreshold = failureThreshold;
    }

    public R call(T arg) {
        return protectedFunction.apply(arg);
    }

    public Status getStatus() {
        return Status.CLOSED;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public enum Status {CLOSED}
}
