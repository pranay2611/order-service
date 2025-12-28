package com.dissertation.orderservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private String username;
    private String email;
    private String subject;
    private String message;
    private String type;
}

