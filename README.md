# Hyperapps Backend API

A comprehensive Spring Boot backend service for a restaurant delivery and management platform.

## Overview

Hyperapps Backend API powers a full-featured food delivery ecosystem, handling order management, payment processing, delivery coordination, and restaurant operations. The platform integrates with multiple third-party services to provide a seamless experience for customers, restaurant partners, and delivery personnel.

## Branch: security-implementation

This branch contains a comprehensive security implementation with the following features:

### Security Features

| Feature | Description | Status |
|---------|-------------|--------|
| JWT Authentication | Access & refresh token management with secure storage | ✅ Complete |
| API Key Authentication | Webhook authentication with STATIC_TOKEN & HMAC modes | ✅ Complete |
| Permission-Based RBAC | Granular permissions with role inheritance | ✅ Complete |
| Access Policies | URL-based policy system for fine-grained authorization | ✅ Complete |
| Rate Limiting | Token bucket algorithm (Bucket4j) for abuse prevention | ✅ Complete |
| Multi-Tenancy | Restaurant-scoped data isolation | ✅ Complete |

### Security Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────────┐
│               SECURITY FILTER CHAIN                      │
├─────────────────────────────────────────────────────────┤
│  1. RateLimitingFilter      → Rate limit check          │
│  2. ApiKeyAuthenticationFilter → Webhook auth           │
│  3. JwtAuthenticationFilter → JWT token validation      │
│  4. PolicyEnforcementFilter → Permission-based authz    │
└─────────────────────────────────────────────────────────┘
     │
     ▼
Controller → Service → Repository
```

### Default Roles

| Role | Description |
|------|-------------|
| PLATFORM_ADMIN | Super admin - bypasses all permission checks |
| PLATFORM_USER | Platform-level read access |
| RESTAURANT_ADMIN | Full restaurant management (inherits RESTAURANT_USER) |
| RESTAURANT_USER | Basic restaurant operations (inherits PUBLIC) |
| CUSTOMER | Customer actions - order, payment (inherits PUBLIC) |
| PUBLIC | Base role - menu read, location search, auth |
| POS_PARTNER | POS webhook callbacks |
| DELIVERY_PARTNER | Delivery webhook callbacks |
| PAYMENT_PARTNER | Payment webhook callbacks |

> 📖 For detailed security documentation, see [SECURITY_IMPLEMENTATION.md](./SECURITY_IMPLEMENTATION.md)

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.4 |
| Language | Java 17 |
| Database | MongoDB (Atlas) |
| Caching | Redis |
| Workflow Engine | Temporal |
| Build Tool | Maven |
| Containerization | Docker |

## External Integrations

- **Razorpay** - Payment gateway
- **Pidge** - Delivery aggregation platform
- **PetPooja** - Point of Sale (POS) system
- **OneSignal** - Push notifications
- **Meta WhatsApp** - WhatsApp messaging
- **Google Maps** - Geolocation services
- **Backblaze B2** - Cloud storage
- **2Factor** - OTP/SMS service

## Project Structure

```
src/main/java/com/hyp/
├── config/           # Configuration classes (Redis, Temporal, Security, etc.)
├── controller/       # REST API controllers
│   └── admin/        # Admin controllers (roles, policies)
├── entity/           # MongoDB document entities
├── enums/            # Enumeration types
├── event/            # Spring event classes
├── exception/        # Custom exceptions and handlers
├── playground/       # Development/testing utilities
├── repository/       # MongoDB repositories
├── request/          # Request DTOs
├── response/         # Response DTOs
├── seeder/           # Database seeders (permissions, roles, policies)
├── security/         # Security implementation
│   ├── apikey/       # API key authentication (webhooks)
│   ├── exception/    # Security exceptions
│   ├── jwt/          # JWT token management
│   ├── policy/       # Access policy enforcement
│   ├── principal/    # User principal & restaurant context
│   ├── ratelimit/    # Rate limiting
│   └── service/      # Security services (roles, permissions, cache)
├── service/          # Business logic services
├── translation/      # DTO translation classes
├── utils/            # Utility classes
├── validation/       # Custom validators
└── workflow/         # Temporal workflow definitions

load-testing/         # k6 load testing scripts
├── k6-config.js      # Configuration
├── k6-order-flow.js  # Order flow tests
├── k6-soak-test.js   # Soak testing
└── run-tests.sh      # Test runner script
```

## Core Features

### Order Management
- Full order lifecycle (create, update, cancel)
- Status tracking and transitions
- Order history and search

### Payment Processing
- Razorpay integration
- Payment order creation and verification
- Refund handling
- Multiple payment methods (UPI, Card, Wallet, NetBanking, Cash)

### Delivery Management
- Delivery quote generation
- Rider assignment and tracking
- Real-time delivery status updates
- Multiple delivery partner support

### Menu Management
- Menu extraction from POS systems
- Category and item management
- Variations and add-ons support
- Stock management

### Restaurant Operations
- Restaurant profile management
- Operating hours and availability
- Settlement and financial reconciliation
- Reports and analytics

### Notifications
- Push notifications (OneSignal)
- WhatsApp messaging (Meta)
- Email notifications
- SMS/OTP services

## API Endpoints

Base Path: `/api/v2`

### Core Endpoints

| Endpoint | Description |
|----------|-------------|
| `/order` | Order management |
| `/payment` | Payment processing |
| `/delivery` | Delivery operations |
| `/restaurant` | Restaurant management |
| `/customer` | Customer profiles |
| `/menu` | Menu extraction and import |
| `/item` | Menu item management |
| `/category` | Category management |
| `/settlement` | Financial settlements |
| `/report` | Business reports |
| `/notification` | Notification dispatch |
| `/pos` | POS data synchronization |

### Security Endpoints

| Endpoint | Description |
|----------|-------------|
| `/auth/refresh` | Refresh access token |
| `/auth/logout` | Logout current session |
| `/auth/logout-all` | Logout all sessions |
| `/auth/me` | Get current user info |
| `/admin/permissions` | Permission management (PLATFORM_ADMIN) |
| `/admin/roles` | Role management (PLATFORM_ADMIN) |
| `/admin/policies` | Access policy management (PLATFORM_ADMIN) |

## Workflow Engine

The application uses Temporal for orchestrating complex business processes:

- **OrderPaymentWorkflow** - Payment order creation and verification
- **OrderFulfillmentWorkflow** - POS order processing and delivery creation
- **OrderTrackWorkflow** - Order status tracking and notifications
- **RestaurantWorkflow** - Menu sync and restaurant status management
- **StockUpdateWorkflow** - Inventory synchronization

## Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB
- Redis
- Temporal Server
- Docker (optional)

## Configuration

Create a `.env` file in the project root with the following variables:

```env
# Environment
ENV=dev

