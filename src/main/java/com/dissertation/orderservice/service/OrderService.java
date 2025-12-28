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
            
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderNumber(order.getOrderNumber())
                    .username(request.getUsername())
                    .amount(totalAmount)
                    .build();
            
            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);
            
            if ("COMPLETED".equals(paymentResponse.getStatus())) {
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                order.setPaymentId(paymentResponse.getPaymentId());
                order = orderRepository.save(order);
                
                // Send notification
                sendOrderNotification(user, order, "Order Created Successfully");
            } else {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                throw new RuntimeException("Payment processing failed");
            }
        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage());
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw new RuntimeException("Order creation failed: " + e.getMessage());
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

