package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private static final int DEFAULT_FAILURE_THRESHOLD = 1;
    private static final long DEFAULT_RETRY_TIMEOUT = 3000L;

    private final Function<T, R> protectedFunction;
    private final int failureThreshold;
    private final long retryTimeout;
    private final Function<T, R> fallbackFunction;
    private int failureCount;
    private long lastFailureTime;

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, Function<T, R> fallbackFunction) {
        this(protectedFunction, DEFAULT_FAILURE_THRESHOLD, DEFAULT_RETRY_TIMEOUT, fallbackFunction);
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold,
                                Function<T, R> fallbackFunction) {
        this(protectedFunction, failureThreshold, DEFAULT_RETRY_TIMEOUT, fallbackFunction);
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold, long retryTimeout,
                                Function<T, R> fallbackFunction) {
        this.protectedFunction = protectedFunction;
        this.failureThreshold = failureThreshold;
        this.retryTimeout = retryTimeout;
        this.fallbackFunction = fallbackFunction;
        reset();
    }

    public R call(T arg) throws Exception {
        switch (getStatus()) {
            case CLOSED:
                try {
                    return protectedFunction.apply(arg);
                } catch (Exception e) {
                    registerFailure(e);
                    throw e;
                }
            case OPEN:
                return fallbackFunction.apply(arg);
            case HALF_OPEN:
                try {
                    R result = protectedFunction.apply(arg);
                    reset();
                    return result;
                } catch (Exception e) {
                    registerFailure(e);
                    return fallbackFunction.apply(arg);
                }
        }
        return null;
    }

    private void reset() {
        this.failureCount = 0;
        this.lastFailureTime = 0;
    }

    private void registerFailure(Exception e) {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
    }

    private boolean isFailureCountUnderThreshold() {
        return failureCount < failureThreshold;
    }

    private boolean isUnderRetryTimeout() {
        long elapsedTimeFromLastFailure = System.currentTimeMillis() - lastFailureTime;
        return elapsedTimeFromLastFailure < retryTimeout;
    }

    public Status getStatus() {
        if (isFailureCountUnderThreshold()) {
            return Status.CLOSED;
        } else {
            if (isUnderRetryTimeout()) {
                return Status.OPEN;
            } else {
                return Status.HALF_OPEN;
            }
        }
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public long getLastFailureTime() {
        return lastFailureTime;
    }

    public enum Status {CLOSED, OPEN, HALF_OPEN}
}
