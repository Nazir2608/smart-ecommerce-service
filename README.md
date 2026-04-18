# SmartCommerce Pro

> **AI-Powered Production-Ready E-Commerce Platform**
> Phase 1 — Authentication & User Management

[![CI Pipeline](https://github.com/nazir/smart-ecommerce-service/actions/workflows/ci.yml/badge.svg)](https://github.com/nazir/smart-ecommerce-service/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [1 · Clone & configure](#1--clone--configure)
  - [2 · Start infrastructure](#2--start-infrastructure)
  - [3 · Run the application](#3--run-the-application)
  - [4 · Verify](#4--verify)
- [API Reference — Phase 1](#api-reference--phase-1)
  - [Authentication](#authentication-endpoints)
  - [Users & Addresses](#users--addresses-endpoints)
- [Security Design](#security-design)
- [Configuration Reference](#configuration-reference)
- [Running Tests](#running-tests)
- [Docker](#docker)
- [CI/CD](#cicd)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Overview

**SmartCommerce Pro** is a modular-monolith e-commerce backend built to production standards. Each feature area lives in its own bounded package (module) with strict dependency rules, making it straightforward to extract individual services later without a full rewrite.

**Phase 1** delivers a complete, production-grade authentication and user management system:

| Feature | Status |
|---|---|
| Email/password registration & login | ✅ |
| JWT access token (15 min) + refresh token (7 days) | ✅ |
| Refresh token rotation with reuse detection | ✅ |
| Logout — token revocation | ✅ |
| Forgot/reset password via Redis-backed 6-digit OTP | ✅ |
| Google OAuth2 login | ✅ |
| GitHub OAuth2 login | ✅ |
| User profile: read + update | ✅ |
| Delivery address CRUD (max 10, auto-default logic) | ✅ |
| BCrypt password hashing (cost 12) | ✅ |
| Rate limiting on OTP requests (5/hour per email) | ✅ |
| Flyway database migrations | ✅ |
| Swagger / OpenAPI 3 docs | ✅ |
| Structured logging (SLF4J) | ✅ |
| Health & metrics endpoints (Actuator + Prometheus) | ✅ |
| Docker Compose local dev stack (+ Mailhog, pgAdmin) | ✅ |
| GitHub Actions CI (build, test, OWASP, Docker push) | ✅ |

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 (virtual threads) |
| Framework | Spring Boot | 3.3.x |
| Security | Spring Security + JWT (jjwt) + OAuth2 | 6.x / 0.12.6 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | 10.x |
| Cache / OTP | Redis | 7.2 |
| Email | Spring Mail + Mailhog (dev) | — |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.6.x |
| Build | Maven | 3.9.x |
| Container | Docker + Docker Compose | — |
| CI/CD | GitHub Actions | — |

---

## Project Structure

```
smart-ecommerce-service/
├── .github/workflows/
│   └── ci.yml                        # Build · test · OWASP · Docker push
├── docker/
│   ├── Dockerfile                    # Multi-stage production image
│   └── docker-compose.yml            # Full local dev stack
│
└── src/main/java/com/nazir/ecommerce/
    ├── SmartCommerceApplication.java  # Entry point
    │
    ├── auth/                          # Bounded module: Authentication
    │   ├── controller/AuthController.java
    │   ├── dto/                       # Request & response DTOs
    │   ├── model/RefreshToken.java    # JPA entity
    │   ├── repository/
    │   └── service/
    │       ├── AuthService.java       # Core business logic
    │       └── OtpService.java        # Redis-backed OTP
    │
    ├── user/                          # Bounded module: Users & Addresses
    │   ├── controller/
    │   │   ├── UserController.java
    │   │   └── AddressController.java
    │   ├── dto/
    │   ├── model/
    │   │   ├── User.java
    │   │   └── Address.java
    │   ├── repository/
    │   └── service/
    │       ├── UserService.java
    │       └── AddressService.java
    │
    ├── common/                        # Shared across all modules
    │   ├── dto/ApiResponse.java       # Unified response envelope
    │   ├── dto/ErrorResponse.java
    │   ├── enums/
    │   └── exception/
    │       ├── GlobalExceptionHandler.java
    │       ├── ResourceNotFoundException.java
    │       ├── BusinessException.java
    │       └── AuthException.java
    │
    └── infrastructure/
        ├── config/
        │   ├── SecurityConfig.java    # Filter chain, CORS, OAuth2
        │   ├── RedisConfig.java
        │   └── SwaggerConfig.java
        ├── email/EmailService.java    # HTML email sender
        └── security/
            ├── JwtService.java        # Token generation & parsing
            ├── JwtAuthenticationFilter.java
            ├── UserPrincipal.java     # UserDetails wrapper
            ├── CustomUserDetailsService.java
            └── OAuth2LoginSuccessHandler.java
```

---

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Docker + Docker Compose | Latest |
| (Optional) IntelliJ IDEA / VS Code | — |

---

### 1 · Clone & configure

```bash
git clone https://github.com/nazir/smart-ecommerce-service.git
cd smart-ecommerce-service

# Copy and fill in environment variables
cp .env.example .env
```

Minimum required changes in `.env`:

```dotenv
DB_PASSWORD=your_strong_password
JWT_SECRET=$(openssl rand -base64 64)   # run this in your shell

# For Google OAuth2 (optional in dev):
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=xxx
```

---

### 2 · Start infrastructure

```bash
# Start PostgreSQL, Redis, and Mailhog
cd docker
docker compose up -d postgres redis mailhog

# To also open pgAdmin and Redis Commander:
docker compose --profile tools up -d
```

| Service | URL |
|---|---|
| PostgreSQL | `localhost:5432` / db: `smartcommerce` |
| Redis | `localhost:6379` |
| Mailhog (email UI) | http://localhost:8025 |
| pgAdmin | http://localhost:5050 (profile: tools) |
| Redis Commander | http://localhost:8081 (profile: tools) |

---

### 3 · Run the application

**Option A — Maven (development)**
```bash
# From the project root
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Option B — Docker (full stack)**
```bash
cd docker
docker compose up -d
```

**Option C — JAR**
```bash
mvn package -DskipTests
java -jar target/smart-ecommerce-service-*.jar --spring.profiles.active=dev
```

---

### 4 · Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected output:
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},...}}
```

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## API Reference — Phase 1

All endpoints return the following envelope:

```json
// Success
{
  "success": true,
  "data": { ... },
  "message": "OK",
  "timestamp": "2024-01-01T10:00:00Z"
}

// Error
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": { "email": "Must be a valid email address" },
  "timestamp": "2024-01-01T10:00:00Z"
}
```

---

### Authentication Endpoints

#### `POST /api/v1/auth/register`

Register a new BUYER account. Returns a token pair immediately.

**Request**
```json
{
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "password": "Password1"
}
```

**Validation rules**
- `fullName` — 2–255 characters
- `email` — valid RFC email format
- `password` — 8–100 chars, must contain uppercase, lowercase, and digit

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

---

#### `POST /api/v1/auth/login`

```json
// Request
{ "email": "alice@example.com", "password": "Password1" }

// Response 200
{ "data": { "accessToken": "...", "refreshToken": "...", "expiresIn": 900 } }
```

---

#### `POST /api/v1/auth/refresh`

Rotates the token pair. The old refresh token is revoked.

```json
// Request
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }

// Response 200 — new pair
{ "data": { "accessToken": "...", "refreshToken": "new-uuid", "expiresIn": 900 } }
```

> ⚠️ Submitting an already-used refresh token triggers **reuse detection**: all sessions for that user are immediately revoked.

---

#### `POST /api/v1/auth/logout`

```json
// Request
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }

// Response 200
{ "message": "Logged out successfully" }
```

---

#### `POST /api/v1/auth/forgot-password`

Sends a 6-digit OTP to the email. Always returns `200` to prevent user enumeration.

```json
// Request
{ "email": "alice@example.com" }

// Response 200
{ "message": "If an account exists for this email, an OTP has been sent" }
```

**Rate limit:** 5 requests per email per hour (enforced in Redis).

---

#### `POST /api/v1/auth/reset-password`

```json
// Request
{
  "email": "alice@example.com",
  "otp": "482931",
  "newPassword": "NewPassword1"
}

// Response 200
{ "message": "Password reset successfully. Please log in with your new password." }
```

---

#### `GET /api/v1/auth/oauth2/google`

Redirect the browser to `/oauth2/authorization/google`. After consent, Google redirects to the configured callback, the server issues tokens and forwards them to:

```
{OAUTH2_REDIRECT_URI}?accessToken=...&refreshToken=...
```

---

### Users & Addresses Endpoints

All require `Authorization: Bearer <accessToken>`.

#### `GET /api/v1/users/me`

```json
// Response 200
{
  "data": {
    "id": "uuid",
    "email": "alice@example.com",
    "fullName": "Alice Smith",
    "avatarUrl": null,
    "phoneNumber": null,
    "role": "BUYER",
    "oauthProvider": "LOCAL",
    "emailVerified": true,
    "createdAt": "2024-01-01T10:00:00Z"
  }
}
```

---

#### `PUT /api/v1/users/me`

All fields optional — only provided fields are updated.

```json
// Request
{
  "fullName": "Alice Johnson",
  "phoneNumber": "+919876543210",
  "avatarUrl": "https://cdn.example.com/avatars/alice.png"
}
```

---

#### `GET /api/v1/users/me/addresses`

```json
// Response 200
{
  "data": [
    {
      "id": "uuid",
      "label": "HOME",
      "fullName": "Alice Smith",
      "phoneNumber": "+919876543210",
      "line1": "42 MG Road",
      "line2": "Apt 3B",
      "city": "Bengaluru",
      "state": "Karnataka",
      "postalCode": "560001",
      "country": "IN",
      "isDefault": true,
      "createdAt": "2024-01-01T10:00:00Z"
    }
  ]
}
```

---

#### `POST /api/v1/users/me/addresses`

```json
// Request
{
  "label": "WORK",
  "fullName": "Alice Smith",
  "phoneNumber": "+919876543210",
  "line1": "100 Tech Park",
  "city": "Bengaluru",
  "state": "Karnataka",
  "postalCode": "560103",
  "country": "IN",
  "isDefault": false
}
```

**Rules:**
- Maximum **10** addresses per user.
- The **first** address is always set as default.
- Setting `isDefault: true` clears the previous default automatically.

---

#### `PUT /api/v1/users/me/addresses/{id}`

Same body as POST. Returns the updated address.

---

#### `DELETE /api/v1/users/me/addresses/{id}`

Returns `200`. If the deleted address was the default, the oldest remaining address is automatically promoted.

---

## Security Design

```
Request
  └─► JwtAuthenticationFilter
        ├── Extract Bearer token from Authorization header
        ├── Parse + validate JWT signature
        ├── Load UserDetails from DB (by email claim)
        ├── Set SecurityContext with UserPrincipal
        └─► Controller
```

| Token | Storage | Expiry | Rotation |
|---|---|---|---|
| Access token | Client memory / Authorization header | 15 min | New on each refresh |
| Refresh token | PostgreSQL `refresh_tokens` table | 7 days | Rotated on every use |

**Reuse detection:** If a revoked refresh token is submitted, all active sessions for that user are immediately revoked (silent re-authentication attack mitigation).

**Password reset OTP:**
- 6-digit, cryptographically random (`SecureRandom`)
- Stored in Redis with 10-minute TTL
- Single-use: deleted after successful verification
- Rate limited: 5 requests per email per hour

---

## Configuration Reference

All properties can be set via environment variables (see `.env.example`):

| Property | Env Var | Default | Description |
|---|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | *(required)* | 256-bit HMAC signing key |
| `app.jwt.access-token-expiry-ms` | `JWT_ACCESS_EXPIRY_MS` | `900000` | Access token TTL (ms) |
| `app.jwt.refresh-token-expiry-ms` | `JWT_REFRESH_EXPIRY_MS` | `604800000` | Refresh token TTL (ms) |
| `app.otp.expiry-minutes` | `OTP_EXPIRY_MINUTES` | `10` | OTP TTL in minutes |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated origins |
| `app.oauth2.redirect-uri` | `OAUTH2_REDIRECT_URI` | `http://localhost:3000/oauth2/callback` | Frontend callback URL |
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/smartcommerce` | PostgreSQL JDBC URL |

---

## Running Tests

```bash
# Unit + slice tests only (no Docker required)
mvn test

# Full integration tests (requires running PostgreSQL + Redis)
mvn verify -Dspring.profiles.active=test

# With coverage report
mvn verify jacoco:report
open target/site/jacoco/index.html
```

**Test coverage targets (Phase 1):**

| Module | Min coverage |
|---|---|
| `auth` | 80% |
| `user` | 75% |
| `infrastructure/security` | 70% |

---

## Docker

```bash
# Build production image
docker build -f docker/Dockerfile -t smart-ecommerce:latest .

# Start full local dev stack
cd docker && docker compose up -d

# View application logs
docker compose logs -f app

# Stop all services
docker compose down

# Stop and remove volumes (clean slate)
docker compose down -v
```

**Services in docker-compose.yml:**

| Container | Port | Notes |
|---|---|---|
| `sc-app` | `8080` | Spring Boot application |
| `sc-postgres` | `5432` | PostgreSQL 16 |
| `sc-redis` | `6379` | Redis 7.2 |
| `sc-mailhog` | `1025` / `8025` | SMTP trap + Web UI |
| `sc-pgadmin` *(tools)* | `5050` | PostgreSQL GUI |
| `sc-redis-ui` *(tools)* | `8081` | Redis Commander GUI |

---

## CI/CD

The GitHub Actions pipeline runs on every push and pull request:

```
push / PR
  └─► build-and-test
        ├── Start PostgreSQL + Redis services
        ├── mvn verify (compile + test + JaCoCo)
        ├── Upload test reports
        └── Upload coverage to Codecov
  └─► code-quality
        └── OWASP Dependency-Check (continues on error)
  └─► docker-build  ← only on push to main
        ├── Login to GitHub Container Registry (ghcr.io)
        ├── Build multi-stage Docker image
        └── Push with sha- and branch tags
```

Required secrets in your GitHub repository:

| Secret | Description |
|---|---|
| `CODECOV_TOKEN` | Codecov upload token (optional) |
| Automatic `GITHUB_TOKEN` | Used for GHCR push — no manual setup |

---

## Roadmap

| Phase | Features | Status |
|---|---|---|
| **Phase 1** | Auth + Users + Addresses | ✅ Complete |
| **Phase 2** | Products + Categories + Cart + Checkout + Orders + Stripe/Razorpay | 🔜 Next |
| **Phase 3** | Elasticsearch search + Redis caching + Apache Kafka events | Planned |
| **Phase 4** | Spring AI: chatbot, recommendations, semantic search (Gemini/GPT-4o/Ollama) | Planned |
| **Phase 5** | WebFlux SSE real-time notifications | Planned |
| **Phase 6** | Prometheus + Grafana dashboards + production hardening | Planned |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Follow the existing package structure and naming conventions
4. Add or update tests for any changed behaviour
5. Run `mvn verify` and ensure all tests pass before submitting a PR
6. Open a pull request against `develop`

---

## License

MIT — see [LICENSE](LICENSE)

---

*SmartCommerce Pro · Phase 1 · Spring Boot 3.3 · Java 21*
