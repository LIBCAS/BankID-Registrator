package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate
 */
@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}