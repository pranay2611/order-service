package com.dissertation.orderservice.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private String status;
    private String orderNumber;
}

