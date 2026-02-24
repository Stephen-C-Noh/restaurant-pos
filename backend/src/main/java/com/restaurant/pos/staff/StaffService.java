package com.restaurant.pos.staff;

import com.restaurant.pos.common.BusinessRuleException;
import com.restaurant.pos.user.Role;
import com.restaurant.pos.user.User;
import com.restaurant.pos.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder pinEncoder;

    public StaffService(UserRepository userRepository, BCryptPasswordEncoder pinEncoder) {
        this.userRepository = userRepository;
        this.pinEncoder = pinEncoder;
    }

    public StaffResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username already exists: " + request.getUsername());
        }

        if (request.getPin() != null) {
            List<User> allUsers = userRepository.findAll();
            boolean pinTaken = allUsers.stream()
                    .filter(u -> u.getPinHash() != null)
                    .anyMatch(u -> pinEncoder.matches(request.getPin(), u.getPinHash()));
            if (pinTaken) {
                throw new BusinessRuleException("PIN is already in use");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setPasswordHash("N/A");
        user.setStatus("ACTIVE");
        if (request.getPin() != null) {
            user.setPinHash(pinEncoder.encode(request.getPin()));
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public List<StaffResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private StaffResponse toResponse(User user) {
        return new StaffResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
