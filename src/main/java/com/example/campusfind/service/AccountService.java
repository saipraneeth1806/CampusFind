package com.example.campusfind.service;

import com.example.campusfind.dto.RegistrationForm;
import com.example.campusfind.model.UserAccount;
import com.example.campusfind.repository.UserAccountRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements UserDetailsService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UserAccount register(RegistrationForm form) {
        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) throw new EmailAlreadyRegisteredException();
        UserAccount account = new UserAccount(form.getDisplayName().trim(), normalizedEmail,
                passwordEncoder.encode(form.getPassword()));
        account.setCreatedAt(java.time.LocalDateTime.now(clock));
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public UserAccount findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        UserAccount account = findByEmail(email);
        return User.withUsername(account.getEmail()).password(account.getPasswordHash()).roles("USER").build();
    }

    public static class EmailAlreadyRegisteredException extends RuntimeException { }
}
