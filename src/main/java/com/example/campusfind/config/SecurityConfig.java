package com.example.campusfind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/reports/new", "/reports/*/contact", "/account", "/inbox", "/inbox/**", "/saved", "/notifications", "/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/", "/reports", "/reports/*", "/about", "/login", "/register", "/api/**", "/css/**", "/images/**", "/uploads/**", "/js/**", "/error/**", "/manifest.webmanifest", "/manifest.json", "/sw.js").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reports", "/reports/*/resolve", "/reports/*/contact", "/reports/*/save", "/inbox/*/messages").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/account", false).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/?logout").permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
