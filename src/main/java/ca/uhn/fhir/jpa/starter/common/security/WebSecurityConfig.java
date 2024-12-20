package ca.uhn.fhir.jpa.starter.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Allow access to FHIR tester app and resources
                .requestMatchers("/", "/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico", "/resources/**", "/content/**").permitAll()
                // Allow access to FHIR tester pages and functionality
                .requestMatchers("/home", "/about", "/resource", "/search", "/read/**", "/history/**", "/delete/**", "/page/**", "/tester/**", "/server/**").permitAll()
                // Allow access to metadata endpoint
                .requestMatchers("/fhir/metadata").permitAll()
                // Require authentication for all other FHIR endpoints
                .requestMatchers("/fhir/**").authenticated()
                // Secure actuator endpoints
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt());
        
        return http.build();
    }
} 
