package com.finpay.auth.service;

import com.finpay.auth.dto.AuthResponse;
import com.finpay.auth.dto.LoginRequest;
import com.finpay.auth.dto.RegisterRequest;
import com.finpay.auth.dto.ResetPasswordRequest;
import com.finpay.auth.dto.UserDTO;
import com.finpay.auth.entity.Provider;
import com.finpay.auth.entity.Role;
import com.finpay.auth.entity.User;
import com.finpay.auth.exception.AccountDisabledException;
import com.finpay.auth.exception.InvalidCredentialsException;
import com.finpay.auth.exception.OtpInvalidException;
import com.finpay.auth.exception.TokenExpiredException;
import com.finpay.auth.exception.UserAlreadyExistsException;
import com.finpay.auth.exception.UserNotFoundException;
import com.finpay.auth.repository.UserRepository;
import com.finpay.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration BLACKLIST_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${google.oauth.client-secret}")
    private String googleClientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.BORROWER;
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .provider(Provider.LOCAL)
                .isVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        return issueTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            throw new TokenExpiredException("Refresh token is invalid or expired");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        String userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new TokenExpiredException("Refresh token is invalid or expired");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpiration())
                .user(toUserDto(user))
                .build();
    }

    public void logout(String authHeader, String userId) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            redisTemplate.opsForValue().set("blacklist:" + token, "true", BLACKLIST_TTL);
        }
        if (userId != null && !userId.isBlank()) {
            redisTemplate.delete("refresh:" + userId);
        }
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public AuthResponse googleLogin(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("grant_type", "authorization_code");

        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token",
                new HttpEntity<>(form, tokenHeaders),
                Map.class);

        Map<String, Object> tokenBody = tokenResponse.getBody();
        if (tokenBody == null || tokenBody.get("access_token") == null) {
            throw new InvalidCredentialsException("Google token exchange failed");
        }
        String googleAccessToken = tokenBody.get("access_token").toString();

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(googleAccessToken);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Map.class);

        Map<String, Object> profile = userResponse.getBody();
        if (profile == null || profile.get("email") == null) {
            throw new InvalidCredentialsException("Unable to fetch Google user profile");
        }

        String email = profile.get("email").toString().toLowerCase();
        String name = profile.get("name") != null ? profile.get("name").toString() : email;

        Optional<User> existing = userRepository.findByEmailAndProvider(email, Provider.GOOGLE);
        User user = existing.orElseGet(() -> userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(null)
                .role(Role.BORROWER)
                .provider(Provider.GOOGLE)
                .isVerified(true)
                .isActive(true)
                .build()));

        if (!user.isActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        return issueTokens(user);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisTemplate.opsForValue().set("otp:" + user.getEmail(), otp, OTP_TTL);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("FinPay Password Reset OTP");
        message.setText("Your OTP is: " + otp + ". Valid for 5 minutes.");
        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        String storedOtp = redisTemplate.opsForValue().get("otp:" + email);
        if (storedOtp == null) {
            throw new OtpInvalidException("OTP expired");
        }
        if (!storedOtp.equals(request.getOtp())) {
            throw new OtpInvalidException("Invalid OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete("otp:" + email);
    }

    public UserDTO getCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Missing Authorization header");
        }
        String token = authHeader.substring(7).trim();
        if (!jwtUtil.isTokenValid(token)) {
            throw new TokenExpiredException("Access token is invalid or expired");
        }
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toUserDto(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        redisTemplate.opsForValue().set("refresh:" + user.getId(), refreshToken, REFRESH_TTL);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpiration())
                .user(toUserDto(user))
                .build();
    }

    private UserDTO toUserDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .provider(user.getProvider().name())
                .build();
    }
}
