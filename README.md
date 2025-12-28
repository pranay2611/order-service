# Order Service

Business logic microservice for order management with inter-service communication.

## Features
- Order creation with user validation
- Integration with Payment Service
- Integration with Notification Service
- Order status tracking
- Feign Client for service communication
- Comprehensive integration tests with mocked external services

## API Endpoints

### POST /api/orders
Create new order
```json
{
  "username": "john",
  "productName": "Laptop",
  "quantity": 1,
  "unitPrice": 999.99
}
```

### GET /api/orders/{orderNumber}
Get order details

### GET /api/orders/user/{username}
Get all orders for a user

### GET /api/orders
Get all orders

### PATCH /api/orders/{orderNumber}/status?status=SHIPPED
Update order status

## Dependencies
- User Service (port 8081)
- Payment Service (port 8083)
- Notification Service (port 8084)

## Run Locally
```bash
mvn spring-boot:run
```

## Run Tests
```bash
mvn test
```

Service runs on port 8082

