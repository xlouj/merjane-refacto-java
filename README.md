# Spring Boot Order Processing API

A Spring Boot REST API for processing orders and managing product inventory across different product types.

---

## Tech Stack

- Java 17
- Spring Boot 2.6.9
- Spring Data JPA
- Lombok
- JUnit 5 + Mockito

---

## Architecture

The project follows a standard layered architecture:
```
HTTP Request
     ↓
Controller    → handles HTTP routing and request/response
     ↓
Service       → business logic and transactions
     ↓
Repository    → database access via Spring Data JPA
     ↓
Database
```

---


---

## API Endpoints

### Process an Order
```
POST /orders/{orderId}/processOrder
```

**Path Variable:**
- `orderId` — ID of the order to process

**Response `200 OK`:**
```json
{
  "id": 1
}
```

**Response `404 Not Found`:**
```json
{
  "status": 404,
  "message": "Order not found: 1"
}
```

---

## Product Types

The API handles three product types defined in the `ProductType` enum:

### `NORMAL`

### `SEASONAL`

### `EXPIRABLE`
---

## Key Design Decisions

### Single Responsibility
Each layer has one job:
- `OrderController` — HTTP routing and delegation only
- `ProductService` — all business logic
- Repository interfaces — database access

### Constructor Injection
All dependencies are injected via constructor for immutability and testability:
```java
public OrderController(ProductService productService, OrderRepository orderRepository) {
    this.productService = productService;
    this.orderRepository = orderRepository;
}
```

### Transactions
`@Transactional` on `processProduct` ensures that if anything fails during product processing, all database changes for that product are rolled back:
```java
@Transactional
public void processProduct(Product p) { ... }
```

### ProductType Enum
Product types are defined as an enum to eliminate magic strings and prevent silent failures from typos:
```java
public enum ProductType {
    NORMAL, SEASONAL, EXPIRABLE
}
```

---

### Run the application
```bash
./mvnw spring-boot:run
```

### Run the tests
```bash
./mvnw test
```

---

## Testing Strategy

### Controller Tests (`@WebMvcTest`)
- Tests HTTP routing and response codes only
- All services and repositories are mocked with `@MockBean`
- Fast — only loads the web layer

### Service Tests (`@ExtendWith(MockitoExtension.class)`)
- Tests all business logic branches
- Repository and NotificationService are mocked with `@Mock`
- Covers every product type and edge case

---

## Refactoring Summary

| Area | Change |
|---|---|
| `OrderController` | Removed business logic, constructor injection, `orElseThrow` 404 |
| `ProductService` | Extracted handlers, dispatcher pattern, `@Transactional` |
| Product types | Replaced magic strings with `ProductType` enum |
| Tests | Split into controller tests and service tests with correct responsibilities |