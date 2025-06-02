package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import com.example.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, 
                         PasswordEncoder passwordEncoder,
                         FileStorageService fileStorageService,
                         NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role))
                    .collect(java.util.stream.Collectors.toList())
        );
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Override
    public User save(User user) {
        // Chỉ mã hóa nếu password chưa được mã hóa (BCrypt hash luôn bắt đầu bằng $2a$ hoặc $2b$)
        if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    @Override
    public User update(Long id, String fullName, LocalDate dateOfBirth, String phoneNumber, 
                      String address, String bio, String gender) {
        User user = findById(id);
        user.setFullName(fullName);
        user.setDateOfBirth(dateOfBirth);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        user.setBio(bio);
        user.setGender(gender);
        return userRepository.save(user);
    }

    @Override
    public User updateAvatar(Long id, MultipartFile avatarFile) throws IOException {
        User user = findById(id);
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }
        String avatarUrl = fileStorageService.storeFile(avatarFile, "avatars");
        user.setAvatarUrl(avatarUrl);
        user.setAvatarData(avatarFile.getBytes());
        return userRepository.save(user);
    }

    @Override
    public User updateProfileImage(Long id, MultipartFile profileImageFile) throws IOException {
        User user = findById(id);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            fileStorageService.deleteFile(user.getProfileImageUrl());
        }
        String profileImageUrl = fileStorageService.storeFile(profileImageFile, "profile-images");
        user.setProfileImageUrl(profileImageUrl);
        user.setProfileImageData(profileImageFile.getBytes());
        return userRepository.save(user);
    }

    @Override
    public void deleteAvatar(Long id) throws IOException {
        User user = findById(id);
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            fileStorageService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            user.setAvatarData(null);
            userRepository.save(user);
        }
    }

    @Override
    public void deleteProfileImage(Long id) throws IOException {
        User user = findById(id);
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            fileStorageService.deleteFile(user.getProfileImageUrl());
            user.setProfileImageUrl(null);
            user.setProfileImageData(null);
            userRepository.save(user);
        }
    }

    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Gửi push notification khi đổi mật khẩu thành công
        if (user.getDeviceToken() != null && !user.getDeviceToken().isEmpty()) {
            notificationService.sendPushNotification(
                user.getDeviceToken(),
                "Đổi mật khẩu thành công!",
                "Bạn vừa đổi mật khẩu thành công.",
                null
            );
        }
    }
} 