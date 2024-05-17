package cz.cas.lib.bankid_registrator.configurations;

import cz.cas.lib.bankid_registrator.services.AppUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
    @Autowired
    private AppUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
            .authorizeHttpRequests(authorize -> authorize
                    .antMatchers("/dashboard").authenticated()
                    .antMatchers("/user/register", "/user/login").permitAll())
            .formLogin(formLogin -> formLogin
                    .loginPage("/user/login")
                    .failureUrl("/user/login?error=true")
                    .defaultSuccessUrl("/dashboard", true)
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/user/logout")
                    .logoutSuccessUrl("/user/login")
                    .permitAll());
            // .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}