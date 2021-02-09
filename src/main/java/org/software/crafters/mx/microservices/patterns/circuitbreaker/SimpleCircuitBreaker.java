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
    private long lastFailureTimestamp;

    public SimpleCircuitBreaker(Function<T, R> protectedFunction) {
        this(protectedFunction, DEFAULT_FAILURE_THRESHOLD);
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold) {
        this(protectedFunction, failureThreshold, DEFAULT_RETRY_TIMEOUT, null);
    }

    public SimpleCircuitBreaker(Function<T, R> protectedFunction, int failureThreshold, long retryTimeout,
                                Function<T, R> fallbackFunction) {
        this.protectedFunction = protectedFunction;
        this.failureThreshold = failureThreshold;
        this.retryTimeout = retryTimeout;
        this.fallbackFunction = fallbackFunction;
        this.status = Status.CLOSED;
        this.failureCount = 0;
    }

    public R call(T arg) throws Exception {
        switch (status) {
            case CLOSED:
                try {
                    return protectedFunction.apply(arg);
                } catch (Exception e) {
                    failureCount++;
                    lastFailureTimestamp = System.currentTimeMillis();
                    if (failureCountReachesThreshold()) {
                        trip();
                    }
                    throw e;
                }
            case OPEN:
                if (isUnderRetryTimeout()) {
                    return fallbackFunction.apply(arg);
                }
                break;
        }
        return null;
    }

    private boolean failureCountReachesThreshold() {
        return failureCount == failureThreshold;
    }

    private void trip() {
        this.status = Status.OPEN;
    }

    private boolean isUnderRetryTimeout() {
        long elapsedTimeLastFailure = System.currentTimeMillis() - lastFailureTimestamp;
        return elapsedTimeLastFailure < retryTimeout;
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
