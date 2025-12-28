package com.dissertation.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dissertation.orderservice.client.NotificationServiceClient;
import com.dissertation.orderservice.client.PaymentServiceClient;
import com.dissertation.orderservice.client.UserServiceClient;
import com.dissertation.orderservice.dto.*;
import com.dissertation.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @MockBean
    private UserServiceClient userServiceClient;
    
    @MockBean
    private PaymentServiceClient paymentServiceClient;
    
    @MockBean
    private NotificationServiceClient notificationServiceClient;
    
    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        
        UserResponse mockUser = new UserResponse();
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setActive(true);
        when(userServiceClient.getUserByUsername(any())).thenReturn(mockUser);
        
        PaymentResponse mockPayment = new PaymentResponse();
        mockPayment.setPaymentId("PAY-123");
        mockPayment.setStatus("COMPLETED");
        when(paymentServiceClient.processPayment(any())).thenReturn(mockPayment);
    }
    
    @Test
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUsername("testuser");
        request.setProductName("Laptop");
        request.setQuantity(1);
        request.setUnitPrice(BigDecimal.valueOf(999.99));
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.status").value("PAYMENT_COMPLETED"));
    }
    
    @Test
    void shouldGetOrdersByUsername() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUsername("testuser");
        request.setProductName("Laptop");
        request.setQuantity(1);
        request.setUnitPrice(BigDecimal.valueOf(999.99));
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        mockMvc.perform(get("/api/orders/user/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }
}

