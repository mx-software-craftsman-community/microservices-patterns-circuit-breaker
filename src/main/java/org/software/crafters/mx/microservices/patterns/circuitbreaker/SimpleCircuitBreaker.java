package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private static final int DEFAULT_FAILURE_THRESHOLD = 1;
    private static final long DEFAULT_RETRY_TIMEOUT = 3000L;

    private final Function<T, R> protectedFunction;
    private final int failureThreshold;
    private final long retryTimeout;
    private final Function<T, R> fallbackFunction;
    private Status status;
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
        this.status = Status.CLOSED;
        this.failureCount = 0;
        this.lastFailureTime = 0;
    }

    public R call(T arg) throws Exception {
        switch (status) {
            case CLOSED:
                try {
                    return protectedFunction.apply(arg);
                } catch (Exception e) {
                    registerFailure(e);
                    if (failureCountReachesThreshold()) {
                        trip();
                    }
                    throw e;
                }
            case OPEN:
                if (isUnderRetryTimeout()) {
                    return fallbackFunction.apply(arg);
                } else {
                    this.status = Status.HALF_OPEN;
                    try {
                        R result = protectedFunction.apply(arg);
                        this.status = Status.CLOSED;
                        this.failureCount = 0;
                        this.lastFailureTime = 0;
                        return result;
                    } catch (Exception e) {
                        registerFailure(e);
                        if (failureCountReachesThreshold()) {
                            trip();
                        }
                        return fallbackFunction.apply(arg);
                    }
                }
        }
        return null;
    }

    private void registerFailure(Exception e) {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
    }

    private boolean failureCountReachesThreshold() {
        return failureCount >= failureThreshold;
    }

    private void trip() {
        this.status = Status.OPEN;
    }

    private void goToHalOpenState() {
        this.status = Status.HALF_OPEN;
    }

    private boolean isUnderRetryTimeout() {
        long elapsedTimeFromLastFailure = System.currentTimeMillis() - lastFailureTime;
        return elapsedTimeFromLastFailure < retryTimeout;
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

    public long getLastFailureTime() {
        return lastFailureTime;
    }

    public enum Status {CLOSED, OPEN, HALF_OPEN}
}
