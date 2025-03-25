package cz.cas.lib.bankid_registrator.configurations;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for external API services
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig
{
    private MapyCz mapyCz;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class MapyCz {
        private String key;
    }
}