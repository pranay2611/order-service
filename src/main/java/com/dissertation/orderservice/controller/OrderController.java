package com.dissertation.orderservice.controller;

import com.dissertation.orderservice.dto.CreateOrderRequest;
import com.dissertation.orderservice.dto.OrderResponse;
import com.dissertation.orderservice.model.OrderStatus;
import com.dissertation.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received order creation request for username: {}, product: {}, quantity: {}, unitPrice: {}", 
                request.getUsername(), request.getProductName(), request.getQuantity(), request.getUnitPrice());
        
        try {
            OrderResponse response = orderService.createOrder(request);
            if (response == null) {
                log.error("Order creation returned null response");
                throw new RuntimeException("Order creation returned null response");
            }
            log.info("Order created successfully: orderNumber={}, status={}", response.getOrderNumber(), response.getStatus());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Order creation failed with RuntimeException: {}", e.getMessage(), e);
            // Re-throw to let GlobalExceptionHandler handle it
            throw e;
        } catch (Exception e) {
            log.error("Order creation failed with unexpected exception: {}", e.getMessage(), e);
            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
    }
    
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }
    
    @GetMapping("/user/{username}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable String username) {
        return ResponseEntity.ok(orderService.getOrdersByUsername(username));
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    
    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, status));
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running");
    }
}

