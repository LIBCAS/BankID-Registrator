package cz.cas.lib.bankid_registrator.configurations;

import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.services.MainServiceTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class MainServiceConfig
{
    @Bean
    @Profile("production")
    public MainService mainService() {
        return new MainService();
    }

    @Bean
    @Profile({ "local", "testing" })
    public MainService mainServiceTest() {
        return new MainServiceTest();
    }
}