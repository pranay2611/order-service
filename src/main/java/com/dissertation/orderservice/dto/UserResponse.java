package com.dissertation.orderservice.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean active;
}

