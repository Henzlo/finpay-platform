package com.finpay.auth.dto;

import com.finpay.auth.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserDTO {

    private UUID id;
    private String name;
    private String email;
    private Role role;
    private boolean isVerified;
    private String provider;
}
