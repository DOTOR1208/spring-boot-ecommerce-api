# Spring Boot E-Commerce REST API

A RESTful backend service for an e-commerce platform built with Spring Boot 3, Spring Security 6, and MySQL. It handles user authentication with JWT, product catalog management, shopping cart operations, and transactional order checkout with inventory validation.

---

## Tech Stack

- **Framework**: Spring Boot 3.4.5 (Spring MVC, Spring Data JPA, Spring Security, Validation)
- **Language**: Java 17
- **Database**: MySQL 8.0 with HikariCP connection pooling
- **Security**: Spring Security 6, JJWT 0.12.6, BCrypt
- **Mapping & Utilities**: MapStruct 1.6.3, Project Lombok
- **Build Tool**: Maven Wrapper (`mvnw`)

---

## Key Features & Design Decisions

### 1. Authentication & Role-Based Access Control (RBAC)
- **Stateless Authentication**: Uses JWT (JSON Web Tokens) with HMAC-SHA512 signature. The filter chain (`JwtAuthenticationFilter`) validates the token on each request and populates the `SecurityContext`.
- **Role Enforcement**: Protected routes check permissions via `@PreAuthorize("hasRole('ADMIN')")` or SecurityFilterChain URL matching (`/api/auth/**` and `GET /api/products/**` are public).
- **Graceful Error Handling**: Custom `AuthenticationEntryPoint` and `AccessDeniedHandler` return standardized JSON error envelopes instead of default HTML error pages.

### 2. DTO-First Architecture
- JPA entities are never exposed directly to controllers. All client requests and responses use dedicated DTOs (`request/` and `response/`).
- **MapStruct** generates type-safe object mappings at compile-time, avoiding reflection overhead while preventing entity leakage and lazy loading serialization issues.

### 3. Shopping Cart & Inventory Management
- Carts are tied to individual user accounts.
- Adding existing items to the cart automatically increments item quantities.
- Stock availability is verified prior to cart additions.

### 4. Transactional Order Checkout
- Order checkout is handled in a single `@Transactional` method (`OrderServiceImpl.checkout`):
  - Validates cart contents and checks live product stock.
  - Decrements product inventory atomically.
  - Snapshots current unit prices into `OrderItem.priceAtOrderTime` so historical orders remain unaffected by future product price changes.
  - Clears the user's cart upon successful order creation.

### 5. Soft Delete & Relational Integrity
- Critical domain entities (`User`, `Product`) use soft deletion (`deleted` flag) to prevent broken foreign key references in historical order records.

### 6. Unified Response Envelope & Exception Handling
- All endpoints wrap responses in a standard `SuccessResponse<T>` format, including pagination metadata (`page`, `size`, `totalElements`, `totalPages`) for paged queries.
- Business and validation errors are handled by `@RestControllerAdvice` (`GlobalExceptionHandler`), returning consistent `ErrorResponse` objects with domain context and field validation details.

---

## Project Structure

```text
src/main/java/com/ecommerce/
├── controller/         # REST endpoints (Auth, Product, Cart, Order, User)
├── service/            # Business interfaces & transactional implementations
│   └── impl/
├── repository/         # Spring Data JPA repositories with derived queries
├── model/              # JPA Entities (User, Role, Product, Cart, CartItem, Orders, OrderItem)
├── dto/                # Request validation payloads and Response representations
│   ├── request/
│   └── response/
├── mapper/             # MapStruct compile-time mappers
├── security/           # Filter chain, JWT provider, UserDetailsService, auth entry points
├── exception/          # Custom exceptions (ResourceNotFound, BadRequest) & GlobalExceptionHandler
└── utils/              # JWT signing, claims extraction, and validation helpers
```

---

## API Endpoints Reference

### Authentication (`/api/auth`)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | Public | Register a new user (`ROLE_USER`) |
| `POST` | `/api/auth/signin` | Public | Authenticate credentials and receive a JWT token |

### Products (`/api/products`)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | Public | List products (paginated, or `unpaged=true`) |
| `GET` | `/api/products/{id}` | Public | Get product details by ID |
| `GET` | `/api/products/search?name=...` | Public | Search products by name (paginated) |
| `POST` | `/api/products` | Admin | Create a new product |
| `PUT` | `/api/products/{id}` | Admin | Update product information |
| `DELETE` | `/api/products/{id}` | Admin | Soft delete a product |

### Cart (`/api/cart`)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/cart` | User | Get current user's shopping cart |
| `POST` | `/api/cart/items` | User | Add item to cart or increment quantity |
| `DELETE` | `/api/cart/items/{productId}` | User | Remove an item from the cart |
| `DELETE` | `/api/cart` | User | Clear entire cart |

### Orders (`/api/orders`)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/orders/checkout` | User | Checkout active cart and generate order |
| `GET` | `/api/orders` | User | View order history summaries for current user |
| `GET` | `/api/orders/{orderId}` | User / Admin | View order details (owner or admin only) |

### Users (`/api/users`)
| Method | Path | Auth | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | User | View current authenticated user's profile |
| `PUT` | `/api/users/me` | User | Update current authenticated user's profile |
| `GET` | `/api/users` | Admin | List all registered users (paginated) |
| `GET` | `/api/users/{id}` | Admin | Get user profile by ID |
| `PUT` | `/api/users/{id}` | Admin | Update user information |
| `DELETE` | `/api/users/{id}` | Admin | Soft delete user account |
| `GET` | `/api/users/search?keyword=...` | Admin | Search users by keyword |

---

## Response Format

### Success Response
```json
{
  "timestamp": "2026-10-27T15:00:00Z",
  "status": 200,
  "data": {
    "id": 1,
    "productName": "Mechanical Keyboard",
    "price": 89.99,
    "stock": 50
  },
  "pagination": null
}
```

### Paginated Response
```json
{
  "timestamp": "2026-10-27T15:00:00Z",
  "status": 200,
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

### Error Response
```json
{
  "timestamp": "2026-10-27T15:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "domain": "product",
  "message": "Product not found with id: 99",
  "details": null
}
```

---

## Local Setup

### Requirements
- **JDK 17** or higher
- **MySQL 8.0+**

### Configuration
Update the database connection details in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_password
server.port=8081
```

### Running the Application

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The application runs on `http://localhost:8081`.

### Running Tests

```powershell
.\mvnw.cmd test
```
