package ar.edu.utn.donatrack.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
        String traceId = incoming != null && incoming.matches("[A-Za-z0-9_-]{1,128}")
            ? incoming : UUID.randomUUID().toString();

        ServerWebExchange traced = exchange.mutate()
            .request(exchange.getRequest().mutate().header(TRACE_HEADER, traceId).build())
            .response(exchange.getResponse())
            .build();
        traced.getResponse().getHeaders().set(TRACE_HEADER, traceId);
        return chain.filter(traced);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
