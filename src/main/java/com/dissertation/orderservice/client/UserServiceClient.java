package com.dissertation.orderservice.client;

import com.dissertation.orderservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    
    @GetMapping("/api/auth/user/{username}")
    UserResponse getUserByUsername(@PathVariable String username);
}

