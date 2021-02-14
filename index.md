# Microservices Patterns - Circuit Breaker

## Problem

Software systems make remote calls to software running in different processes, usually on different machines across a network. One of the big differences between in-memory calls and remote calls is that remote calls can fail, or hang without a response until some timeout limit is reached. What's worse if you have many callers on an unresponsive supplier, then you can run out of critical resources leading to cascading failures across multiple systems.

## Solution

The Circuit Breaker pattern addresses the cascading failure problem caused for failing remote calls.  

The basic idea behind the Circuit Breaker is very simple. You **wrap** a **remote call** in a **circuit breaker object**, which **monitors for failures**. Once the **failures reach** a certain **threshold**, the circuit breaker **trips**, and all further calls to the circuit breaker return an error or are handled by calling a **local fallback method**, without the remote call being made at all.  

Once **open**, the Circuit Breaker can **itself detect** if the **remote calls are working again**. We can implement this behavior by trying the remote call again after a **suitable interval** (timeout), and **resetting** the Circuit Breaker if the remote call succeed.  

\* Michael Nygard popularized this pattern in his book Release It.  
  
  
The following diagram illustrates the basic behavior of the Circuit Breaker:

![Circuit Breaker basic behavior](/images/circuit-breaker-basic-behavior.png)
  
  
### States of the Circuit Breaker

- **Closed**: The Circuit Breaker performs the remote call, and if it succeeds, the circuit breaker remains closed.
  - If the remote call failed and the number of failures is under the predetermined threshold, the circuit remains closed, and returns an error.
  - If the remote call failed and the number of failures reaches the predetermined threshold, returns an error, the Circuit Breaker trips, and it goes to the open state.
- **Open**: While the Circuit Breaker is in the open state under a predetermined timeout, it doesn’t perform the remote call, and returns an error or calls a local fallback method.
  - If the predetermined timeout is reached, the Circuit Breaker goes to the half open state.
- **Half-Open**: The Circuit Breaker performs the remote call, and if it succeeds, the circuit breaker goes to the closed state.
  - If the remote call failed, the Circuit Breaker is once again tripped, an goes to the open state.
  
\* The above explanation shows the simplest behavior of a Circuit Breaker, but in practice, these provide more features and parameterization.  
  
The next diagram illustrates the states of the Circuit Breaker:

![Circuit Breaker basic behavior](/images/circuit-breaker-states.jpg)

### Implementation

The project associated with this page provides a simple circuit breaker that implements the previously described behavior. This implementations is only for demonstration purposes and was developed using practices described by Extreme Programming or Clean Code, for example:

- Test-First Programming / TDD
- To use intention-revealing names.
- Refactoring

## Final thoughts

The following notes were toked from Martin Fowler's Circuit Breaker blog post:

- “Not all errors should trip the circuit, some should reflect normal failures and be dealt with as part of regular logic.”
- “With lots of traffic, you can have problems with many calls just waiting for the initial timeout. Since remote calls are often slow, it's often a good idea to put each call on a different thread using a future or promise to handle the results when they come back. By drawing these threads from a thread pool, you can arrange for the circuit to break when the thread pool is exhausted.”
- “A more sophisticated approach might look at frequency of errors, tripping once you get, say, a 50% failure rate. You might also have different thresholds for different errors, such as a threshold of 10 for timeouts but 3 for connection failures.”
- “Circuit Breakers are also useful for asynchronous communications. A common technique here is to put all requests on a queue, which the supplier consumes at its speed - a useful technique to avoid overloading servers. In this case the circuit breaks when the queue fills up.”
- “Circuit Breakers help reduce resources tied up in operations which are likely to fail. You avoid waiting on timeouts for the client, and a broken circuit avoids putting load on a struggling server.”
Threads are not waiting for an unresponsive call, for example.
- “Circuit breakers are a valuable place for monitoring.”
“Breaker behavior is often a good source of warnings about deeper troubles in the environment. Operations staff should be able to trip or reset breakers.”

## Bibliography

[Martin Fowler’s Circuit Breaker blog post](https://martinfowler.com/bliki/CircuitBreaker.html)  

[DZone’s Circuit Breaker Pattern](https://dzone.com/articles/circuit-breaker-pattern)
