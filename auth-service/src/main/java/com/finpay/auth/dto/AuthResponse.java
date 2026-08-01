package com.finpay.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    @Builder.Default
    private long expiresIn = 3600000;

    private UserDTO user;
}
