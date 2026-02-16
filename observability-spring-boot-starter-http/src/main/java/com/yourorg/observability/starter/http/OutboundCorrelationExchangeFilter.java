package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsMdcKeys;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient equivalent of {@link OutboundCorrelationInterceptor}.
 * Adds X-Correlation-Id from MDC to outgoing reactive HTTP requests.
 *
 * <p>
 * Auto-configured when {@code spring-webflux} is on the classpath.
 * </p>
 */
public class OutboundCorrelationExchangeFilter implements ExchangeFilterFunction {

    private final String headerName;

    public OutboundCorrelationExchangeFilter(String headerName) {
        this.headerName = headerName;
    }

    @Override
    @NonNull
    public Mono<ClientResponse> filter(@NonNull ClientRequest request, @NonNull ExchangeFunction next) {
        String cid = MDC.get(ObsMdcKeys.CORRELATION_ID);
        if (cid != null && !cid.isBlank() && !request.headers().containsKey(headerName)) {
            ClientRequest mutated = ClientRequest.from(request)
                    .header(headerName, java.util.Objects.requireNonNull(cid))
                    .build();
            return next.exchange(mutated);
        }
        return next.exchange(request);
    }
}
