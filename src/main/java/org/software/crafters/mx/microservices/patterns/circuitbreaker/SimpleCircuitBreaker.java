package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private final Function<T, R> protectedFunction;
    private Status status;
    private final int failureThreshold;
    private int failureCount;

    public SimpleCircuitBreaker(Function<T, R> protectedFunction) {
        this(protectedFunction, 1);
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold) {
        this.protectedFunction = protectedFunction;
        this.status = Status.CLOSED;
        this.failureThreshold = failureThreshold;
        this.failureCount = 0;
    }

    public R call(T arg) throws Exception {
        try {
            return protectedFunction.apply(arg);
        } catch (Exception e) {
            failureCount++;
            if(failureCountReachesThreshold()) {
                trip();
            }
            throw e;
        }
    }

    private boolean failureCountReachesThreshold() {
        return failureCount == failureThreshold;
    }

    private void trip() {
        this.status = Status.OPEN;
    }

    public Status getStatus() {
        return status;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public enum Status {OPEN, CLOSED}
}
