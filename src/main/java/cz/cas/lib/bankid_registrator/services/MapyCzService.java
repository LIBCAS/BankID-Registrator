package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class MapyCzService
{
    private final ApiConfig apiConfig;
    private final WebClient webClient;

    public MapyCzService(ApiConfig apiConfig, WebClient.Builder webClientBuilder) {
        this.apiConfig = apiConfig;
        this.webClient = webClientBuilder.baseUrl("https://api.mapy.cz/v1").build();
    }

    public Mono<String> suggestAddress(String query) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/suggest")
                .queryParam("lang", "cs")
                .queryParam("limit", 5)
                .queryParam("type", "regional")
                .queryParam("locality", "cz")
                .queryParam("apikey", this.apiConfig.getMapyCz().getKey())
                .queryParam("query", query)
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(WebClientResponseException.class, e -> Mono.empty());
    }
}
