# Spring Boot E-Commerce REST API

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.0-green.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-MySQL%20Docker-blueviolet.svg?style=flat-square&logo=testcontainers)](https://testcontainers.com/)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF.svg?style=flat-square&logo=githubactions)](https://github.com/features/actions)

A RESTful backend service for an e-commerce platform built with Spring Boot 3, Spring Security 6, and MySQL 8.0. The system provides stateless JWT authentication, Role-Based Access Control (RBAC), Anti-IDOR authorization checks, compile-time DTO mappings via MapStruct, and atomic transactional checkout with inventory validation.

---

## Tech Stack

| Category | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java (OpenJDK) | 17 LTS | Core programming language |
| **Framework** | Spring Boot | 3.4.5 | Web MVC, Data JPA, Security, Validation |
| **Database** | MySQL | 8.0 | Primary relational database |
| **Connection Pool** | HikariCP | Default | High-performance JDBC connection pooling |
| **Security** | Spring Security, JJWT | 6.0 / 0.12.6 | Stateless JWT authentication, BCrypt, RBAC |
| **Mapping** | MapStruct | 1.6.3 | Compile-time, type-safe entity-DTO mapper |
| **Testing** | JUnit 5, Mockito, AssertJ | Latest | Unit, security, and slice testing |
| **Integration Test** | Testcontainers | 1.20.6 | Real MySQL 8.0 Docker container integration |
| **Containerization** | Docker, Docker Compose | Multi-Stage | Zero-setup local container deployment |
| **CI/CD** | GitHub Actions | Workflows | Automated test pipeline on push and pull request |

---

## Testing Suite (188 Tests)

The test suite follows the Testing Pyramid methodology, covering database slice queries, domain services, security filter lifecycle, web controllers, and containerized integration flows.

| Test Type | Scope | Technology | Tests | Status |
| :--- | :--- | :--- | :---: | :---: |
| **Repository Slice Tests** | Custom derived queries, entity mapping, soft-delete filtering, pagination | Spring Data JPA, H2, AssertJ | **50** | Passed |
| **Service Unit Tests** | Business rules, stock checks, cart calculations, exception propagation | JUnit 5, Mockito | **63** | Passed |
| **Security & Utility Tests** | HMAC-SHA512 signing, expiration handling, signature tampering, BCrypt hashing | JJWT 0.12.6, Spring Security Crypto | **21** | Passed |
| **Controller Slice Tests** | HTTP payload binding, `@Valid` constraints, status codes, `@PreAuthorize` | `@WebMvcTest`, MockMvc | **39** | Passed |
| **Full Integration Tests** | End-to-end user journeys against MySQL 8.0 (Auth, Catalog, Cart, Checkout) | `@SpringBootTest`, Testcontainers | **15** | Passed |
| **TOTAL** | **Full Project Test Suite** | | **188** | **Passed** |

### Testcontainers Singleton Container Pattern
Integration tests share a single container lifecycle to minimize startup overhead:
- A singleton `mysql:8.0.36` container is managed statically in `BaseIntegrationTest`.
- Shared across integration test classes (`AuthIntegrationTest`, `ProductCatalogIntegrationTest`, `OrderCheckoutIntegrationTest`) using dynamic port mapping via `@DynamicPropertySource`.
- Configured via `docker-java.properties` (`api.version=1.44`) to support Docker Desktop on Windows and Linux CI runners.

### Test Execution Commands
```powershell
# Run all 188 tests
.\mvnw.cmd test

# Run only Full Integration tests
.\mvnw.cmd test "-Dtest=*IntegrationTest"

# Run only Service Unit tests
.\mvnw.cmd test "-Dtest=*ServiceImplTest"

# Run only Repository Slice tests
.\mvnw.cmd test "-Dtest=*RepositoryTest"
```

---

## Core Engineering & Security Features

### 1. Stateless Authentication & Role-Based Access Control (RBAC)
- Stateless authentication using JWT signed with HMAC-SHA512.
- Role-based authorization enforced via method-level annotations (`@PreAuthorize("hasRole('ADMIN')")`) and URL security matchers.
- Public read access for product catalog endpoints without requiring authentication.

### 2. Anti-IDOR Authorization
- Enforces strict resource ownership verification on order endpoints (`/api/orders/{id}`).
- Regular users cannot inspect or access orders belonging to other accounts.
- Administrators retain elevated privileges to inspect all orders for auditing.

### 3. Atomic Checkout & Inventory Validation
The checkout workflow executes within a single `@Transactional` method (`OrderServiceImpl.checkout`):
- **Stock Check**: Validates requested cart quantities against live inventory.
- **Stock Decrement**: Deducts product stock in the database.
- **Price Snapshot**: Saves unit prices at purchase time into `OrderItem.priceAtOrderTime`, isolating historical orders from subsequent price changes.
- **Cart Cleanup**: Flushes active cart items upon order completion.
- **Rollback Guarantee**: In case of insufficient stock or system errors, the transaction rolls back completely—no partial records are created.

### 4. DTO-First Architecture with MapStruct
- Prevents entity leakage and circular reference serialization issues.
- MapStruct generates type-safe object mappers at compile time, eliminating reflection overhead.

### 5. Soft Delete & Relational Integrity
- Primary domain entities (`User`, `Product`) utilize soft deletion (`deleted = true`) to prevent foreign key violations in completed order histories.

### 6. Standardized Response Envelope
All controller endpoints return a standardized envelope for consistency across consumers:
- Success: `SuccessResponse<T>` with optional pagination metadata.
- Error: `ErrorResponse` handled via `@RestControllerAdvice` (`GlobalExceptionHandler`).

---

## Domain Model & Entity Relationships

- **User & Role**: Many-to-Many association (`users_roles`). Users hold encrypted credentials, account status flags, and an auto-initialized shopping cart.
- **Product**: Catalog item with attributes including name, category, price, available stock, and soft-delete status.
- **Cart & CartItem**: One-to-One mapping between User and Cart. Cart items reference products with requested quantities, verified against real-time inventory.
- **Orders & OrderItem**: One-to-Many relation representing finalised purchases. Each `OrderItem` captures the snapshot unit price (`priceAtOrderTime`) and quantity.

---

## Project Structure

```text
src/
├── main/java/com/ecommerce/
│   ├── constant/           # Constants and SecurityRole definitions
│   ├── controller/         # REST Controllers (Auth, Product, Cart, Order, User)
│   ├── dto/                # Request & Response DTO records and classes
│   │   ├── request/
│   │   └── response/
│   ├── exception/          # Custom business exceptions & GlobalExceptionHandler
│   ├── mapper/             # MapStruct compile-time mappers
│   ├── model/              # JPA domain entities (User, Product, Cart, Orders, etc.)
│   ├── repository/         # Spring Data JPA repositories with derived queries
│   ├── envelope/           # Unified API response envelope wrappers (SuccessResponse, ErrorResponse)
│   ├── security/           # Filter chain, CustomUserDetails, JWT filter, Entry points
│   ├── service/            # Business interfaces & transactional implementations
│   │   └── impl/
│   └── utils/              # JWT signing, claims extraction, and validation helpers
└── test/java/com/ecommerce/
    ├── controller/         # WebMvc slice tests (MockMvc)
    ├── integration/        # Full integration tests (Testcontainers MySQL)
    ├── repository/         # DataJpaTest slice tests (H2)
    ├── security/           # Security & authorization tests
    ├── service/            # Service unit tests (Mockito)
    └── utils/              # JWT & crypto utility tests
```

---

## API Endpoints Reference

### Authentication (`/api/auth`)
| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | Public | Register a new user account (`ROLE_USER`) |
| `POST` | `/api/auth/signin` | Public | Authenticate credentials and return JWT token |

### Products (`/api/products`)
| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/products` | Public | List products (paginated, or `unpaged=true`) |
| `GET` | `/api/products/{id}` | Public | Get product details by ID |
| `GET` | `/api/products/search?name=...` | Public | Search products by name (paginated) |
| `POST` | `/api/products` | Admin | Create a new product |
| `PUT` | `/api/products/{id}` | Admin | Update product details |
| `DELETE` | `/api/products/{id}` | Admin | Soft delete a product |

### Cart (`/api/cart`)
| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/cart` | User | Get current user's shopping cart |
| `POST` | `/api/cart/items` | User | Add item to cart or increment quantity |
| `PUT` | `/api/cart/items/{productId}` | User | Update item quantity (removes if quantity <= 0) |
| `DELETE` | `/api/cart/items/{productId}` | User | Remove an item from the cart |
| `DELETE` | `/api/cart` | User | Clear entire shopping cart |

### Orders (`/api/orders`)
| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/orders/checkout` | User | Execute atomic checkout on active cart |
| `GET` | `/api/orders` | User | View order history for authenticated user |
| `GET` | `/api/orders/{orderId}` | User / Admin | View order details (Owner or Admin only) |

### Users (`/api/users`)
| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/me` | User | View current authenticated user profile |
| `PUT` | `/api/users/me` | User | Update current authenticated user profile |
| `PUT` | `/api/users/change-password` | User | Change user password (validates current password) |
| `GET` | `/api/users` | Admin | List all registered users (paginated) |
| `GET` | `/api/users/{id}` | Admin | Get user profile by ID |
| `PUT` | `/api/users/{id}` | Admin | Update user details |
| `DELETE` | `/api/users/{id}` | Admin | Soft delete a user account |
| `PUT` | `/api/users/{id}/restore` | Admin | Restore a soft-deleted user account |
| `GET` | `/api/users/search?keyword=...` | Admin | Search users by keyword (paginated) |

---

## Response Format

### Success Response (`200 OK`)
```json
{
  "timestamp": "2026-11-19T19:30:00Z",
  "operationType": "SUCCESS",
  "data": {
    "orderId": 1,
    "totalPrice": 300.0,
    "status": "COMPLETED"
  },
  "pagination": null
}
```

### Paginated Response (`200 OK`)
```json
{
  "timestamp": "2026-11-19T19:30:00Z",
  "operationType": "SUCCESS",
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

### Error Response (`400 Bad Request` / `403 Forbidden` / `404 Not Found`)
```json
{
  "timestamp": "2026-11-19T19:30:00Z",
  "operationType": "FAILURE",
  "code": "RESOURCE_NOT_FOUND",
  "domain": "product",
  "message": "Product not found with id: 99",
  "details": null
}
```

---

## Getting Started

### Prerequisites
- JDK 17 or higher
- Docker Desktop (for Docker Compose and Testcontainers)
- MySQL 8.0+ (if running without Docker)

### Option 1: Docker Compose (Multi-Stage Build — Recommended)
```powershell
# Build and start all services
docker compose up --build -d

# Check container status
docker compose ps

# View application logs
docker compose logs -f app

# Stop containers
docker compose down
```
The API will be available at `http://localhost:8081`.

### Option 2: Run Locally with Maven
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

---

## CI/CD Pipeline
Continuous Integration is configured via **GitHub Actions** (`.github/workflows/maven.yml`):
- Triggers on pull requests and pushes to `main`.
- Environment: `ubuntu-latest` with Temurin JDK 17 and Maven caching.
- Executes full test suite (`mvn clean test -B`), with automated Testcontainers provisioning on the runner.
