package com.dissertation.orderservice.service;

import com.dissertation.orderservice.client.NotificationServiceClient;
import com.dissertation.orderservice.client.PaymentServiceClient;
import com.dissertation.orderservice.client.UserServiceClient;
import com.dissertation.orderservice.dto.*;
import com.dissertation.orderservice.model.Order;
import com.dissertation.orderservice.model.OrderStatus;
import com.dissertation.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Validate user exists
        UserResponse user;
        try {
            user = userServiceClient.getUserByUsername(request.getUsername());
            if (!user.isActive()) {
                throw new RuntimeException("User account is not active");
            }
        } catch (Exception e) {
            log.error("Failed to validate user: {}", e.getMessage());
            throw new RuntimeException("User validation failed: " + e.getMessage());
        }
        
        // Calculate total amount
        var totalAmount = request.getUnitPrice().multiply(
            java.math.BigDecimal.valueOf(request.getQuantity())
        );
        
        // Create order
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .username(request.getUsername())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();
        
        order = orderRepository.save(order);
        
        // Process payment
        try {
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);
            log.info("Processing payment for order: {}, amount: {}", order.getOrderNumber(), totalAmount);
            
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderNumber(order.getOrderNumber())
                    .username(request.getUsername())
                    .amount(totalAmount)
                    .build();
            
            PaymentResponse paymentResponse = null;
            try {
                paymentResponse = paymentServiceClient.processPayment(paymentRequest);
                log.info("Payment service response received: status={}, paymentId={}", 
                        paymentResponse != null ? paymentResponse.getStatus() : "null",
                        paymentResponse != null ? paymentResponse.getPaymentId() : "null");
            } catch (Exception feignException) {
                log.error("Feign client exception when calling payment service: {}", feignException.getMessage(), feignException);
                throw new RuntimeException("Payment service call failed: " + feignException.getMessage(), feignException);
            }
            
            if (paymentResponse == null) {
                log.error("Payment response is null");
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                throw new RuntimeException("Payment service returned null response");
            }
            
            if ("COMPLETED".equals(paymentResponse.getStatus())) {
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                order.setPaymentId(paymentResponse.getPaymentId());
                order = orderRepository.save(order);
                log.info("Payment completed successfully for order: {}", order.getOrderNumber());
                
                // Send notification
                sendOrderNotification(user, order, "Order Created Successfully");
            } else {
                log.warn("Payment processing failed. Status: {}, Order: {}", paymentResponse.getStatus(), order.getOrderNumber());
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                throw new RuntimeException("Payment processing failed with status: " + paymentResponse.getStatus());
            }
        } catch (RuntimeException e) {
            log.error("Payment processing failed with RuntimeException: {}", e.getMessage(), e);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw e; // Re-throw to preserve the original exception
        } catch (Exception e) {
            log.error("Payment processing failed with unexpected exception: {}", e.getMessage(), e);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
        
        return mapToResponse(order);
    }
    
    public List<OrderResponse> getOrdersByUsername(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        return mapToResponse(order);
    }
    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        
        order.setStatus(status);
        order = orderRepository.save(order);
        
        return mapToResponse(order);
    }
    
    private void sendOrderNotification(UserResponse user, Order order, String subject) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .subject(subject)
                    .message(String.format("Order %s for %s has been created. Total: $%.2f",
                            order.getOrderNumber(),
                            order.getProductName(),
                            order.getTotalAmount()))
                    .type("ORDER_CONFIRMATION")
                    .build();
            
            notificationServiceClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }
    
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .username(order.getUsername())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentId(order.getPaymentId())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt() : java.time.LocalDateTime.now())
                .build();
    }
}

