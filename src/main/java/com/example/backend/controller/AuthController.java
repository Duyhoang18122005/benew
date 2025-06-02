package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.security.JwtTokenUtil;
import com.example.backend.service.UserService;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                         JwtTokenUtil jwtTokenUtil,
                         UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenUtil.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại");
        }

        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setFullName(registerRequest.getFullName());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setGender(registerRequest.getGender());
        user.getRoles().add("ROLE_USER");

        userService.save(user);

        return ResponseEntity.ok("Đăng ký thành công");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(
            @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.update(
            currentUser.getId(),
            request.getFullName(),
            request.getDateOfBirth(),
            request.getPhoneNumber(),
            request.getAddress(),
            request.getBio(),
            request.getGender()
        );
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/update/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.updateAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/update/profile-image")
    public ResponseEntity<?> updateProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        User updatedUser = userService.updateProfileImage(currentUser.getId(), file);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        userService.deleteAvatar(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/profile-image")
    public ResponseEntity<?> deleteProfileImage(Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        userService.deleteProfileImage(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/device-token")
    public ResponseEntity<?> saveDeviceToken(@RequestBody DeviceTokenRequest request, Authentication authentication) {
        System.out.println("[AuthController] ====== START saveDeviceToken ======");
        System.out.println("[AuthController] Request body: " + request);
        System.out.println("[AuthController] Authentication: " + authentication);
        System.out.println("[AuthController] Authentication name: " + authentication.getName());
        System.out.println("[AuthController] Authentication authorities: " + authentication.getAuthorities());
        
        try {
            if (request.getDeviceToken() == null || request.getDeviceToken().trim().isEmpty()) {
                System.out.println("[AuthController] Device token is null or empty");
                System.out.println("[AuthController] ====== END saveDeviceToken with error ======");
                return ResponseEntity.badRequest().body("Token thiết bị không được để trống");
            }

            User user = userService.findByUsername(authentication.getName());
            System.out.println("[AuthController] Found user: " + user);
            System.out.println("[AuthController] Current device token: " + user.getDeviceToken());
            System.out.println("[AuthController] New device token: " + request.getDeviceToken().trim());
            
            user.setDeviceToken(request.getDeviceToken().trim());
            userService.save(user);
            System.out.println("[AuthController] Device token updated successfully");
            System.out.println("[AuthController] ====== END saveDeviceToken ======");
            
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật token thiết bị thành công",
                "deviceToken", user.getDeviceToken()
            ));
        } catch (Exception e) {
            System.out.println("[AuthController] Exception: " + e.getMessage());
            e.printStackTrace();
            System.out.println("[AuthController] ====== END saveDeviceToken with error ======");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Không thể cập nhật token thiết bị: " + e.getMessage()));
        }
    }

    @PostMapping("/update/avatar-db")
    public ResponseEntity<?> updateAvatarDb(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        currentUser.setAvatarData(file.getBytes());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/avatar/{userId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        User user = userService.findById(userId);
        byte[] image = user.getAvatarData();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @PostMapping("/update/profile-image-db")
    public ResponseEntity<?> updateProfileImageDb(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        currentUser.setProfileImageData(file.getBytes());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/profile-image/{userId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long userId) {
        User user = userService.findById(userId);
        byte[] image = user.getProfileImageData();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @PostMapping("/update/avatar-from-url")
    public ResponseEntity<?> updateAvatarFromUrl(@RequestBody ImageUrlRequest request, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        URL url = new URL(request.getUrl());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
        }
        currentUser.setAvatarData(baos.toByteArray());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update/profile-image-from-url")
    public ResponseEntity<?> updateProfileImageFromUrl(@RequestBody ImageUrlRequest request, Authentication authentication) throws IOException {
        User currentUser = userService.findByUsername(authentication.getName());
        URL url = new URL(request.getUrl());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = is.read(buffer)) > 0) {
                baos.write(buffer, 0, n);
            }
        }
        currentUser.setProfileImageData(baos.toByteArray());
        userService.save(currentUser);
        return ResponseEntity.ok().build();
    }
}

@Data
class LoginRequest {
    private String username;
    private String password;
}

@Data
class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String gender;
}

@Data
class UpdateUserRequest {
    private String fullName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String address;
    private String bio;
    private String gender;
}

@Data
class JwtResponse {
    private String token;

    public JwtResponse(String token) {
        this.token = token;
    }
}

@Data
class ImageUrlRequest {
    private String url;
} 