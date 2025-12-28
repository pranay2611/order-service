package com.dissertation.orderservice.dto;

import com.dissertation.orderservice.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String username;
    private String productName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentId;
    private LocalDateTime createdAt;
}

