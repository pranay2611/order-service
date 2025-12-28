package com.dissertation.orderservice.client;

import com.dissertation.orderservice.dto.PaymentRequest;
import com.dissertation.orderservice.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${services.payment-service.url}")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}

