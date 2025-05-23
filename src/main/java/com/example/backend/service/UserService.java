package com.example.backend.service;

import com.example.backend.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User save(User user);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findByUsername(String username);
    User findById(Long id);
} 