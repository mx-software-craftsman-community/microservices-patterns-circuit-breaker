package org.software.crafters.mx.microservices.patterns.circuitbreaker;

public class SimpleCircuitBreaker {
    public void execute() {
    }

    public Status getStatus() {
        return Status.CLOSED;
    }

    public enum Status {CLOSED}
}
