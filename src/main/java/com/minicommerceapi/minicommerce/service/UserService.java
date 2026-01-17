package com.minicommerceapi.minicommerce.service;

import com.minicommerceapi.minicommerce.domain.User;
import com.minicommerceapi.minicommerce.dto.UserDtos;
import com.minicommerceapi.minicommerce.exception.ConflictException;
import com.minicommerceapi.minicommerce.exception.NotFoundException;
import com.minicommerceapi.minicommerce.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already exists");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(normalizedEmail);
        u = userRepository.save(u);
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse get(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(u);
    }

    @Transactional
    public UserDtos.UserResponse update(Long id, UserDtos.UpdateUserRequest req) {
        User u = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        String normalizedEmail = normalizeEmail(req.email());
        if (!u.getEmail().equals(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email already exists");
        }
        u.setName(req.name().trim());
        u.setEmail(normalizedEmail);
        return toResponse(u);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    private UserDtos.UserResponse toResponse(User u) {
        return new UserDtos.UserResponse(u.getId(), u.getName(), u.getEmail());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
