package com.gitu.expense_tracker.service;

import com.gitu.expense_tracker.dto.AuthResponse;
import com.gitu.expense_tracker.dto.LoginRequest;
import com.gitu.expense_tracker.dto.RegisterRequest;
import com.gitu.expense_tracker.dto.UserResponse;
import com.gitu.expense_tracker.entity.User;
import com.gitu.expense_tracker.exception.ConflictException;
import com.gitu.expense_tracker.exception.InvalidCredentialsException;
import com.gitu.expense_tracker.repository.UserRepository;
import com.gitu.expense_tracker.security.JwtService;
import com.gitu.expense_tracker.security.UserPrincipal;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedAt(Instant.now());
        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("An account with this email already exists");
        }
        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.generateToken(new UserPrincipal(user.getId(), user.getEmail())),
                UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
