package com.restaurant.pos.auth;

import com.restaurant.pos.common.BusinessRuleException;
import com.restaurant.pos.user.Role;
import com.restaurant.pos.user.User;
import com.restaurant.pos.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder pinEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService,
                       BCryptPasswordEncoder pinEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.pinEncoder = pinEncoder;
    }

    public AuthResponse login(String pin) {
        List<User> activeUsers = userRepository.findAllByStatus("ACTIVE");

        User matched = activeUsers.stream()
                .filter(u -> u.getRole() != Role.KITCHEN)
                .filter(u -> u.getPinHash() != null)
                .filter(u -> pinEncoder.matches(pin, u.getPinHash()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Invalid PIN"));

        matched.setLastLoginAt(LocalDateTime.now());
        userRepository.save(matched);

        String token = jwtService.generateToken(matched);
        return new AuthResponse(token, matched.getId(), matched.getUsername(),
                matched.getFullName(), matched.getRole().name());
    }
}
