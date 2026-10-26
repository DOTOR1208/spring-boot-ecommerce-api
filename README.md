# 🛒 E-Commerce RESTful API Backend

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-green.svg)](https://spring.io/projects/spring-security)
[![JWT](https://img.shields.io/badge/JWT-JJWT%200.12.6-blue.svg)](https://github.com/jwtk/jjwt)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-yellow.svg)](https://mapstruct.org/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

A robust, enterprise-grade e-commerce backend built with **Spring Boot 3**, **Spring Security 6**, and **MySQL**. Designed adhering to **Clean Architecture**, **SOLID principles**, and pure **RESTful API standards**, featuring stateless JWT authentication, role-based access control (RBAC), and transactional order processing.

---

## 🌟 Key Highlights & Architecture

- **Clean Layered Architecture**: Strict separation of concerns across `Controller`, `Service`, `Repository`, and `Model` layers.
- **DTO-First Pattern & Zero Entity Leakage**: All API contracts communicate strictly via dedicated Request/Response DTOs mapped with **MapStruct 1.6.3** at compile-time for optimal runtime performance.
- **Stateless JWT & RBAC**: Fully stateless authentication powered by **Spring Security 6** and **JJWT 0.12.6**. Fine-grained endpoint authorization via method-level `@PreAuthorize` (`ROLE_USER`, `ROLE_ADMIN`).
- **Transactional Order Checkout**: Atomically validates inventory, decrements product stock, snapshots unit price at checkout time (`priceAtOrderTime`), and clears the user's active cart in a single `@Transactional` boundary.
- **Standardized API Envelope**: Unified response schema across all endpoints:
  - `SuccessResponse<T>` with automated pagination metadata (`size`, `page`, `totalPages`, `totalElements`).
  - `ErrorResponse` with RFC 7807 compliant structure, domain categorization, and field-level validation details.
- **Centralized Exception Handling**: Hierarchical `BaseException` caught by `@RestControllerAdvice` (`GlobalExceptionHandler`) to ensure consistent, secure error payloads without exposing internal stack traces.
- **Soft Deletion & Data Audit**: Soft delete mechanism implemented on critical resources (`User`, `Product`) to preserve relational integrity with historical orders.

---

## 🛠️ Tech Stack

| Category | Technologies |
| :--- | :--- |
| **Language & Framework** | Java 17, Spring Boot 3.4.5 |
| **Web & Security** | Spring Web MVC, Spring Security 6, JJWT 0.12.6, BCrypt |
| **Persistence & Database** | Spring Data JPA (Hibernate 6), MySQL 8.0, HikariCP |
| **Object Mapping & Utilities**| MapStruct 1.6.3, Project Lombok |
| **Build & Tooling** | Apache Maven 3.9+ (Maven Wrapper included) |

---

## 📂 Project Structure

```text
src/main/java/com/ecommerce/
├── controller/         # REST Controllers handling HTTP requests & response wrapping
│   ├── AuthController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── ProductController.java
│   └── UserController.java
├── service/            # Business logic interfaces & transactional implementations
│   ├── AuthService.java
│   ├── CartService.java
│   ├── OrderService.java
│   ├── ProductService.java
│   ├── UserService.java
│   └── impl/
├── repository/         # Spring Data JPA repositories with derived & custom queries
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── ProductRepository.java
│   ├── RoleRepository.java
│   └── UserRepository.java
├── model/              # JPA Entities & relational mapping
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Orders.java
│   ├── OrderItem.java
│   ├── Product.java
│   ├── Role.java
│   ├── User.java
│   └── enums/
├── dto/                # Data Transfer Objects
│   ├── request/        # Validated input payloads (@Valid, @NotBlank, etc.)
│   └── response/       # Client-facing API response objects & envelopes
├── mapper/             # Compile-time MapStruct mapper interfaces
│   ├── CartMapper.java
│   ├── OrderMapper.java
│   ├── ProductMapper.java
│   └── UserMapper.java
├── security/           # Spring Security filter chain, JWT filter & custom handlers
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── CustomAuthenticationEntryPoint.java
│   ├── CustomAccessDeniedHandler.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfiguration.java
├── exception/          # Custom business exceptions & GlobalExceptionHandler
│   ├── BaseException.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── GlobalExceptionHandler.java
└── utils/              # JWT token generation, parsing & validation utilities
    └── JwtUtils.java
```

---

## 🔌 RESTful API Endpoints

### 🔐 Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | Public | Register a new user account with default `ROLE_USER` |
| `POST` | `/api/auth/signin` | Public | Authenticate user credentials and return JWT Bearer token |

### 🛍️ Product Catalog (`/api/products`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | Public | List products (supports pagination `page`, `size`, `sort` or `unpaged=true`) |
| `GET` | `/api/products/{id}` | Public | Get product details by ID |
| `GET` | `/api/products/search?name=...` | Public | Case-insensitive product search with pagination |
| `POST` | `/api/products` | `ADMIN` | Create a new product |
| `PUT` | `/api/products/{id}` | `ADMIN` | Update an existing product |
| `DELETE` | `/api/products/{id}` | `ADMIN` | Soft delete a product |

### 🛒 Shopping Cart (`/api/cart`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/cart` | `USER` / `ADMIN` | Retrieve authenticated user's cart and item totals |
| `POST` | `/api/cart/items` | `USER` / `ADMIN` | Add item to cart or increment quantity |
| `DELETE` | `/api/cart/items/{productId}` | `USER` / `ADMIN` | Remove a product from the cart |
| `DELETE` | `/api/cart` | `USER` / `ADMIN` | Empty all items from the cart |

### 📦 Orders (`/api/orders`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/orders/checkout` | `USER` / `ADMIN` | Place order from current cart (validates stock, creates order, clears cart) |
| `GET` | `/api/orders` | `USER` / `ADMIN` | View order history summaries for the authenticated user |
| `GET` | `/api/orders/{orderId}` | `USER` / `ADMIN` | View detailed order breakdown with item snapshot prices |

### 👤 User Management (`/api/users`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | `USER` / `ADMIN` | View current user's profile |
| `GET` | `/api/users` | `ADMIN` | List all registered users (paginated) |
| `GET` | `/api/users/{id}` | `ADMIN` | Retrieve user profile by ID |
| `PUT` | `/api/users/{id}` | `USER` / `ADMIN` | Update profile information (Self or Admin) |
| `DELETE` | `/api/users/{id}` | `ADMIN` | Soft delete user account |
| `GET` | `/api/users/search?keyword=...`| `ADMIN` | Search users by keyword (username, email, phone) |

---

## 📦 API Response Schema

### Standard Success Response
```json
{
  "timestamp": "2026-10-26T20:00:00Z",
  "status": 200,
  "data": {
    "id": 1,
    "productName": "Wireless Mechanical Keyboard",
    "price": 89.99,
    "stock": 45
  },
  "pagination": null
}
```

### Paginated Success Response
```json
{
  "timestamp": "2026-10-26T20:00:00Z",
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

### Standard Error Response
```json
{
  "timestamp": "2026-10-26T20:00:00Z",
  "status": 404,
  "error": "Not Found",
  "code": "RESOURCE_NOT_FOUND",
  "domain": "product",
  "message": "Product not found with id: 99",
  "details": null
}
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or higher
- **MySQL 8.0+**
- **Git**

### Installation & Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/DOTOR1208/spring-boot-ecommerce-api.git
   cd spring-boot-ecommerce-api
   ```

2. **Configure Database**:
   Update `src/main/resources/application.properties` with your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Build & Run**:
   - **Linux / macOS**:
     ```bash
     ./mvnw clean spring-boot:run
     ```
   - **Windows**:
     ```powershell
     .\mvnw.cmd clean spring-boot:run
     ```

4. **Verify Application**:
   The server will start at: `http://localhost:8081`

---

## 🧪 Running Tests

Execute automated unit and integration tests using Maven:
```powershell
.\mvnw.cmd test
```

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
