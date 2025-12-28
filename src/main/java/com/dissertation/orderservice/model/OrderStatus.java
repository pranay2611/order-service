package com.dissertation.orderservice.model;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

