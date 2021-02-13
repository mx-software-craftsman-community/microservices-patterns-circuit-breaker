package org.software.crafters.mx.microservices.patterns.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SimpleCircuitBreakerIT {

    private RestTemplate restTemplate;
    private Function<String, String> protectedFunction;
    private Function<String, String> fallbackFunction;

    public SimpleCircuitBreakerIT() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        restTemplate = restTemplateBuilder.build();
    }

    @BeforeEach
    public void setup() {
        protectedFunction = url -> {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
            return responseEntity.getBody();
        };

        fallbackFunction = url -> {
            return "Hello World!";
        };
    }

    @Test
    public void functionCallSuccessfully() {
        System.out.println("Test - Function call successfully [Closed state]");
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker<>(protectedFunction,
                fallbackFunction);
        HttpbinService service = new HttpbinService(circuitBreaker);
        assertTrue(service.invokeGetMethod("/get").contains("https://httpbin.org/get"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void functionCallFailedAndIsUnderFailureThreshold() {
        System.out.println("Test - Function call failed and is under the failure threshold [Closed state]");
        int failureThreshold = 3;
        SimpleCircuitBreaker<String, String> circuitBreaker = new SimpleCircuitBreaker(protectedFunction,
                failureThreshold, fallbackFunction);
        HttpbinService service = new HttpbinService(circuitBreaker);

        callFailedFunctionUntilUnderThreshold(service);
        assertEquals(circuitBreaker.getFailureThreshold() - 1, circuitBreaker.getFailureCount());
        assertEquals(SimpleCircuitBreaker.State.CLOSED, circuitBreaker.getState());
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

    private void callFailedFunctionUntilUnderThreshold(HttpbinService service) {
        SimpleCircuitBreaker<String, String> circuitBreaker = service.circuitBreaker;
        while (circuitBreaker.getFailureCount() < circuitBreaker.getFailureThreshold() - 1) {
            try {
                service.invokeGetMethod("/status/500");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
