package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import java.util.function.Function;

public class SimpleCircuitBreaker<T, R> {

    private static final int DEFAULT_FAILURE_THRESHOLD = 1;
    private static final long DEFAULT_RETRY_TIMEOUT = 3000L;

    private final Function<T, R> protectedFunction;
    private final int failureThreshold;
    private final long retryTimeout;
    private final Function<T, R> fallbackFunction;
    private final FailureMonitor failureMonitor;

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
        this.failureMonitor = new FailureMonitor();
    }

    public R call(T arg) throws RuntimeException {
        switch (getState()) {
            case CLOSED:
                try {
                    return protectedFunction.apply(arg);
                } catch (RuntimeException e) {
                    failureMonitor.update(e);
                    throw e;
                }
            case OPEN:
                return fallbackFunction.apply(arg);
            case HALF_OPEN:
            default:
                try {
                    R result = protectedFunction.apply(arg);
                    failureMonitor.reset();
                    return result;
                } catch (RuntimeException e) {
                    failureMonitor.update(e);
                    return fallbackFunction.apply(arg);
                }
        }
    }

    private boolean isFailureCountUnderThreshold() {
        return failureMonitor.getFailureCount() < failureThreshold;
    }

    private boolean isUnderRetryTimeout() {
        long elapsedTimeFromLastFailure = System.currentTimeMillis() - failureMonitor.getLastFailureTime();
        return elapsedTimeFromLastFailure < retryTimeout;
    }

    public State getState() {
        if (isFailureCountUnderThreshold()) {
            return State.CLOSED;
        } else {
            if (isUnderRetryTimeout()) {
                return State.OPEN;
            } else {
                return State.HALF_OPEN;
            }
        }
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public long getRetryTimeout() {
        return retryTimeout;
    }

    public int getFailureCount() {
        return failureMonitor.getFailureCount();
    }

    public long getLastFailureTime() {
        return failureMonitor.getLastFailureTime();
    }

    public enum State {CLOSED, OPEN, HALF_OPEN}

    private static class FailureMonitor {
        private int failureCount;
        private long lastFailureTime;

        FailureMonitor() {
            reset();
        }

        private void reset() {
            this.failureCount = 0;
            this.lastFailureTime = 0L;
        }

        private void update(Exception e) {
            this.failureCount++;
            this.lastFailureTime = System.currentTimeMillis();
        }

        public int getFailureCount() {
            return this.failureCount;
        }

        public long getLastFailureTime() {
            return this.lastFailureTime;
        }
    }
}
