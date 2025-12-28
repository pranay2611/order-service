package com.dissertation.orderservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequest {
    private String orderNumber;
    private String username;
    private BigDecimal amount;
}