# MongoDB
MONGODB_DATABASE=
MONGODB_USERNAME=
MONGODB_PASSWORD=
MONGODB_CLUSTER=

# Application
APP_DOMAIN=
APP_SECRET_KEY=
CORS_ALLOWED_ORIGINS=*

# JWT Security (Required)
JWT_SECRET=your-256-bit-secret-key-change-in-production

# POS System (PetPooja)
POS_PETPOOJA_URL=
POS_PETPOOJA_TOKEN=
POS_PETPOOJA_SECRET=
POS_PETPOOJA_KEY=

# SMS/OTP (2Factor)
SMS_KEY=

# Razorpay Payment Gateway
RAZORPAY_KEY=
RAZORPAY_SECRET=

# Pidge Delivery
DELIVERY_PIDGE_URL=
DELIVERY_PIDGE_USERNAME=
DELIVERY_PIDGE_PASSWORD=
DELIVERY_PIDGE_TOKEN=
DELIVERY_PIDGE_SMARTID=

# Google Maps
GOOGLE_KEY=

# Meta WhatsApp
FACEBOOK_GRAPH_API_URL=
NOTIFICATION_META_TOKEN=

# Chat.io
CHAT_IO_URL=

# OneSignal Push Notifications
ONE_SIGNAL_KEY=

# Backblaze B2 Storage
BUCKET_KEY=

# Email (Gmail SMTP)
MAIL_APP_PASSWORD=

# Redis Cache
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# Temporal Workflow Engine
TEMPORAL_HOST=
TEMPORAL_PORT=

# Internal Configuration
INTERNAL_USER_NUMBERS=
```

## Running the Application

### Using Maven

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Using Docker

```bash
# Build Docker image
docker build -t hyp-backend-api .

# Run container
docker run -p 9090:9090 --env-file .env hyp-backend-api
```

### Using Docker Compose

```bash
docker-compose up -d
```

## API Documentation

Once the application is running, access the API documentation at:

- Swagger UI: `http://localhost:9090/api/v2/swagger-ui.html`
- OpenAPI JSON: `http://localhost:9090/api/v2/api-docs`

## Environments

| Environment | URL |
|-------------|-----|
| Staging | https://api.hyperapps.cloud/api/v2 |
| Production | https://api.hyperapps.in/api/v2 |

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
```

## Load Testing

The `load-testing/` directory contains k6 scripts for performance testing:

```bash
# Install k6 (macOS)
brew install k6

# Run order flow load test
cd load-testing
./run-tests.sh

# Or run specific tests
k6 run k6-order-flow.js
k6 run k6-soak-test.js
```

> See [load-testing/README.md](./load-testing/README.md) and [load-testing/SETUP_GUIDE.md](./load-testing/SETUP_GUIDE.md) for detailed instructions.

## Code Formatting

The project uses Spotless with Palantir Java formatter:

```bash
# Check formatting
mvn spotless:check

# Apply formatting
mvn spotless:apply
```

## Key Entities

### Business Entities

| Entity | Description |
|--------|-------------|
| Order | Customer orders with items, status, and payment details |
| Customer | Customer profiles and preferences |
| Restaurant | Restaurant information and configuration |
| Payment | Payment transactions and status |
| Delivery | Delivery tracking and rider information |
| Settlement | Financial settlement records |
| Item | Menu items with pricing and attributes |
| Category | Menu categories hierarchy |
| Variation | Item variations (size, color, etc.) |
| AddonGroup | Groups of optional add-on items |

### Security Entities

| Entity | Collection | Description |
|--------|------------|-------------|
| Permission | `permissions` | Granular capabilities (e.g., `menu:read`, `order:create`) |
| Role | `roles` | Permission sets with inheritance support |
| AccessPolicy | `access_policies` | URL-based authorization rules |
| RefreshToken | `refresh_tokens` | Hashed refresh tokens with device tracking |
| ApiKeyConfig | `api_key_configs` | Webhook API key configuration |

## Order Status Flow

```
CREATED → CONFIRMED → PAYMENT_PENDING → PAID → PROCESSING → ACCEPTED → DISPATCHED → READY_FOR_DELIVERY → DELIVERED
                                                                    ↓
                                                               CANCELLED
```

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Run tests and ensure code formatting
4. Submit a pull request

## License

Proprietary - All rights reserved.