package cz.cas.lib.bankid_registrator.configurations;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig
{
    @Bean
    /**
     * ModelMapper bean for mapping DTOs to entities/models and vice versa
     * @return
     */
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
