package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleCircuitBreakerIT {

    private RestTemplate restTemplate;
    private Function<String, String> remoteCall;
    private Function<String, String> fallbackMethod;

    public SimpleCircuitBreakerIT() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        restTemplate = restTemplateBuilder.build();
    }

    @BeforeEach
    public void setup() {
        remoteCall = url -> {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            return responseEntity.getBody();
        };

        fallbackMethod = url -> {
            return "Hello World!";
        };
    }

    @Test
    public void callSuccessfully() {
        System.out.println("Integration Test - Call successfully [Closed state]");
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker<>(remoteCall, fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);
        assertTrue(service.invokeGetMethod("/get").contains("https://httpbin.org/get"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void callFailedAndFailureCountIsUnderFailureThreshold() {
        System.out.println("Integration Test - Call failed and failure count is under the failure threshold [Closed state]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedUntilBeingUnderThreshold(service);
        assertEquals(circuitBreaker.getFailureThreshold() - 1, circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void callFailedAndFailureCountReachesThreshold() {
        System.out.println("Integration Test - Call failed and failure count reaches threshold [Goes to Open state]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedUntilReachesThreshold(service);
        assertEquals(circuitBreaker.getFailureThreshold(), circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void callInOpenStateUnderRetryTimeout() throws Exception {
        System.out.println("Integration Test - Call in Open state under retry timeout [Calls the fallback method]");
        int failureThreshold = 3;
        long retryTimeout = 5000L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedUntilReachesThreshold(service);
        assertEquals("Hello World!", service.invokeGetMethod("/status/500"));
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void callInHalfOpenStateSuccessfully() throws Exception {
        System.out.println("Integration Test - Call in Half-Open state successfully [Goes to Closed state]");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedUntilReachesThreshold(service);
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals(SimpleCircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        assertTrue(service.invokeGetMethod("/get").contains("https://httpbin.org/get"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getFailureCount());
        assertEquals(0L, circuitBreaker.getLastFailureTime());
    }

    @Test
    public void callInHalfOpenStateFailed() throws Exception {
        System.out.println("Integration Test - Call in Half-Open state failed [Goes to Open state again]");
        int failureThreshold = 3;
        long retryTimeout = 100L;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(remoteCall, failureThreshold,
                retryTimeout, fallbackMethod);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedUntilReachesThreshold(service);
        long timeFailureReachesThreshold = circuitBreaker.getLastFailureTime();
        TimeUnit.MILLISECONDS.sleep(150L);
        assertEquals(SimpleCircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        assertEquals("Hello World!", service.invokeGetMethod("/status/500"));
        assertEquals(SimpleCircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(failureThreshold + 1, circuitBreaker.getFailureCount());
        assertTrue(circuitBreaker.getLastFailureTime() > timeFailureReachesThreshold);
    }

    private static class HttpbinService {
        private static final String URL_BASE = "https://httpbin.org";

        private final SimpleCircuitBreaker<String, String> circuitBreaker;

        public HttpbinService(SimpleCircuitBreaker<String, String> circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public String invokeGetMethod(String resourceUrl) {
            return circuitBreaker.call(URL_BASE.concat(resourceUrl));
        }

        public SimpleCircuitBreaker<String, String> getCircuitBreaker() {
            return circuitBreaker;
        }
    }

    private void callFailedUntilBeingUnderThreshold(HttpbinService service) {
        SimpleCircuitBreaker<String, String> circuitBreaker = service.circuitBreaker;
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold() - 1) {
            try {
                service.invokeGetMethod("/status/500");
            } catch (Exception e) {
                System.out.println("Exception message: " + e.getMessage());
            }
        }
    }

    private void callFailedUntilReachesThreshold(HttpbinService service) {
        SimpleCircuitBreaker<String, String> circuitBreaker = service.circuitBreaker;
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold()) {
            try {
                service.invokeGetMethod("/status/500");
            } catch (Exception e) {
                System.out.println("Exception message: " + e.getMessage());
            }
        }
    }
}
