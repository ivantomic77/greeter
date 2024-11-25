package dev.tomic.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private final KeycloakAuthenticator keycloakAuthenticator;

    public SecurityConfig(KeycloakAuthenticator keycloakAuthenticator) {
        this.keycloakAuthenticator = keycloakAuthenticator;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpSecurity) {
        return serverHttpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .anyExchange().authenticated()
                ).oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults()))
                .addFilterAt(customFilter(), SecurityWebFiltersOrder.LAST)
                .build();
    }

    private WebFilter customFilter() {
        return ((exchange, chain) -> {
            String originalHeaderValue = exchange.getRequest().getHeaders().getFirst("Authorization");

            exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.set("x-user-auth", originalHeaderValue));
            exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.setBearerAuth(keycloakAuthenticator.getCredentials().accessToken));

            return chain.filter(exchange);
        });
    }
}