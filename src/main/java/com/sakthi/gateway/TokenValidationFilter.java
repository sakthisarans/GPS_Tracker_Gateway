package com.sakthi.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component
public class TokenValidationFilter extends AbstractGatewayFilterFactory<TokenValidationFilter.Config> {

    @Value("${tracker.validateUrl}")
    String validateUrl;

    final RestTemplate restTemplate=new RestTemplate();

    public TokenValidationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if(exchange.getRequest().getURI().getPath().contains("api-docs")){
                return chain.filter(exchange);
            }
//            else if(Objects.requireNonNull(exchange.getRequest().getHeaders().get("Authorization")).get(0)!=null){
//                return Mono.defer(() -> {
//                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                    return exchange.getResponse().setComplete();
//                });
//            }
            String email="";
            try {
                email = (validateToken(Objects.requireNonNull(exchange.getRequest().getHeaders().get("Authorization")).get(0)));
            }catch (HttpClientErrorException ex){
                if(ex.getStatusCode()==HttpStatus.UNAUTHORIZED){
                    return Mono.defer(() -> {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                    });
                }else{
                    return Mono.defer(() -> {
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    });
                }
            }
            HttpHeaders updatedRequestHeaders = updateRequestHeaders(exchange, email);
            System.out.println(updatedRequestHeaders);
            ServerHttpRequest originalRequest = exchange.getRequest();
            ServerHttpRequest.Builder requestBuilder = originalRequest.mutate();
            requestBuilder.headers(httpHeaders -> {
                List<String> headersToRemove = new ArrayList<>();
                updatedRequestHeaders.forEach((key, value) -> {
                    String lowerCaseKey = key.toLowerCase();
                    httpHeaders.entrySet().stream()
                            .filter(entry -> entry.getKey().toLowerCase().equals(lowerCaseKey))
                            .forEach(entry -> headersToRemove.add(entry.getKey()));
                });
                headersToRemove.forEach(httpHeaders::remove);
                httpHeaders.addAll(updatedRequestHeaders);
            });

            System.out.println(exchange.getRequest().getHeaders());
            return chain.filter(exchange);
        };
    }

    private HttpHeaders updateRequestHeaders(ServerWebExchange exchange, String email){
        HttpHeaders headers = exchange.getRequest().getHeaders();
        HttpHeaders updatedHeaders = new HttpHeaders();
        updatedHeaders.add("email", email);
        return updatedHeaders;
    }

    public String validateToken(String token){
        URI genericUri= UriComponentsBuilder.fromHttpUrl(validateUrl).path("/tracker/user/validateToken").build().toUri();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-Type", "application/json");
        headers.add("Authorization", token);
        ResponseEntity<JsonNode> email=restTemplate.exchange(genericUri,HttpMethod.GET,new HttpEntity<>(headers), JsonNode.class);
        if(Objects.requireNonNull(email.getBody()).has("email")){
            return email.getBody().get("email").textValue();
        }else {
            return null;
        }
    }

    public static class Config {
    }

}
