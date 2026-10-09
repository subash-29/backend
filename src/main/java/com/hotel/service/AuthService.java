package com.hotel.service;

import com.hotel.dto.Dtos.*;
import com.hotel.entity.Role;
import com.hotel.entity.User;
import com.hotel.exception.ApiException;
import com.hotel.repository.UserRepository;
import com.hotel.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = users.save(User.builder()
                .name(req.name().trim())
                .email(email)
                .password(encoder.encode(req.password()))
                .role(Role.GUEST)   // admins are never self-registered
                .build());
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()),
                u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
