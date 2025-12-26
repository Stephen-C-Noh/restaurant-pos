# Restaurant POS System - Complete Implementation Guide

**Project Type:** Solo Learning Project with Production Patterns  
**Deployment:** On-premises (Local Development)  
**Timeline:** Flexible, milestone-driven  
**Date:** December 23, 2025

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Development Environment Setup](#4-development-environment-setup)
5. [Database Design](#5-database-design)
6. [Kafka Event Architecture](#6-kafka-event-architecture)
7. [Offline Mode Implementation](#7-offline-mode-implementation)
8. [API Specifications](#8-api-specifications)
9. [Frontend Architecture](#9-frontend-architecture)
10. [Phase-by-Phase Implementation](#10-phase-by-phase-implementation)
11. [Testing Strategy](#11-testing-strategy)
12. [Hardware Emulation](#12-hardware-emulation)
13. [Backend Core Implementation](#13-backend-core-implementation)
14. [Deployment & Operations](#14-deployment--operations)
15. [Next Steps & Resources](#15-next-steps--resources)
16. [Conclusion](#16-conclusion)

---

## 1. Project Overview

### 1.1 Goals
- Build a full-featured restaurant POS system as a learning project
- Implement production-grade patterns: event-driven architecture, offline-first, idempotency
- Emulate restaurant hardware (tablets, KDS screens, payment terminals) using standard devices
- Create a portfolio-worthy project demonstrating modern software engineering practices

### 1.2 Core Features
- **Order Management:** Full order lifecycle from creation to completion
- **Kitchen Display System (KDS):** Real-time order display for kitchen staff
- **Offline Mode:** Continue operations without internet connectivity
- **Event-Driven:** Kafka-based messaging for system decoupling
- **Payment Processing:** Mock payment terminal with proper flow
- **Table Management:** Floor plan, table states, server assignments
- **Reporting:** Basic sales and operational reports

### 1.3 Non-Goals (For Now)
- Multi-tenant/multi-location support (can add later)
- Advanced inventory management (focus on order flow first)
- Customer-facing online ordering (internal operations only)
- Complex loyalty programs

---

## 2. Architecture

### 2.1 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend Layer                           │
├──────────────────┬──────────────────┬──────────────────────────┤
│  POS Terminal    │   KDS Display    │   Admin Dashboard        │
│  (React + PWA)   │   (React)        │   (React)                │
│  ├─IndexedDB     │   └─WebSocket    │   └─REST API             │
│  └─Service Worker│                  │                           │
└────────┬─────────┴──────────┬───────┴──────────────────────────┘
         │                     │
         │ REST API            │ WebSocket
         │                     │
┌────────▼─────────────────────▼───────────────────────────────────┐
│                        API Gateway                                │
│                    (Spring Boot Backend)                          │
├───────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Order      │  │   Payment    │  │   Menu       │          │
│  │   Service    │  │   Service    │  │   Service    │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┴──────────────────┘                   │
│                            │                                      │
│                    Kafka Producer                                 │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   Apache Kafka  │
                    │   Topics:       │
                    │   - order.*     │
                    │   - payment.*   │
                    │   - inventory.* │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼─────┐      ┌─────▼──────┐     ┌─────▼──────┐
    │   KDS    │      │ Analytics  │     │ Inventory  │
    │ Consumer │      │ Consumer   │     │ Consumer   │
    └────┬─────┘      └────┬───────┘     └────┬───────┘
         │                 │                   │
         │ WebSocket       │ Write             │ Update
         │                 │                   │
    ┌────▼─────────────────▼───────────────────▼───────┐
    │              PostgreSQL Database                  │
    │  - Orders    - Payments    - Inventory            │
    │  - Menu      - Users       - Analytics            │
    └───────────────────────────────────────────────────┘
```

### 2.2 Service Breakdown

**Core Services:**
1. **Order Service:** Order CRUD, state management, idempotency
2. **Payment Service:** Payment intent creation, capture, refunds
3. **Menu Service:** Menu items, modifiers, pricing, availability
4. **KDS Consumer:** Consumes order events, pushes to WebSocket
5. **Analytics Consumer:** Aggregates events for reporting
6. **Inventory Consumer:** Tracks ingredient depletion (future)

**Cross-Cutting:**
- Authentication & Authorization (JWT)
- Idempotency handling
- Event publishing (Outbox pattern)
- Offline sync resolution

---

## 3. Technology Stack

### 3.1 Backend
```yaml
Language: Java 21
Framework: Spring Boot 3.4.x
Dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-websocket
  - spring-kafka
  - postgresql
  - flyway-core
  - lombok
  - jjwt (JWT handling)
  - jackson-databind
  - spring-boot-starter-validation
```

### 3.2 Frontend
```yaml
Framework: React 18 + Vite
State Management: React Context + useReducer (or Zustand for simpler state)
Offline Storage: IndexedDB (via idb library)
Service Worker: Workbox
Real-time: WebSocket (SockJS + STOMP)
HTTP Client: Axios
UI Components: TailwindCSS + Headless UI
Testing: Vitest + React Testing Library
```

### 3.3 Infrastructure
```yaml
Database: PostgreSQL 16
Message Broker: Apache Kafka 3.6 + Zookeeper
Containerization: Docker + Docker Compose
Development Tools:
  - pgAdmin (database GUI)
  - Kafka UI (Conduktor or similar)
  - Postman/Insomnia (API testing)
```

### 3.4 Observability (Optional, Later)
```yaml
Logging: Logback with JSON formatting
Metrics: Micrometer + Prometheus
Tracing: Spring Boot Actuator
Monitoring Dashboard: Grafana
```

---

## 4. Development Environment Setup

### 4.1 Prerequisites
```bash
# Required installations
- Java JDK 21 (OpenJDK or Oracle)
- Node.js 20.x LTS
- Docker Desktop
- Git
- IDE: IntelliJ IDEA Community or VS Code
```

### 4.2 Project Structure
```
restaurant-pos/
├── docker/
│   ├── docker-compose.yml
│   └── postgres/
│       └── init.sql
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yourname/pos/
│   │   │   │   ├── PosApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── KafkaConfig.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   └── JwtService.java
│   │   │   │   ├── order/
│   │   │   │   │   ├── Order.java
│   │   │   │   │   ├── OrderService.java
│   │   │   │   │   ├── OrderController.java
│   │   │   │   │   ├── OrderRepository.java
│   │   │   │   │   └── OrderEventPublisher.java
│   │   │   │   ├── menu/
│   │   │   │   │   ├── MenuItem.java
│   │   │   │   │   ├── MenuService.java
│   │   │   │   │   ├── MenuController.java
│   │   │   │   │   └── MenuRepository.java
│   │   │   │   ├── payment/
│   │   │   │   │   ├── Payment.java
│   │   │   │   │   ├── PaymentService.java
│   │   │   │   │   ├── PaymentController.java
│   │   │   │   │   └── PaymentRepository.java
│   │   │   │   ├── kds/
│   │   │   │   │   ├── KdsController.java
│   │   │   │   │   ├── KdsEventConsumer.java
│   │   │   │   │   └── KdsWebSocketHandler.java
│   │   │   │   ├── common/
│   │   │   │   │   ├── IdempotencyService.java
│   │   │   │   │   ├── OutboxEventRepository.java
│   │   │   │   │   └── OutboxEventPublisher.java
│   │   │   │   └── events/
│   │   │   │       ├── OrderCreatedEvent.java
│   │   │   │       ├── OrderFiredEvent.java
│   │   │   │       ├── OrderCompletedEvent.java
│   │   │   │       └── PaymentCapturedEvent.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__initial_schema.sql
│   │   │           ├── V2__add_idempotency.sql
│   │   │           └── V3__add_outbox.sql
│   │   └── test/
│   ├── pom.xml
│   └── README.md
├── frontend/
│   ├── pos-terminal/
│   │   ├── public/
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── Menu/
│   │   │   │   ├── Cart/
│   │   │   │   ├── Checkout/
│   │   │   │   └── Tables/
│   │   │   ├── services/
│   │   │   │   ├── api.js
│   │   │   │   ├── offlineQueue.js
│   │   │   │   ├── indexedDB.js
│   │   │   │   └── syncService.js
│   │   │   ├── hooks/
│   │   │   │   ├── useOffline.js
│   │   │   │   └── useOrders.js
│   │   │   ├── context/
│   │   │   │   ├── AuthContext.jsx
│   │   │   │   └── OrderContext.jsx
│   │   │   ├── App.jsx
│   │   │   ├── main.jsx
│   │   │   └── sw.js (Service Worker)
│   │   ├── package.json
│   │   └── vite.config.js
│   ├── kds-display/
│   │   ├── src/
│   │   │   ├── components/
│   │   │   │   ├── TicketBoard/
│   │   │   │   └── Ticket/
│   │   │   ├── services/
│   │   │   │   └── websocket.js
│   │   │   └── App.jsx
│   │   └── package.json
│   └── admin-dashboard/
│       └── (similar structure)
└── README.md
```

### 4.3 Docker Compose Configuration

```yaml
# docker/docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: pos-postgres
    environment:
      POSTGRES_DB: restaurant_pos
      POSTGRES_USER: pos_user
      POSTGRES_PASSWORD: pos_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U pos_user -d restaurant_pos"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: pos-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: pos-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 10s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: pos-kafka-ui
    depends_on:
      - kafka
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181

  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: pos-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@pos.local
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
```

### 4.4 Initial Setup Commands

```bash
# 1. Clone/Create project
mkdir restaurant-pos && cd restaurant-pos
git init

# 2. Start infrastructure
cd docker
docker-compose up -d

# Verify services
docker ps  # All containers should be running
docker logs pos-kafka  # Check Kafka is ready

# 3. Backend setup
cd ../backend
# Create Spring Boot project using start.spring.io or use Maven archetype
# Add dependencies (see pom.xml section below)

./mvnw clean install
./mvnw spring-boot:run

# 4. Frontend setup
cd ../frontend/pos-terminal
npm install
npm run dev

# 5. Verify
# - Backend: http://localhost:8090
# - Frontend: http://localhost:5173
# - Kafka UI: http://localhost:8080
# - pgAdmin: http://localhost:5050
```

---

## 5. Database Design

### 5.1 Core Schema

```sql
-- V1__initial_schema.sql

-- Users & Authentication
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'SERVER', 'KITCHEN')),
    pin_hash VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

-- Menu Categories
CREATE TABLE menu_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Menu Items
CREATE TABLE menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES menu_categories(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sku VARCHAR(50) UNIQUE,
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
    tax_rate DECIMAL(5,4) DEFAULT 0.05,
    prep_time_minutes INT DEFAULT 10,
    active BOOLEAN DEFAULT true,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_items_category ON menu_items(category_id);
CREATE INDEX idx_menu_items_active ON menu_items(active);

-- Modifiers (e.g., "No onions", "Extra cheese")
CREATE TABLE modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    modifier_type VARCHAR(20) CHECK (modifier_type IN ('ADDITION', 'REMOVAL', 'SUBSTITUTION')),
    price_delta DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Menu Item Modifiers (many-to-many)
CREATE TABLE menu_item_modifiers (
    menu_item_id UUID REFERENCES menu_items(id) ON DELETE CASCADE,
    modifier_id UUID REFERENCES modifiers(id) ON DELETE CASCADE,
    max_selections INT DEFAULT 1,
    required BOOLEAN DEFAULT false,
    PRIMARY KEY (menu_item_id, modifier_id)
);

-- Tables
CREATE TABLE tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_number VARCHAR(10) UNIQUE NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    zone VARCHAR(50),
    status VARCHAR(20) DEFAULT 'AVAILABLE' 
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING')),
    current_server_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tables_status ON tables(status);

-- Orders
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(20) UNIQUE NOT NULL,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('DINE_IN', 'TAKEOUT', 'DELIVERY')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'FIRED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED')),
    table_id UUID REFERENCES tables(id),
    server_id UUID REFERENCES users(id),
    guest_count INT CHECK (guest_count > 0),
    subtotal DECIMAL(10,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    tip_amount DECIMAL(10,2) DEFAULT 0.00,
    total DECIMAL(10,2) DEFAULT 0.00,
    notes TEXT,
    fired_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INT DEFAULT 1  -- For optimistic locking
);

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_table ON orders(table_id);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_order_number ON orders(order_number);

-- Order Items
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    special_instructions TEXT,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'FIRED', 'PREPARING', 'READY', 'SERVED')),
    course_number INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_status ON order_items(status);

-- Order Item Modifiers
CREATE TABLE order_item_modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_id UUID NOT NULL REFERENCES modifiers(id),
    price_delta DECIMAL(10,2) DEFAULT 0.00
);

-- Payments
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    payment_intent_id VARCHAR(100) UNIQUE,
    payment_method VARCHAR(20) CHECK (payment_method IN ('CARD', 'CASH', 'MOBILE_WALLET')),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'VOIDED')),
    gateway_reference VARCHAR(100),
    gateway_response JSONB,
    captured_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_intent ON payments(payment_intent_id);
```

### 5.2 Idempotency & Offline Support

```sql
-- V2__add_idempotency.sql

-- Idempotency tracking
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(36) UNIQUE;
ALTER TABLE orders ADD COLUMN client_timestamp TIMESTAMP;
ALTER TABLE orders ADD COLUMN sync_status VARCHAR(20) DEFAULT 'SYNCED'
    CHECK (sync_status IN ('SYNCED', 'PENDING', 'CONFLICT'));

CREATE INDEX idx_orders_idempotency ON orders(idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(36) UNIQUE;
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Request deduplication cache (for short-term idempotency)
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(36) PRIMARY KEY,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    response_body JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_records(expires_at);
```

### 5.3 Event Outbox Pattern

```sql
-- V3__add_outbox.sql

-- Transactional outbox for reliable event publishing
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,  -- 'Order', 'Payment', 'Inventory'
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,     -- 'order.created', 'order.fired'
    payload JSONB NOT NULL,
    kafka_topic VARCHAR(100) NOT NULL,
    kafka_key VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
```

### 5.4 Seed Data

```sql
-- Insert default admin user (password: admin123)
INSERT INTO users (id, username, password_hash, full_name, role, pin_hash) VALUES
('00000000-0000-0000-0000-000000000001', 'admin', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
 'System Admin', 'ADMIN', '$2a$10$1234567890abcdefghijk');

-- Insert sample categories
INSERT INTO menu_categories (id, name, display_order) VALUES
('10000000-0000-0000-0000-000000000001', 'Appetizers', 1),
('10000000-0000-0000-0000-000000000002', 'Entrees', 2),
('10000000-0000-0000-0000-000000000003', 'Desserts', 3),
('10000000-0000-0000-0000-000000000004', 'Beverages', 4);

-- Insert sample menu items
INSERT INTO menu_items (id, category_id, name, description, sku, base_price, prep_time_minutes) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 
 'Caesar Salad', 'Crisp romaine, parmesan, croutons', 'APP-001', 12.99, 5),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 
 'Grilled Salmon', 'Atlantic salmon with seasonal vegetables', 'ENT-001', 28.99, 15),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 
 'Chocolate Lava Cake', 'Warm chocolate cake with vanilla ice cream', 'DES-001', 9.99, 8);

-- Insert sample modifiers
INSERT INTO modifiers (id, name, modifier_type, price_delta) VALUES
('30000000-0000-0000-0000-000000000001', 'No Onions', 'REMOVAL', 0.00),
('30000000-0000-0000-0000-000000000002', 'Extra Cheese', 'ADDITION', 2.00),
('30000000-0000-0000-0000-000000000003', 'Gluten Free Bun', 'SUBSTITUTION', 1.50);

-- Insert sample tables
INSERT INTO tables (id, table_number, capacity, zone) VALUES
('40000000-0000-0000-0000-000000000001', 'T1', 4, 'Main Dining'),
('40000000-0000-0000-0000-000000000002', 'T2', 2, 'Main Dining'),
('40000000-0000-0000-0000-000000000003', 'T3', 6, 'Patio');
```

---

## 6. Kafka Event Architecture

### 6.1 Event Types & Topics

```yaml
Topics:
  order.events:
    - order.created       # New order submitted
    - order.updated       # Order modified (items added/removed)
    - order.fired         # Sent to kitchen
    - order.preparing     # Kitchen started working
    - order.ready         # Food ready for pickup
    - order.completed     # Order closed/paid
    - order.cancelled     # Order cancelled
  
  payment.events:
    - payment.intent.created
    - payment.authorized
    - payment.captured
    - payment.failed
    - payment.refunded
  
  inventory.events:
    - inventory.depleted  # Item used in order
    - inventory.adjusted  # Manual adjustment
    - inventory.low.stock # Alert threshold reached
```

### 6.2 Event Schema

```java
// Base Event
@Data
public abstract class DomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant timestamp = Instant.now();
    private int version = 1;
}

// Order Events
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends DomainEvent {
    private UUID orderId;
    private String orderNumber;
    private String orderType;
    private UUID tableId;
    private String tableName;
    private UUID serverId;
    private String serverName;
    private List<OrderItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private Instant createdAt;
    
    public OrderCreatedEvent(Order order, Map<UUID, MenuItem> menuItemMap, Map<UUID, Modifier> modifierMap) {
        this.setEventType("order.created");
        this.setAggregateId(order.getId().toString());
        this.setAggregateType("Order");
        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.orderType = order.getOrderType().toString();
        this.tableId = order.getTableId();
        this.serverId = order.getServerId();
        this.subtotal = order.getSubtotal();
        this.taxAmount = order.getTaxAmount();
        this.total = order.getTotal();
        this.createdAt = order.getCreatedAt();
        
        // Map items to DTOs with menu item names
        this.items = order.getItems().stream()
            .map(item -> OrderItemDto.from(item, menuItemMap.get(item.getMenuItemId())))
            .collect(Collectors.toList());
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderFiredEvent extends DomainEvent {
    private UUID orderId;
    private String orderNumber;
    private UUID tableId;
    private String tableName;
    private List<OrderItemDto> items;
    private Instant firedAt;
    private String firedBy;
    
    public OrderFiredEvent(Order order, Map<UUID, MenuItem> menuItemMap, Map<UUID, Modifier> modifierMap) {
        this.setEventType("order.fired");
        this.setAggregateId(order.getId().toString());
        this.setAggregateType("Order");
        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.tableId = order.getTableId();
        this.firedAt = order.getFiredAt();
        
        // Map items to DTOs with menu item names
        this.items = order.getItems().stream()
            .map(item -> OrderItemDto.from(item, menuItemMap.get(item.getMenuItemId())))
            .collect(Collectors.toList());
    }
}

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCompletedEvent extends DomainEvent {
    private UUID orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private Instant completedAt;
    
    public OrderCompletedEvent(Order order) {
        this.setEventType("order.completed");
        this.setAggregateId(order.getId().toString());
        this.setAggregateType("Order");
        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.totalAmount = order.getTotal();
        this.completedAt = order.getCompletedAt();
    }
}

// Payment Events
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentCapturedEvent extends DomainEvent {
    private UUID paymentId;
    private UUID orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private String gatewayReference;
    private Instant capturedAt;
    
    public PaymentCapturedEvent(Payment payment) {
        this.setEventType("payment.captured");
        this.setAggregateId(payment.getId().toString());
        this.setAggregateType("Payment");
        this.paymentId = payment.getId();
        this.orderId = payment.getOrderId();
        this.paymentMethod = payment.getPaymentMethod().toString();
        this.amount = payment.getAmount();
        this.gatewayReference = payment.getGatewayReference();
        this.capturedAt = payment.getCapturedAt();
    }
}

// DTO Classes for Events
@Data
public class OrderItemDto {
    private UUID id;
    private UUID menuItemId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private String specialInstructions;
    private List<ModifierDto> modifiers;
    
    public static OrderItemDto from(OrderItem item, MenuItem menuItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setMenuItemId(item.getMenuItemId());
        dto.setName(menuItem.getName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSpecialInstructions(item.getSpecialInstructions());
        
        if (item.getModifiers() != null) {
            dto.setModifiers(item.getModifiers().stream()
                .map(ModifierDto::from)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
}

@Data
public class ModifierDto {
    private UUID modifierId;
    private String name;
    private BigDecimal priceDelta;
    
    public static ModifierDto from(OrderItemModifier modifier) {
        ModifierDto dto = new ModifierDto();
        dto.setModifierId(modifier.getModifierId());
        dto.setName(modifier.getModifier().getName()); // Assumes OrderItemModifier has a Modifier reference
        dto.setPriceDelta(modifier.getPriceDelta());
        return dto;
    }
}
```

### 6.3 Kafka Configuration (Spring Boot)

```java
// KafkaConfig.java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, DomainEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, DomainEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "pos-service-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.yourname.pos.events");
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DomainEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: pos-service-group
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "com.yourname.pos.events"
```

### 6.4 Event Publishing (Outbox Pattern)

```java
// OutboxEventPublisher.java
@Service
@Slf4j
public class OutboxEventPublisher {
    
    @Autowired
    private OutboxEventRepository outboxRepo;
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    /**
     * Save event to outbox (called within transaction)
     */
    public void saveToOutbox(DomainEvent event, String topic) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(event.getAggregateType());
        outboxEvent.setAggregateId(UUID.fromString(event.getAggregateId()));
        outboxEvent.setEventType(event.getEventType());
        outboxEvent.setPayload(convertToJson(event));
        outboxEvent.setKafkaTopic(topic);
        outboxEvent.setKafkaKey(event.getAggregateId());
        outboxEvent.setStatus("PENDING");
        
        outboxRepo.save(outboxEvent);
        log.info("Saved event to outbox: {}", event.getEventType());
    }
    
    /**
     * Background job to publish pending events
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepo.findByStatusOrderByCreatedAtAsc("PENDING", 
            PageRequest.of(0, 100));
        
        for (OutboxEvent event : pending) {
            try {
                DomainEvent domainEvent = convertFromJson(event.getPayload(), 
                    event.getEventType());
                
                kafkaTemplate.send(event.getKafkaTopic(), event.getKafkaKey(), domainEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            event.setStatus("PUBLISHED");
                            event.setPublishedAt(Instant.now());
                            outboxRepo.save(event);
                            log.info("Published event: {}", event.getEventType());
                        } else {
                            event.setStatus("FAILED");
                            event.setRetryCount(event.getRetryCount() + 1);
                            event.setErrorMessage(ex.getMessage());
                            outboxRepo.save(event);
                            log.error("Failed to publish event: {}", event.getEventType(), ex);
                        }
                    });
                    
            } catch (Exception e) {
                log.error("Error processing outbox event: {}", event.getId(), e);
            }
        }
    }
    
    private String convertToJson(DomainEvent event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
    
    private DomainEvent convertFromJson(String json, String eventType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            // Map event type to class
            Class<? extends DomainEvent> eventClass;
            switch (eventType) {
                case "order.created":
                    eventClass = OrderCreatedEvent.class;
                    break;
                case "order.fired":
                    eventClass = OrderFiredEvent.class;
                    break;
                case "order.completed":
                    eventClass = OrderCompletedEvent.class;
                    break;
                case "payment.captured":
                    eventClass = PaymentCapturedEvent.class;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown event type: " + eventType);
            }
            
            return mapper.readValue(json, eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + eventType, e);
        }
    }
}
```

### 6.5 Event Consumers

```java
// KdsEventConsumer.java
@Service
@Slf4j
public class KdsEventConsumer {
    
    @Autowired
    private SimpMessagingTemplate websocketTemplate;
    
    @Autowired
    private OrderRepository orderRepo;
    
    @KafkaListener(topics = "order.events", groupId = "kds-service-group")
    public void handleOrderEvent(DomainEvent event) {
        log.info("Received event: {}", event.getEventType());
        
        switch (event.getEventType()) {
            case "order.created":
                handleOrderCreated((OrderCreatedEvent) event);
                break;
            case "order.fired":
                handleOrderFired((OrderFiredEvent) event);
                break;
            case "order.completed":
                handleOrderCompleted((OrderCompletedEvent) event);
                break;
            default:
                log.warn("Unhandled event type: {}", event.getEventType());
        }
    }
    
    private void handleOrderCreated(OrderCreatedEvent event) {
        // Don't show on KDS until fired
        log.info("Order created: {}", event.getOrderNumber());
    }
    
    private void handleOrderFired(OrderFiredEvent event) {
        // Send to KDS display via WebSocket
        KdsTicket ticket = createKdsTicket(event);
        websocketTemplate.convertAndSend("/topic/kds", ticket);
        log.info("Sent order to KDS: {}", event.getOrderNumber());
    }
    
    private void handleOrderCompleted(OrderCompletedEvent event) {
        // Remove from KDS display
        websocketTemplate.convertAndSend("/topic/kds/completed", event.getOrderId());
        log.info("Order completed: {}", event.getOrderNumber());
    }
    
    private KdsTicket createKdsTicket(OrderFiredEvent event) {
        KdsTicket ticket = new KdsTicket();
        ticket.setOrderId(event.getOrderId());
        ticket.setOrderNumber(event.getOrderNumber());
        ticket.setTableName(event.getTableId() != null ? "Table " + event.getTableId() : "Takeout");
        ticket.setItems(event.getItems());
        ticket.setFiredAt(event.getFiredAt());
        ticket.setStatus("NEW");
        return ticket;
    }
}

// AnalyticsEventConsumer.java
@Service
@Slf4j
public class AnalyticsEventConsumer {
    
    @Autowired
    private OrderRepository orderRepo;
    
    @KafkaListener(topics = "order.events", groupId = "analytics-service-group")
    public void handleOrderEvent(DomainEvent event) {
        // Aggregate events for reporting
        switch (event.getEventType()) {
            case "order.completed":
                updateDailySalesMetrics((OrderCompletedEvent) event);
                break;
            // Handle other events...
        }
    }
    
    private void updateDailySalesMetrics(OrderCompletedEvent event) {
        // Update daily sales aggregates
        log.info("Updated analytics for order: {}", event.getOrderNumber());
    }
}
```

---

## 7. Offline Mode Implementation

### 7.1 Architecture Overview

```
┌─────────────────────────────────────────────┐
│         POS Terminal (Browser)              │
├─────────────────────────────────────────────┤
│  ┌────────────┐  ┌──────────────┐          │
│  │   React    │  │   Service    │          │
│  │    App     │←→│   Worker     │          │
│  └─────┬──────┘  └──────┬───────┘          │
│        │                 │                   │
│        ↓                 ↓                   │
│  ┌────────────┐  ┌──────────────┐          │
│  │  IndexedDB │  │   Cache API  │          │
│  │  (Data)    │  │   (Assets)   │          │
│  └────────────┘  └──────────────┘          │
└─────────────────────────────────────────────┘
                    │
        ┌───────────┴──────────┐
        │  Online?             │
        ├──────────┬───────────┤
        │   YES    │    NO     │
        │          │           │
        ↓          ↓           ↓
   Direct API   Queue in   Read from
   Calls        IndexedDB  IndexedDB
                    │
                    └→ Sync when online
```

### 7.2 Service Worker Setup

```javascript
// frontend/pos-terminal/public/sw.js
const CACHE_NAME = 'pos-cache-v1';
const urlsToCache = [
  '/',
  '/index.html',
  '/static/js/main.js',
  '/static/css/main.css',
  '/manifest.json'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
  );
});

// Fetch event - serve from cache when offline
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        // Cache hit - return response
        if (response) {
          return response;
        }
        
        // Clone the request
        const fetchRequest = event.request.clone();
        
        return fetch(fetchRequest).then((response) => {
          // Check if valid response
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response;
          }
          
          // Clone the response
          const responseToCache = response.clone();
          
          caches.open(CACHE_NAME)
            .then((cache) => {
              cache.put(event.request, responseToCache);
            });
          
          return response;
        });
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
```

### 7.3 IndexedDB Schema

```javascript
// frontend/pos-terminal/src/services/indexedDB.js
import { openDB } from 'idb';

const DB_NAME = 'pos-db';
const DB_VERSION = 1;

export async function initDB() {
  const db = await openDB(DB_NAME, DB_VERSION, {
    upgrade(db, oldVersion, newVersion, transaction) {
      // Menu items cache
      if (!db.objectStoreNames.contains('menu-items')) {
        const menuStore = db.createObjectStore('menu-items', { keyPath: 'id' });
        menuStore.createIndex('categoryId', 'categoryId');
        menuStore.createIndex('active', 'active');
      }
      
      // Menu categories cache
      if (!db.objectStoreNames.contains('menu-categories')) {
        db.createObjectStore('menu-categories', { keyPath: 'id' });
      }
      
      // Offline orders queue
      if (!db.objectStoreNames.contains('orders-queue')) {
        const ordersStore = db.createObjectStore('orders-queue', { keyPath: 'id' });
        ordersStore.createIndex('synced', 'synced');
        ordersStore.createIndex('timestamp', 'timestamp');
      }
      
      // Settings cache
      if (!db.objectStoreNames.contains('settings')) {
        db.createObjectStore('settings', { keyPath: 'key' });
      }
      
      // Tables cache
      if (!db.objectStoreNames.contains('tables')) {
        db.createObjectStore('tables', { keyPath: 'id' });
      }
    }
  });
  
  return db;
}

// Menu operations
export async function cacheMenu(menuItems, categories) {
  const db = await initDB();
  const tx = db.transaction(['menu-items', 'menu-categories'], 'readwrite');
  
  // Clear existing
  await tx.objectStore('menu-items').clear();
  await tx.objectStore('menu-categories').clear();
  
  // Store new data
  for (const item of menuItems) {
    await tx.objectStore('menu-items').put(item);
  }
  
  for (const category of categories) {
    await tx.objectStore('menu-categories').put(category);
  }
  
  await tx.done;
  console.log('Menu cached successfully');
}

export async function getMenuFromCache() {
  const db = await initDB();
  const [items, categories] = await Promise.all([
    db.getAll('menu-items'),
    db.getAll('menu-categories')
  ]);
  
  return { items, categories };
}

// Offline queue operations
export async function queueOrder(order) {
  const db = await initDB();
  const queueItem = {
    id: crypto.randomUUID(),
    idempotencyKey: crypto.randomUUID(),
    order: order,
    timestamp: Date.now(),
    synced: false,
    retryCount: 0
  };
  
  await db.put('orders-queue', queueItem);
  console.log('Order queued:', queueItem.id);
  return queueItem;
}

export async function getPendingOrders() {
  const db = await initDB();
  const index = db.transaction('orders-queue').store.index('synced');
  return await index.getAll(false);
}

export async function markOrderSynced(queueItemId) {
  const db = await initDB();
  const item = await db.get('orders-queue', queueItemId);
  if (item) {
    item.synced = true;
    await db.put('orders-queue', item);
  }
}

export async function incrementRetryCount(queueItemId) {
  const db = await initDB();
  const item = await db.get('orders-queue', queueItemId);
  if (item) {
    item.retryCount = (item.retryCount || 0) + 1;
    await db.put('orders-queue', item);
  }
}
```

### 7.4 Offline Detection Hook

```javascript
// frontend/pos-terminal/src/hooks/useOffline.js
import { useState, useEffect } from 'react';

export function useOffline() {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  
  useEffect(() => {
    const handleOnline = () => {
      console.log('Connection restored');
      setIsOnline(true);
    };
    
    const handleOffline = () => {
      console.log('Connection lost');
      setIsOnline(false);
    };
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  
  return isOnline;
}
```

### 7.5 Sync Service

```javascript
// frontend/pos-terminal/src/services/syncService.js
import axios from 'axios';
import { getPendingOrders, markOrderSynced, incrementRetryCount } from './indexedDB';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090/api';

export class SyncService {
  constructor() {
    this.isSyncing = false;
  }
  
  async syncPendingOrders() {
    if (this.isSyncing) {
      console.log('Sync already in progress');
      return;
    }
    
    this.isSyncing = true;
    console.log('Starting sync...');
    
    try {
      const pending = await getPendingOrders();
      console.log(`Found ${pending.length} pending orders`);
      
      for (const queueItem of pending) {
        try {
          await this.syncSingleOrder(queueItem);
        } catch (error) {
          console.error(`Failed to sync order ${queueItem.id}:`, error);
          await incrementRetryCount(queueItem.id);
          
          // If retry count exceeds threshold, notify user
          if (queueItem.retryCount >= 5) {
            this.notifyFailedSync(queueItem);
          }
        }
      }
      
      console.log('Sync completed');
    } finally {
      this.isSyncing = false;
    }
  }
  
  async syncSingleOrder(queueItem) {
    const response = await axios.post(
      `${API_BASE_URL}/orders`,
      queueItem.order,
      {
        headers: {
          'Idempotency-Key': queueItem.idempotencyKey,
          'X-Client-Timestamp': queueItem.timestamp,
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );
    
    if (response.status === 200 || response.status === 201) {
      await markOrderSynced(queueItem.id);
      console.log(`Order ${queueItem.id} synced successfully`);
      return response.data;
    }
  }
  
  notifyFailedSync(queueItem) {
    // Show toast notification or modal to user
    console.error(`Order ${queueItem.id} failed to sync after ${queueItem.retryCount} attempts`);
    // You can dispatch a Redux action or use a notification library here
  }
}

export const syncService = new SyncService();
```

### 7.6 API Service with Offline Support

```javascript
// frontend/pos-terminal/src/services/api.js
import axios from 'axios';
import { queueOrder } from './indexedDB';
import { syncService } from './syncService';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      // Network error - likely offline
      console.log('Network error detected');
    }
    return Promise.reject(error);
  }
);

export const api = {
  // Auth
  async login(credentials) {
    const response = await apiClient.post('/auth/login', credentials);
    return response.data;
  },
  
  // Menu
  async getMenu() {
    const response = await apiClient.get('/menu');
    return response.data;
  },
  
  // Orders - with offline support
  async createOrder(orderData) {
    // Check if online
    if (!navigator.onLine) {
      console.log('Offline - queueing order');
      const queuedItem = await queueOrder(orderData);
      return {
        id: queuedItem.id,
        status: 'QUEUED',
        message: 'Order queued - will sync when online',
        queued: true
      };
    }
    
    try {
      const idempotencyKey = crypto.randomUUID();
      const response = await apiClient.post('/orders', orderData, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      });
      
      // Trigger background sync of any pending orders
      setTimeout(() => syncService.syncPendingOrders(), 1000);
      
      return response.data;
    } catch (error) {
      // If network error, queue the order
      if (!error.response) {
        console.log('Network error - queueing order');
        const queuedItem = await queueOrder(orderData);
        return {
          id: queuedItem.id,
          status: 'QUEUED',
          message: 'Order queued - will sync when online',
          queued: true
        };
      }
      throw error;
    }
  },
  
  async getOrders(filters) {
    const response = await apiClient.get('/orders', { params: filters });
    return response.data;
  },
  
  async getOrderById(id) {
    const response = await apiClient.get(`/orders/${id}`);
    return response.data;
  },
  
  async fireOrder(orderId) {
    const response = await apiClient.post(`/orders/${orderId}/fire`);
    return response.data;
  },
  
  // Payments
  async createPaymentIntent(paymentData) {
    const response = await apiClient.post('/payments/intents', paymentData);
    return response.data;
  },
  
  async capturePayment(intentId) {
    const response = await apiClient.post(`/payments/${intentId}/capture`);
    return response.data;
  },
  
  // Tables
  async getTables() {
    const response = await apiClient.get('/tables');
    return response.data;
  },
  
  async updateTableStatus(tableId, status) {
    const response = await apiClient.patch(`/tables/${tableId}/status`, { status });
    return response.data;
  }
};
```

---

## 8. API Specifications

### 8.1 Authentication Endpoints

```
POST /api/auth/login
Request:
{
  "username": "server1",
  "password": "password123"
}

Response 200:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": "uuid",
    "username": "server1",
    "fullName": "John Doe",
    "role": "SERVER"
  },
  "expiresIn": 3600
}

POST /api/auth/refresh
Request:
{
  "refreshToken": "eyJhbGc..."
}

Response 200:
{
  "accessToken": "eyJhbGc...",
  "expiresIn": 3600
}

POST /api/auth/logout
Headers: Authorization: Bearer <token>
Response 204: No Content
```

### 8.2 Menu Endpoints

```
GET /api/menu
Query Params:
  - activeOnly: boolean (default: true)
  - categoryId: uuid (optional)

Response 200:
{
  "categories": [
    {
      "id": "uuid",
      "name": "Appetizers",
      "displayOrder": 1,
      "items": [
        {
          "id": "uuid",
          "name": "Caesar Salad",
          "description": "...",
          "basePrice": 12.99,
          "taxRate": 0.05,
          "prepTimeMinutes": 5,
          "availableModifiers": [
            {
              "id": "uuid",
              "name": "Extra Cheese",
              "priceDelta": 2.00
            }
          ]
        }
      ]
    }
  ]
}

GET /api/menu/items/{id}
Response 200:
{
  "id": "uuid",
  "name": "Grilled Salmon",
  "basePrice": 28.99,
  ...
}

POST /api/menu/items
Request:
{
  "categoryId": "uuid",
  "name": "New Item",
  "basePrice": 15.99,
  "taxRate": 0.05
}

Response 201:
{
  "id": "uuid",
  ...
}
```

### 8.3 Order Endpoints

```
POST /api/orders
Headers:
  - Idempotency-Key: uuid (required)
  - X-Client-Timestamp: timestamp (optional)

Request:
{
  "orderType": "DINE_IN",
  "tableId": "uuid",
  "serverId": "uuid",
  "guestCount": 2,
  "items": [
    {
      "menuItemId": "uuid",
      "quantity": 1,
      "specialInstructions": "No onions",
      "modifiers": [
        {
          "modifierId": "uuid"
        }
      ]
    }
  ]
}

Response 201:
{
  "id": "uuid",
  "orderNumber": "ORD-20251223-001",
  "status": "DRAFT",
  "subtotal": 42.98,
  "taxAmount": 2.15,
  "total": 45.13,
  "items": [...],
  "createdAt": "2025-12-23T10:30:00Z"
}

GET /api/orders
Query Params:
  - status: DRAFT | SUBMITTED | FIRED | COMPLETED
  - tableId: uuid
  - from: date
  - to: date
  - page: int
  - size: int

Response 200:
{
  "content": [...],
  "totalElements": 50,
  "totalPages": 5,
  "page": 0,
  "size": 10
}

GET /api/orders/{id}
Response 200:
{
  "id": "uuid",
  "orderNumber": "ORD-20251223-001",
  ...
}

POST /api/orders/{id}/fire
Request:
{
  "itemIds": ["uuid1", "uuid2"]  // Optional: fire specific items only
}

Response 200:
{
  "id": "uuid",
  "status": "FIRED",
  "firedAt": "2025-12-23T10:35:00Z"
}

POST /api/orders/{id}/items
Request:
{
  "items": [
    {
      "menuItemId": "uuid",
      "quantity": 1
    }
  ]
}

Response 200:
{
  "id": "uuid",
  "items": [...updated items...]
}

PATCH /api/orders/{id}/status
Request:
{
  "status": "COMPLETED"
}

Response 200:
{
  "id": "uuid",
  "status": "COMPLETED",
  "completedAt": "2025-12-23T11:00:00Z"
}
```

### 8.4 Payment Endpoints

```
POST /api/payments/intents
Request:
{
  "orderId": "uuid",
  "amount": 45.13,
  "paymentMethod": "CARD",
  "tipAmount": 9.00
}

Response 201:
{
  "intentId": "pi_123456789",
  "status": "PENDING",
  "amount": 54.13,
  "clientSecret": "pi_123456789_secret_xyz"
}

POST /api/payments/{intentId}/capture
Request:
{
  "terminalResponse": {
    "approved": true,
    "authCode": "123456",
    "last4": "4242"
  }
}

Response 200:
{
  "id": "uuid",
  "status": "CAPTURED",
  "amount": 54.13,
  "capturedAt": "2025-12-23T11:05:00Z"
}

POST /api/payments/{intentId}/refund
Request:
{
  "amount": 54.13,
  "reason": "Customer request"
}

Response 200:
{
  "id": "uuid",
  "status": "REFUNDED",
  "refundedAmount": 54.13
}
```

### 8.5 Table Endpoints

```
GET /api/tables
Query Params:
  - status: AVAILABLE | OCCUPIED | RESERVED
  - zone: string

Response 200:
[
  {
    "id": "uuid",
    "tableNumber": "T1",
    "capacity": 4,
    "zone": "Main Dining",
    "status": "AVAILABLE",
    "currentServer": null
  }
]

PATCH /api/tables/{id}/status
Request:
{
  "status": "OCCUPIED",
  "serverId": "uuid"
}

Response 200:
{
  "id": "uuid",
  "status": "OCCUPIED",
  "currentServer": {
    "id": "uuid",
    "fullName": "John Doe"
  }
}
```

---

## 9. Frontend Architecture

### 9.1 POS Terminal Structure

```javascript
// frontend/pos-terminal/src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { OrderProvider } from './context/OrderContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import OrderPage from './pages/OrderPage';
import TablesPage from './pages/TablesPage';
import { useOffline } from './hooks/useOffline';
import OfflineBanner from './components/OfflineBanner';

function App() {
  const isOnline = useOffline();
  
  return (
    <BrowserRouter>
      <AuthProvider>
        <OrderProvider>
          {!isOnline && <OfflineBanner />}
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
            <Route path="/order/:tableId?" element={<PrivateRoute><OrderPage /></PrivateRoute>} />
            <Route path="/tables" element={<PrivateRoute><TablesPage /></PrivateRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </OrderProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
```

### 9.2 Auth Context

```javascript
// frontend/pos-terminal/src/context/AuthContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    // Check for existing token
    const token = localStorage.getItem('authToken');
    const userStr = localStorage.getItem('user');
    
    if (token && userStr) {
      setUser(JSON.parse(userStr));
    }
    setLoading(false);
  }, []);
  
  const login = async (credentials) => {
    const response = await api.login(credentials);
    localStorage.setItem('authToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));
    setUser(response.user);
    return response;
  };
  
  const logout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
  };
  
  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
```

### 9.3 Order Context

```javascript
// frontend/pos-terminal/src/context/OrderContext.jsx
import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { api } from '../services/api';
import { cacheMenu, getMenuFromCache } from '../services/indexedDB';
import { syncService } from '../services/syncService';

const OrderContext = createContext(null);

const initialState = {
  currentOrder: null,
  menu: { items: [], categories: [] },
  cart: [],
  tables: [],
  loading: false,
  error: null
};

function orderReducer(state, action) {
  switch (action.type) {
    case 'SET_MENU':
      return { ...state, menu: action.payload };
    
    case 'SET_TABLES':
      return { ...state, tables: action.payload };
    
    case 'ADD_TO_CART':
      const existingIndex = state.cart.findIndex(
        item => item.menuItemId === action.payload.menuItemId &&
                JSON.stringify(item.modifiers) === JSON.stringify(action.payload.modifiers)
      );
      
      if (existingIndex >= 0) {
        const newCart = [...state.cart];
        newCart[existingIndex].quantity += action.payload.quantity;
        return { ...state, cart: newCart };
      }
      
      return { ...state, cart: [...state.cart, action.payload] };
    
    case 'REMOVE_FROM_CART':
      return { 
        ...state, 
        cart: state.cart.filter((_, index) => index !== action.payload) 
      };
    
    case 'UPDATE_CART_ITEM':
      const updatedCart = [...state.cart];
      updatedCart[action.payload.index] = {
        ...updatedCart[action.payload.index],
        ...action.payload.updates
      };
      return { ...state, cart: updatedCart };
    
    case 'CLEAR_CART':
      return { ...state, cart: [] };
    
    case 'SET_CURRENT_ORDER':
      return { ...state, currentOrder: action.payload };
    
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    
    default:
      return state;
  }
}

export function OrderProvider({ children }) {
  const [state, dispatch] = useReducer(orderReducer, initialState);
  
  useEffect(() => {
    loadMenu();
    loadTables();
    
    // Set up periodic sync
    const syncInterval = setInterval(() => {
      if (navigator.onLine) {
        syncService.syncPendingOrders();
      }
    }, 30000); // Sync every 30 seconds when online
    
    // Sync when coming back online
    const handleOnline = () => {
      console.log('Back online - syncing...');
      syncService.syncPendingOrders();
    };
    
    window.addEventListener('online', handleOnline);
    
    return () => {
      clearInterval(syncInterval);
      window.removeEventListener('online', handleOnline);
    };
  }, []);
  
  const loadMenu = async () => {
    try {
      // Try to load from API first
      if (navigator.onLine) {
        const menu = await api.getMenu();
        dispatch({ type: 'SET_MENU', payload: menu });
        
        // Cache for offline use
        await cacheMenu(menu.items, menu.categories);
      } else {
        // Load from cache if offline
        const cachedMenu = await getMenuFromCache();
        dispatch({ type: 'SET_MENU', payload: { 
          items: cachedMenu.items, 
          categories: cachedMenu.categories 
        }});
      }
    } catch (error) {
      console.error('Failed to load menu from API, trying cache:', error);
      // Fallback to cache
      const cachedMenu = await getMenuFromCache();
      if (cachedMenu.items.length > 0) {
        dispatch({ type: 'SET_MENU', payload: { 
          items: cachedMenu.items, 
          categories: cachedMenu.categories 
        }});
      } else {
        dispatch({ type: 'SET_ERROR', payload: 'Failed to load menu' });
      }
    }
  };
  
  const loadTables = async () => {
    try {
      if (navigator.onLine) {
        const tables = await api.getTables();
        dispatch({ type: 'SET_TABLES', payload: tables });
      }
    } catch (error) {
      console.error('Failed to load tables:', error);
    }
  };
  
  const addToCart = (item) => {
    dispatch({ type: 'ADD_TO_CART', payload: item });
  };
  
  const removeFromCart = (index) => {
    dispatch({ type: 'REMOVE_FROM_CART', payload: index });
  };
  
  const updateCartItem = (index, updates) => {
    dispatch({ type: 'UPDATE_CART_ITEM', payload: { index, updates } });
  };
  
  const clearCart = () => {
    dispatch({ type: 'CLEAR_CART' });
  };
  
  const submitOrder = async (orderData) => {
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const order = await api.createOrder({
        ...orderData,
        items: state.cart
      });
      
      dispatch({ type: 'SET_CURRENT_ORDER', payload: order });
      dispatch({ type: 'CLEAR_CART' });
      
      return order;
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: error.message });
      throw error;
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };
  
  return (
    <OrderContext.Provider value={{
      ...state,
      addToCart,
      removeFromCart,
      updateCartItem,
      clearCart,
      submitOrder,
      loadMenu,
      loadTables
    }}>
      {children}
    </OrderContext.Provider>
  );
}

export function useOrder() {
  const context = useContext(OrderContext);
  if (!context) {
    throw new Error('useOrder must be used within OrderProvider');
  }
  return context;
}
```

### 9.4 Key Components

```javascript
// frontend/pos-terminal/src/components/MenuGrid.jsx
import React from 'react';
import { useOrder } from '../context/OrderContext';

function MenuGrid({ categoryId }) {
  const { menu, addToCart } = useOrder();
  
  const items = categoryId 
    ? menu.items.filter(item => item.categoryId === categoryId)
    : menu.items;
  
  const handleItemClick = (item) => {
    // If item has modifiers, show modifier modal
    if (item.availableModifiers && item.availableModifiers.length > 0) {
      // Open modifier modal (implement this)
      return;
    }
    
    // Add directly to cart
    addToCart({
      menuItemId: item.id,
      name: item.name,
      quantity: 1,
      unitPrice: item.basePrice,
      modifiers: []
    });
  };
  
  return (
    <div className="grid grid-cols-3 gap-4 p-4">
      {items.map(item => (
        <button
          key={item.id}
          onClick={() => handleItemClick(item)}
          className="bg-white rounded-lg shadow p-4 hover:shadow-lg transition-shadow"
        >
          <div className="text-lg font-semibold">{item.name}</div>
          <div className="text-gray-600 text-sm mt-1">{item.description}</div>
          <div className="text-blue-600 font-bold mt-2">
            ${item.basePrice.toFixed(2)}
          </div>
        </button>
      ))}
    </div>
  );
}

export default MenuGrid;
```

```javascript
// frontend/pos-terminal/src/components/Cart.jsx
import React from 'react';
import { useOrder } from '../context/OrderContext';

function Cart({ onCheckout }) {
  const { cart, removeFromCart, updateCartItem } = useOrder();
  
  const calculateSubtotal = () => {
    return cart.reduce((sum, item) => {
      const modifierTotal = item.modifiers?.reduce(
        (modSum, mod) => modSum + (mod.priceDelta || 0), 0
      ) || 0;
      return sum + ((item.unitPrice + modifierTotal) * item.quantity);
    }, 0);
  };
  
  const subtotal = calculateSubtotal();
  const tax = subtotal * 0.05;
  const total = subtotal + tax;
  
  return (
    <div className="flex flex-col h-full bg-gray-50">
      <div className="p-4 bg-white border-b">
        <h2 className="text-xl font-bold">Current Order</h2>
      </div>
      
      <div className="flex-1 overflow-y-auto p-4 space-y-2">
        {cart.length === 0 ? (
          <div className="text-gray-400 text-center mt-8">
            Cart is empty
          </div>
        ) : (
          cart.map((item, index) => (
            <div key={index} className="bg-white rounded p-3 shadow">
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="font-semibold">{item.name}</div>
                  {item.modifiers && item.modifiers.length > 0 && (
                    <div className="text-sm text-gray-600 mt-1">
                      {item.modifiers.map(mod => mod.name).join(', ')}
                    </div>
                  )}
                  {item.specialInstructions && (
                    <div className="text-sm text-orange-600 mt-1">
                      Note: {item.specialInstructions}
                    </div>
                  )}
                </div>
                <button
                  onClick={() => removeFromCart(index)}
                  className="text-red-600 hover:text-red-800 ml-2"
                >
                  ✕
                </button>
              </div>
              
              <div className="flex justify-between items-center mt-2">
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateCartItem(index, { 
                      quantity: Math.max(1, item.quantity - 1) 
                    })}
                    className="w-8 h-8 bg-gray-200 rounded"
                  >
                    -
                  </button>
                  <span className="w-8 text-center">{item.quantity}</span>
                  <button
                    onClick={() => updateCartItem(index, { 
                      quantity: item.quantity + 1 
                    })}
                    className="w-8 h-8 bg-gray-200 rounded"
                  >
                    +
                  </button>
                </div>
                <div className="font-bold">
                  ${(item.unitPrice * item.quantity).toFixed(2)}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      
      <div className="p-4 bg-white border-t space-y-2">
        <div className="flex justify-between">
          <span>Subtotal:</span>
          <span>${subtotal.toFixed(2)}</span>
        </div>
        <div className="flex justify-between">
          <span>Tax (5%):</span>
          <span>${tax.toFixed(2)}</span>
        </div>
        <div className="flex justify-between text-xl font-bold">
          <span>Total:</span>
          <span>${total.toFixed(2)}</span>
        </div>
        
        <button
          onClick={onCheckout}
          disabled={cart.length === 0}
          className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold 
                     hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
        >
          Checkout
        </button>
      </div>
    </div>
  );
}

export default Cart;
```

```javascript
// frontend/pos-terminal/src/components/OfflineBanner.jsx
import React from 'react';

function OfflineBanner() {
  return (
    <div className="bg-yellow-500 text-white px-4 py-2 text-center font-semibold">
      ⚠️ Offline Mode - Orders will sync when connection is restored
    </div>
  );
}

export default OfflineBanner;
```

### 9.5 KDS Display App

```javascript
// frontend/kds-display/src/App.jsx
import React, { useState, useEffect } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

function App() {
  const [tickets, setTickets] = useState([]);
  const [stompClient, setStompClient] = useState(null);
  
  useEffect(() => {
    // Connect to WebSocket
    const socket = new SockJS('http://localhost:8090/ws');
    const client = Stomp.over(socket);
    
    client.connect({}, () => {
      console.log('Connected to WebSocket');
      
      // Subscribe to KDS updates
      client.subscribe('/topic/kds', (message) => {
        const ticket = JSON.parse(message.body);
        setTickets(prev => [...prev, ticket]);
      });
      
      // Subscribe to completed orders
      client.subscribe('/topic/kds/completed', (message) => {
        const orderId = message.body;
        setTickets(prev => prev.filter(t => t.orderId !== orderId));
      });
    });
    
    setStompClient(client);
    
    return () => {
      if (client) {
        client.disconnect();
      }
    };
  }, []);
  
  const handleBump = (orderId) => {
    // Send bump to server
    fetch(`http://localhost:8090/api/kds/orders/${orderId}/bump`, {
      method: 'POST'
    }).then(() => {
      setTickets(prev => prev.filter(t => t.orderId !== orderId));
    });
  };
  
  const getTicketAge = (firedAt) => {
    const minutes = Math.floor((Date.now() - new Date(firedAt).getTime()) / 60000);
    return minutes;
  };
  
  const getTicketColor = (minutes) => {
    if (minutes < 5) return 'bg-green-100 border-green-500';
    if (minutes < 10) return 'bg-yellow-100 border-yellow-500';
    return 'bg-red-100 border-red-500';
  };
  
  return (
    <div className="min-h-screen bg-gray-900 p-4">
      <div className="mb-4 text-white text-center">
        <h1 className="text-3xl font-bold">Kitchen Display System</h1>
        <div className="text-sm text-gray-400">
          {tickets.length} active orders
        </div>
      </div>
      
      <div className="grid grid-cols-3 gap-4">
        {tickets.map(ticket => {
          const age = getTicketAge(ticket.firedAt);
          return (
            <div
              key={ticket.orderId}
              className={`${getTicketColor(age)} border-4 rounded-lg p-4`}
            >
              <div className="flex justify-between items-start mb-3">
                <div>
                  <div className="text-2xl font-bold">{ticket.orderNumber}</div>
                  <div className="text-sm text-gray-600">{ticket.tableName}</div>
                </div>
                <div className="text-right">
                  <div className="text-3xl font-bold">{age}m</div>
                </div>
              </div>
              
              <div className="space-y-2 mb-4">
                {ticket.items.map((item, idx) => (
                  <div key={idx} className="border-b pb-2">
                    <div className="flex justify-between">
                      <span className="font-semibold">
                        {item.quantity}x {item.name}
                      </span>
                    </div>
                    {item.modifiers && item.modifiers.length > 0 && (
                      <div className="text-sm text-gray-600 ml-4">
                        {item.modifiers.map(mod => mod.name).join(', ')}
                      </div>
                    )}
                    {item.specialInstructions && (
                      <div className="text-sm text-red-600 ml-4 font-semibold">
                        ⚠️ {item.specialInstructions}
                      </div>
                    )}
                  </div>
                ))}
              </div>
              
              <button
                onClick={() => handleBump(ticket.orderId)}
                className="w-full bg-gray-800 text-white py-3 rounded-lg font-bold 
                           hover:bg-gray-700 text-xl"
              >
                BUMP
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default App;
```

---

## 10. Phase-by-Phase Implementation

### Week 1: Infrastructure & Authentication

**Day 1-2: Setup**
- [ ] Install Java 21, Node.js, Docker
- [ ] Create project structure
- [ ] Set up Docker Compose (PostgreSQL, Kafka, Zookeeper)
- [ ] Verify all containers running
- [ ] Create Spring Boot project with dependencies
- [ ] Create React projects (POS, KDS) with Vite

**Day 3-4: Database & Auth**
- [ ] Run Flyway migrations (V1 initial schema)
- [ ] Seed test data
- [ ] Implement JWT authentication
- [ ] Create login endpoint
- [ ] Test with Postman
- [ ] Build simple login UI

**Day 5: Kafka Setup**
- [ ] Configure Kafka in Spring Boot
- [ ] Create first event: OrderCreatedEvent
- [ ] Implement simple producer
- [ ] Implement simple consumer (just log events)
- [ ] Verify events flow through Kafka UI

**Deliverable:** Login works, database populated, Kafka ready

### Week 2: Core Order Flow

**Day 1-2: Menu API**
- [ ] Implement GET /menu endpoint
- [ ] Test menu retrieval
- [ ] Build menu grid UI component
- [ ] Display categories and items
- [ ] Add items to cart (local state only)

**Day 3-4: Order Creation**
- [ ] Implement POST /orders endpoint
- [ ] Add idempotency handling
- [ ] Publish OrderCreatedEvent to Kafka
- [ ] Test order creation via API
- [ ] Connect frontend cart to API
- [ ] Show success/error messages

**Day 5: Order Firing**
- [ ] Implement POST /orders/{id}/fire endpoint
- [ ] Publish OrderFiredEvent
- [ ] Test fire flow
- [ ] Add "Fire" button to UI

**Deliverable:** Can create order, add items, fire to kitchen

### Week 3: KDS & WebSocket

**Day 1-2: WebSocket Setup**
- [ ] Configure Spring WebSocket
- [ ] Implement WebSocket endpoints
- [ ] Test WebSocket connection with Postman

**Day 3-4: KDS Consumer**
- [ ] Create KDS event consumer
- [ ] Listen to order.fired events
- [ ] Push to WebSocket when order fired
- [ ] Test end-to-end: create → fire → appears on WebSocket

**Day 5: KDS UI**
- [ ] Build KDS React app
- [ ] Connect to WebSocket
- [ ] Display tickets in grid
- [ ] Implement bump functionality
- [ ] Add ticket aging/colors

**Deliverable:** Orders appear on KDS screen in real-time

### Week 4: Offline Mode - Phase 1

**Day 1-2: Service Worker & Cache**
- [ ] Set up Service Worker
- [ ] Cache static assets
- [ ] Test offline asset loading
- [ ] IndexedDB schema setup

**Day 3-4: Menu Caching**
- [ ] Cache menu data to IndexedDB
- [ ] Load from cache when offline
- [ ] Show offline indicator banner
- [ ] Test: disconnect wifi, menu still loads

**Day 5: Offline Queue**
- [ ] Implement offline order queue
- [ ] Queue orders when offline
- [ ] Show "queued" status in UI
- [ ] Test: create order offline, see it queued

**Deliverable:** POS works offline for reading data

### Week 5: Offline Mode - Phase 2

**Day 1-2: Sync Service**
- [ ] Implement sync service
- [ ] Sync queued orders on reconnect
- [ ] Handle sync success/failure
- [ ] Show sync progress in UI

**Day 3-4: Idempotency**
- [ ] Add idempotency keys to queued orders
- [ ] Implement backend idempotency check
- [ ] Test duplicate prevention
- [ ] Handle retry logic

**Day 5: Testing & Polish**
- [ ] Test various offline scenarios
- [ ] Test concurrent edits
- [ ] Add retry limits
- [ ] User feedback for failed syncs

**Deliverable:** Full offline write support with sync

### Week 6: Payments

**Day 1-2: Payment API**
- [ ] Implement POST /payments/intents
- [ ] Implement POST /payments/capture
- [ ] Mock terminal responses
- [ ] Test payment flow

**Day 3-4: Payment UI**
- [ ] Build checkout modal
- [ ] Mock payment terminal UI
- [ ] Handle payment success/failure
- [ ] Show receipt

**Day 5: Payment Events**
- [ ] Publish PaymentCapturedEvent
- [ ] Update order status on payment
- [ ] Test complete flow: order → fire → pay → complete

**Deliverable:** Mock payment flow complete

### Week 7: Tables & Reports

**Day 1-2: Table Management**
- [ ] Implement table endpoints
- [ ] Build floor plan UI
- [ ] Table status updates
- [ ] Assign server to table

**Day 3-4: Basic Reports**
- [ ] Create sales report endpoint
- [ ] Daily sales summary
- [ ] Product mix report
- [ ] Build simple report UI

**Day 5: Polish**
- [ ] Add table filters
- [ ] Export reports to CSV
- [ ] Date range selection

**Deliverable:** Table management + basic reporting

### Week 8+: Advanced Features

**Advanced Order Features:**
- [ ] Split bills
- [ ] Merge orders
- [ ] Course firing
- [ ] Void/refund flows

**Inventory (Optional):**
- [ ] Ingredient tracking
- [ ] Auto-depletion on fire
- [ ] Low stock alerts

**Analytics:**
- [ ] Analytics consumer
- [ ] Aggregate metrics
- [ ] Dashboard charts

**Hardening:**
- [ ] Error handling improvements
- [ ] Loading states
- [ ] Optimistic UI updates
- [ ] Comprehensive testing

---

## 11. Testing Strategy

### 11.1 Backend Testing

```java
// OrderServiceTest.java
@SpringBootTest
@Transactional
class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepo;
    
    @MockBean
    private OutboxEventPublisher eventPublisher;
    
    @Test
    void testCreateOrder_Success() {
        // Arrange
        OrderRequest request = new OrderRequest();
        request.setOrderType("DINE_IN");
        request.setTableId(UUID.randomUUID());
        // ... set items
        
        String idempotencyKey = UUID.randomUUID().toString();
        
        // Act
        Order order = orderService.createOrder(idempotencyKey, request);
        
        // Assert
        assertNotNull(order.getId());
        assertEquals("DRAFT", order.getStatus());
        assertEquals(request.getTableId(), order.getTableId());
        verify(eventPublisher).saveToOutbox(any(OrderCreatedEvent.class), eq("order.events"));
    }
    
    @Test
    void testCreateOrder_Idempotency() {
        // Create order first time
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequest request = new OrderRequest();
        // ... set fields
        
        Order order1 = orderService.createOrder(idempotencyKey, request);
        
        // Try to create same order again
        Order order2 = orderService.createOrder(idempotencyKey, request);
        
        // Should return same order
        assertEquals(order1.getId(), order2.getId());
        
        // Event should only be published once
        verify(eventPublisher, times(1)).saveToOutbox(any(), any());
    }
}
```

### 11.2 Frontend Testing

```javascript
// Cart.test.jsx
import { render, screen, fireEvent } from '@testing-library/react';
import { OrderProvider } from '../context/OrderContext';
import Cart from '../components/Cart';

describe('Cart Component', () => {
  test('displays empty message when cart is empty', () => {
    render(
      <OrderProvider>
        <Cart />
      </OrderProvider>
    );
    
    expect(screen.getByText('Cart is empty')).toBeInTheDocument();
  });
  
  test('adds item to cart', () => {
    // Implementation...
  });
  
  test('calculates total correctly', () => {
    // Implementation...
  });
});
```

### 11.3 Integration Testing

```java
// OrderIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"order.events"})
class OrderIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    
    @Test
    void testOrderCreationPublishesEvent() throws Exception {
        // Create order via API
        OrderRequest request = new OrderRequest();
        // ... set fields
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.set("Authorization", "Bearer " + getAuthToken());
        
        HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Order> response = restTemplate.postForEntity(
            "/api/orders",
            entity,
            Order.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        // Verify event was published (would need consumer verification)
    }
}
```

---

## 12. Hardware Emulation

### 12.1 POS Terminal Emulation

### 12.1 POS Terminal Emulation

**Using Browser DevTools:**
```
1. Open Chrome DevTools (F12)
2. Click "Toggle Device Toolbar" (Ctrl+Shift+M)
3. Select "iPad Pro" or custom dimensions (1024x768)
4. Use this as your POS terminal
5. Enable touch simulation for realistic interaction
```

**Multi-Terminal Testing:**
```
Terminal 1 (Server 1): Chrome Profile "Server1" → localhost:5173
Terminal 2 (Server 2): Chrome Profile "Server2" → localhost:5173
Terminal 3 (Manager):  Chrome Profile "Manager" → localhost:5173

Login with different users to simulate multiple terminals
```

### 12.2 KDS Screen Emulation

**Single Monitor Setup:**
```
Option 1: Separate Browser Window
- Open localhost:5174 (KDS app) in new browser window
- Press F11 for fullscreen
- Position on second monitor or use virtual desktop

Option 2: Split Screen
- Use Window management (Windows Key + Arrow)
- POS on left half, KDS on right half
```

**Dedicated KDS (Recommended):**
```
Hardware: Old laptop or Raspberry Pi 4
Display: Any HDMI monitor (1920x1080)
Setup:
  1. Install Chrome/Chromium
  2. Set to auto-start in kiosk mode:
     chromium-browser --kiosk http://POS_SERVER_IP:5174
  3. Disable screensaver/sleep
  4. Mount on kitchen wall or counter
```

### 12.3 Payment Terminal Emulation

**Mock Terminal UI:**

```javascript
// frontend/pos-terminal/src/components/MockPaymentTerminal.jsx
import React, { useState } from 'react';

function MockPaymentTerminal({ amount, onComplete, onCancel }) {
  const [processing, setProcessing] = useState(false);
  const [cardNumber, setCardNumber] = useState('');
  
  const handleApprove = async () => {
    setProcessing(true);
    
    // Simulate processing delay
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const response = {
      approved: true,
      authCode: Math.random().toString(36).substring(7).toUpperCase(),
      last4: cardNumber.slice(-4) || '4242',
      transactionId: crypto.randomUUID()
    };
    
    onComplete(response);
  };
  
  const handleDecline = async () => {
    setProcessing(true);
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    onComplete({
      approved: false,
      errorMessage: 'Card declined'
    });
  };
  
  return (
    <div className="bg-gray-800 text-white p-6 rounded-lg max-w-md mx-auto">
      <div className="text-center mb-6">
        <div className="text-sm text-gray-400">PAYMENT TERMINAL</div>
        <div className="text-3xl font-bold mt-2">
          ${amount.toFixed(2)}
        </div>
      </div>
      
      <div className="mb-6">
        <input
          type="text"
          placeholder="Card Number (for testing)"
          value={cardNumber}
          onChange={(e) => setCardNumber(e.target.value)}
          className="w-full bg-gray-700 px-4 py-3 rounded text-center text-2xl"
          maxLength="16"
        />
      </div>
      
      {processing ? (
        <div className="text-center py-8">
          <div className="animate-spin text-4xl mb-4">⟳</div>
          <div>Processing payment...</div>
        </div>
      ) : (
        <div className="space-y-3">
          <button
            onClick={handleApprove}
            className="w-full bg-green-600 py-4 rounded-lg text-xl font-bold 
                       hover:bg-green-700"
          >
            ✓ APPROVE
          </button>
          <button
            onClick={handleDecline}
            className="w-full bg-red-600 py-4 rounded-lg text-xl font-bold 
                       hover:bg-red-700"
          >
            ✗ DECLINE
          </button>
          <button
            onClick={onCancel}
            className="w-full bg-gray-600 py-4 rounded-lg font-bold 
                       hover:bg-gray-500"
          >
            CANCEL
          </button>
        </div>
      )}
      
      <div className="mt-4 text-xs text-gray-500 text-center">
        Mock Terminal - No real charges processed
      </div>
    </div>
  );
}

export default MockPaymentTerminal;
```

**Testing Different Scenarios:**
```javascript
// Test card numbers for different scenarios
const TEST_CARDS = {
  APPROVE: '4242424242424242',
  DECLINE: '4000000000000002',
  INSUFFICIENT: '4000000000009995',
  EXPIRED: '4000000000000069'
};

// Simulate different response times
const DELAYS = {
  FAST: 500,      // Quick approval
  NORMAL: 2000,   // Normal processing
  SLOW: 5000      // Slow/timeout scenario
};
```

### 12.4 Receipt Printer Emulation

**PDF Receipt Generation:**

```javascript
// frontend/pos-terminal/src/services/receiptService.js
import jsPDF from 'jspdf';

export function generateReceipt(order, payment) {
  const doc = new jsPDF({
    format: [80, 200], // 80mm thermal printer width
    unit: 'mm'
  });
  
  // Restaurant header
  doc.setFontSize(16);
  doc.text('YOUR RESTAURANT', 40, 10, { align: 'center' });
  doc.setFontSize(10);
  doc.text('123 Main St, Calgary, AB', 40, 16, { align: 'center' });
  doc.text('Tel: (403) 555-0123', 40, 21, { align: 'center' });
  
  // Separator
  doc.text('--------------------------------', 5, 26);
  
  // Order details
  doc.setFontSize(12);
  doc.text(`Order #: ${order.orderNumber}`, 5, 32);
  doc.text(`Date: ${new Date(order.createdAt).toLocaleString()}`, 5, 38);
  doc.text(`Server: ${order.serverName}`, 5, 44);
  if (order.tableName) {
    doc.text(`Table: ${order.tableName}`, 5, 50);
  }
  
  // Items
  doc.text('--------------------------------', 5, 56);
  let yPos = 62;
  
  order.items.forEach(item => {
    doc.text(`${item.quantity}x ${item.name}`, 5, yPos);
    doc.text(`${(item.unitPrice * item.quantity).toFixed(2)}`, 65, yPos);
    yPos += 6;
    
    if (item.modifiers && item.modifiers.length > 0) {
      item.modifiers.forEach(mod => {
        doc.setFontSize(9);
        doc.text(`  + ${mod.name}`, 8, yPos);
        yPos += 5;
      });
      doc.setFontSize(12);
    }
  });
  
  // Totals
  doc.text('--------------------------------', 5, yPos);
  yPos += 6;
  doc.text('Subtotal:', 5, yPos);
  doc.text(`${order.subtotal.toFixed(2)}`, 65, yPos);
  yPos += 6;
  doc.text('Tax:', 5, yPos);
  doc.text(`${order.taxAmount.toFixed(2)}`, 65, yPos);
  yPos += 6;
  
  if (order.tipAmount > 0) {
    doc.text('Tip:', 5, yPos);
    doc.text(`${order.tipAmount.toFixed(2)}`, 65, yPos);
    yPos += 6;
  }
  
  doc.setFontSize(14);
  doc.text('TOTAL:', 5, yPos);
  doc.text(`${order.total.toFixed(2)}`, 65, yPos);
  
  yPos += 8;
  doc.setFontSize(10);
  doc.text('--------------------------------', 5, yPos);
  yPos += 6;
  doc.text(`Payment: ${payment.paymentMethod}`, 5, yPos);
  yPos += 5;
  doc.text(`Card: **** **** **** ${payment.last4}`, 5, yPos);
  yPos += 5;
  doc.text(`Auth: ${payment.authCode}`, 5, yPos);
  
  // Footer
  yPos += 10;
  doc.text('Thank you for dining with us!', 40, yPos, { align: 'center' });
  
  // Save or open
  doc.save(`receipt-${order.orderNumber}.pdf`);
}

// Alternative: Print to browser
export function printReceipt(order, payment) {
  const printWindow = window.open('', '', 'width=300,height=600');
  printWindow.document.write(`
    <html>
      <head>
        <title>Receipt - ${order.orderNumber}</title>
        <style>
          body {
            font-family: monospace;
            width: 80mm;
            margin: 0 auto;
            padding: 10mm;
          }
          .center { text-align: center; }
          .right { text-align: right; }
          .line { border-bottom: 1px dashed #000; margin: 5px 0; }
          .item { display: flex; justify-content: space-between; }
          .total { font-size: 1.2em; font-weight: bold; }
        </style>
      </head>
      <body>
        <div class="center">
          <h2>YOUR RESTAURANT</h2>
          <p>123 Main St, Calgary, AB</p>
          <p>Tel: (403) 555-0123</p>
        </div>
        <div class="line"></div>
        <p>Order #: ${order.orderNumber}</p>
        <p>Date: ${new Date().toLocaleString()}</p>
        <p>Server: ${order.serverName}</p>
        <div class="line"></div>
        ${order.items.map(item => `
          <div class="item">
            <span>${item.quantity}x ${item.name}</span>
            <span>${(item.unitPrice * item.quantity).toFixed(2)}</span>
          </div>
        `).join('')}
        <div class="line"></div>
        <div class="item">
          <span>Subtotal:</span>
          <span>${order.subtotal.toFixed(2)}</span>
        </div>
        <div class="item">
          <span>Tax:</span>
          <span>${order.taxAmount.toFixed(2)}</span>
        </div>
        <div class="item total">
          <span>TOTAL:</span>
          <span>${order.total.toFixed(2)}</span>
        </div>
        <div class="line"></div>
        <p class="center">Thank you!</p>
      </body>
    </html>
  `);
  
  printWindow.document.close();
  printWindow.print();
}
```

### 12.5 Testing Workflow

**Daily Testing Routine:**

```bash
# Morning Setup
1. Start infrastructure:
   cd docker && docker-compose up -d

2. Verify services:
   docker ps  # All containers healthy
   curl http://localhost:8090/actuator/health  # Backend healthy

3. Start frontend apps:
   # Terminal 1: POS
   cd frontend/pos-terminal && npm run dev
   
   # Terminal 2: KDS
   cd frontend/kds-display && npm run dev

4. Open browser windows:
   - POS: http://localhost:5173 (Chrome Profile "Server1")
   - KDS: http://localhost:5174 (Chrome Profile "Kitchen")

# Test Scenario 1: Happy Path
1. Login as server
2. Select table
3. Add items to cart
4. Add modifiers
5. Fire order → verify appears on KDS
6. Bump order on KDS
7. Process payment
8. Print receipt

# Test Scenario 2: Offline Mode
1. Create order while online
2. Disconnect wifi
3. Try to create another order → should queue
4. Reconnect wifi
5. Verify queued order syncs automatically

# Test Scenario 3: Multiple Terminals
1. Login as Server1 on Chrome Profile 1
2. Login as Server2 on Chrome Profile 2
3. Both create orders for different tables
4. Verify both appear on KDS
5. Verify no conflicts
```

---

## 13. Backend Core Implementation

### 13.1 Order Service (Complete)

```java
// IdempotencyService.java
@Service
@Slf4j
public class IdempotencyService {
    
    @Autowired
    private IdempotencyRecordRepository idempotencyRepo;
    
    @Autowired
    private OrderRepository orderRepo;
    
    public Optional<Order> checkIdempotency(String idempotencyKey, String resourceType) {
        Optional<IdempotencyRecord> record = idempotencyRepo.findById(idempotencyKey);
        
        if (record.isPresent()) {
            IdempotencyRecord idem = record.get();
            
            // Check if expired
            if (idem.getExpiresAt().isBefore(Instant.now())) {
                idempotencyRepo.delete(idem);
                return Optional.empty();
            }
            
            // Return cached resource
            if ("Order".equals(resourceType)) {
                return orderRepo.findById(idem.getResourceId());
            }
        }
        
        return Optional.empty();
    }
    
    public void cacheResult(String idempotencyKey, String resourceType, UUID resourceId) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        
        idempotencyRepo.save(record);
    }
}

// OrderService.java
@Service
@Slf4j
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepo;
    
    @Autowired
    private MenuItemRepository menuItemRepo;
    
    @Autowired
    private OutboxEventPublisher eventPublisher;
    
    @Autowired
    private IdempotencyService idempotencyService;
    
    public Order createOrder(String idempotencyKey, OrderRequest request) {
        // Check idempotency
        Optional<Order> existing = idempotencyService.checkIdempotency(
            idempotencyKey, "Order"
        );
        
        if (existing.isPresent()) {
            log.info("Returning cached order for idempotency key: {}", idempotencyKey);
            return existing.get();
        }
        
        // Validate items exist and are active
        validateOrderItems(request.getItems());
        
        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(request.getOrderType());
        order.setStatus(OrderStatus.DRAFT);
        order.setTableId(request.getTableId());
        order.setServerId(request.getServerId());
        order.setGuestCount(request.getGuestCount());
        order.setIdempotencyKey(idempotencyKey);
        
        // Add items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = createOrderItem(itemRequest);
            item.setOrder(order);
            order.getItems().add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        
        // Calculate totals
        order.setSubtotal(subtotal);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));
        order.setTaxAmount(tax);
        order.setTotal(subtotal.add(tax));
        
        // Save
        order = orderRepo.save(order);
        
        // Publish event
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        eventPublisher.saveToOutbox(event, "order.events");
        
        // Cache for idempotency
        idempotencyService.cacheResult(idempotencyKey, "Order", order.getId());
        
        log.info("Created order: {}", order.getOrderNumber());
        return order;
    }
    
    public Order findById(UUID id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
    
    public Page<Order> findOrders(OrderStatus status, UUID tableId, 
                                   LocalDate from, LocalDate to, Pageable pageable) {
        // Build query with filters
        Specification<Order> spec = Specification.where(null);
        
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        
        if (tableId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tableId"), tableId));
        }
        
        if (from != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }
        
        if (to != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
        }
        
        return orderRepo.findAll(spec, pageable);
    }
    
    public Order fireOrder(UUID orderId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.DRAFT && 
            order.getStatus() != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Order cannot be fired in current state");
        }
        
        order.setStatus(OrderStatus.FIRED);
        order.setFiredAt(Instant.now());
        
        // Update item statuses
        order.getItems().forEach(item -> item.setStatus(OrderItemStatus.FIRED));
        
        order = orderRepo.save(order);
        
        // Publish event
        OrderFiredEvent event = new OrderFiredEvent(order);
        eventPublisher.saveToOutbox(event, "order.events");
        
        log.info("Fired order: {}", order.getOrderNumber());
        return order;
    }
    
    public Order addItems(UUID orderId, List<OrderItemRequest> itemRequests) {
        Order order = findById(orderId);
        
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to order in current state");
        }
        
        for (OrderItemRequest itemRequest : itemRequests) {
            OrderItem item = createOrderItem(itemRequest);
            item.setOrder(order);
            order.getItems().add(item);
        }
        
        // Recalculate totals
        recalculateTotals(order);
        
        return orderRepo.save(order);
    }
    
    public Order updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        order.setStatus(newStatus);
        
        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(Instant.now());
        }
        
        return orderRepo.save(order);
    }
    
    public Order completeOrder(UUID orderId) {
        Order order = orderRepo.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        order = orderRepo.save(order);
        
        // Publish event
        OrderCompletedEvent event = new OrderCompletedEvent(order);
        eventPublisher.saveToOutbox(event, "order.events");
        
        log.info("Completed order: {}", order.getOrderNumber());
        return order;
    }
    
    private void validateOrderItems(List<OrderItemRequest> items) {
        List<UUID> menuItemIds = items.stream()
            .map(OrderItemRequest::getMenuItemId)
            .collect(Collectors.toList());
        
        List<MenuItem> menuItems = menuItemRepo.findAllById(menuItemIds);
        
        if (menuItems.size() != menuItemIds.size()) {
            throw new ValidationException("One or more menu items not found");
        }
        
        boolean allActive = menuItems.stream().allMatch(MenuItem::isActive);
        if (!allActive) {
            throw new ValidationException("One or more menu items are not active");
        }
    }
    
    private OrderItem createOrderItem(OrderItemRequest request) {
        MenuItem menuItem = menuItemRepo.findById(request.getMenuItemId())
            .orElseThrow(() -> new NotFoundException("Menu item not found"));
        
        OrderItem item = new OrderItem();
        item.setMenuItemId(request.getMenuItemId());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(menuItem.getBasePrice());
        item.setSpecialInstructions(request.getSpecialInstructions());
        item.setStatus(OrderItemStatus.PENDING);
        
        // Calculate subtotal (including modifiers)
        BigDecimal itemTotal = menuItem.getBasePrice()
            .multiply(new BigDecimal(request.getQuantity()));
        
        if (request.getModifiers() != null) {
            for (ModifierRequest modReq : request.getModifiers()) {
                OrderItemModifier modifier = new OrderItemModifier();
                modifier.setModifierId(modReq.getModifierId());
                modifier.setPriceDelta(modReq.getPriceDelta());
                modifier.setOrderItem(item);
                item.getModifiers().add(modifier);
                
                itemTotal = itemTotal.add(
                    modReq.getPriceDelta().multiply(new BigDecimal(request.getQuantity()))
                );
            }
        }
        
        item.setSubtotal(itemTotal);
        return item;
    }
    
    private void recalculateTotals(Order order) {
        BigDecimal subtotal = order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setSubtotal(subtotal);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));
        order.setTaxAmount(tax);
        order.setTotal(subtotal.add(tax));
    }
    
    private String generateOrderNumber() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        long count = orderRepo.countByCreatedAtBetween(
            today.atStartOfDay(),
            today.plusDays(1).atStartOfDay()
        );
        
        return String.format("ORD-%s-%03d", dateStr, count + 1);
    }
}
```

### 13.2 Order Controller

```java
// OrderController.java
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid OrderRequest request) {
        
        log.info("Creating order with idempotency key: {}", idempotencyKey);
        Order order = orderService.createOrder(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping
    public ResponseEntity<Page<Order>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<Order> orders = orderService.findOrders(status, tableId, from, to, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{id}/fire")
    public ResponseEntity<Order> fireOrder(@PathVariable UUID id) {
        Order order = orderService.fireOrder(id);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{id}/items")
    public ResponseEntity<Order> addItems(
            @PathVariable UUID id,
            @RequestBody @Valid AddItemsRequest request) {
        Order order = orderService.addItems(id, request.getItems());
        return ResponseEntity.ok(order);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStatusRequest request) {
        Order order = orderService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(order);
    }
}
```

### 13.3 WebSocket Configuration

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

---

## 14. Deployment & Operations

### 14.1 Production Checklist

**Before Deployment:**
```
[ ] All tests passing
[ ] Environment variables configured
[ ] Database migrations reviewed
[ ] Kafka topics created with proper partitions
[ ] SSL certificates installed
[ ] Backup strategy in place
[ ] Monitoring configured
[ ] Error alerting set up
[ ] Load testing completed
[ ] Security audit done
```

### 14.2 Environment Configuration

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    producer:
      acks: all
      retries: 3
    consumer:
      auto-offset-reset: earliest
  
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 3600000  # 1 hour

logging:
  level:
    com.yourname.pos: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### 14.3 Monitoring

**Key Metrics to Track:**
```
- Order creation rate (orders/minute)
- Average order processing time
- Kafka consumer lag
- Database connection pool usage
- API response times (p50, p95, p99)
- Error rates by endpoint
- Offline queue size per terminal
- Payment success/failure rates
```

---

## 15. Next Steps & Resources

### 15.1 Immediate Next Actions

1. **Set up development environment** (Day 1)
   - Install all prerequisites
   - Run docker-compose up
   - Verify all services running

2. **Initialize backend project** (Day 1-2)
   - Create Spring Boot project
   - Run first Flyway migration
   - Test database connection

3. **Build first endpoint** (Day 2-3)
   - Implement /auth/login
   - Test with Postman
   - Create simple login UI

4. **Kafka proof of concept** (Day 3-4)
   - Publish first event
   - Consume and log event
   - Verify in Kafka UI

### 15.2 Learning Resources

**Spring Boot & Kafka:**
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Kafka: https://spring.io/projects/spring-kafka
- Kafka Quickstart: https://kafka.apache.org/quickstart

**React & Offline:**
- React Docs: https://react.dev
- IndexedDB Guide: https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API
- Service Workers: https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API

**Architecture Patterns:**
- Event Sourcing: https://martinfowler.com/eaaDev/EventSourcing.html
- Outbox Pattern: https://microservices.io/patterns/data/transactional-outbox.html
- Idempotency: https://stripe.com/docs/api/idempotent_requests

### 15.3 Common Pitfalls to Avoid

1. **Don't skip idempotency** - It's critical for offline mode
2. **Don't use localStorage for orders** - Use IndexedDB for structured data
3. **Don't forget WebSocket reconnection logic** - KDS needs auto-reconnect
4. **Don't ignore Kafka consumer lag** - Monitor and alert on lag
5. **Don't skip database indexing** - Add indexes on foreign keys and query columns
6. **Don't hardcode configuration** - Use environment variables
7. **Don't forget CORS configuration** - Configure properly for frontend access

### 15.4 Future Enhancements

**Phase 2 Features:**
- Split bill functionality
- Course firing (appetizers → entrees → desserts)
- Inventory auto-depletion
- Reservation system
- Employee time tracking
- Advanced reporting dashboard

**Phase 3 Features:**
- Multi-location support
- Customer loyalty program
- Online ordering integration
- Mobile app for servers
- Analytics and forecasting
- Integration with accounting software

---

## 16. Conclusion

This documentation provides a complete blueprint for building a production-grade restaurant POS system as a learning project. The architecture emphasizes:

✅ **Event-Driven Design** - Kafka for system decoupling
✅ **Offline-First** - Full offline support with sync
✅ **Idempotency** - Reliable duplicate prevention
✅ **Real-Time Updates** - WebSocket for KDS
✅ **Modern Stack** - Spring Boot + React + PostgreSQL
✅ **Testability** - Proper testing strategies
✅ **Scalability** - Ready for multi-location

**Remember**: This is a learning project. Don't try to build everything at once. Follow the phased approach, celebrate small wins, and iterate based on what you learn.

Start with Week 1 and get the basic flow working. Once you can create an order and see it on the KDS, you'll have momentum to keep going.

Good luck! 🚀

---

**Document Version**: 1.0  
**Last Updated**: December 23, 2025  
**Author**: AI Assistant for Changbeom Noh
