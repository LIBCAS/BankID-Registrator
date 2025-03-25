package cz.cas.lib.bankid_registrator.configurations;

import lombok.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class AppConfig
{
    @Value("${retirement-age:70}")
    private int retirementAge;

    @Bean
    /**
     * ModelMapper bean for mapping DTOs to entities/models and vice versa
     * @return
     */
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
