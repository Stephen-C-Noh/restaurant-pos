# Restaurant POS System

A full-featured restaurant Point of Sale system built with Spring Boot and React, featuring event-driven architecture, offline-first capabilities, and real-time kitchen display integration.

## 🚀 Features

- **Order Management**: Complete order lifecycle from creation to completion
- **Kitchen Display System (KDS)**: Real-time order updates via WebSocket
- **Offline Mode**: Full offline support with automatic sync when back online
- **Event-Driven Architecture**: Kafka-based messaging for system decoupling
- **Payment Processing**: Mock payment terminal with proper transaction flow
- **Table Management**: Floor plan, table states, and server assignments
- **Real-time Updates**: WebSocket integration for live order status

## 🏗️ Architecture

```
Frontend (React PWA) ←→ REST API/WebSocket ←→ Spring Boot Backend ←→ PostgreSQL
                                                      ↓
                                                Apache Kafka
                                                      ↓
                                    Consumers (KDS, Analytics, Inventory)
```

## 📋 Prerequisites

- Java JDK 21
- Node.js 20.x LTS
- Docker Desktop
- Git

## 🚀 Quick Start

### 1. Start Infrastructure (Database + Kafka)

```bash
cd docker
docker-compose up -d
```

### 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend will be available at: http://localhost:8090

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at: http://localhost:5173

## 📚 Documentation

See [Restaurant_POS_Implementation_Guide.md](./Restaurant_POS_Implementation_Guide.md) for complete implementation details.

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot 3.4.x
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Flyway (Database Migrations)

### Frontend
- React 18 + Vite
- TailwindCSS
- IndexedDB (Offline Storage)
- WebSocket (Real-time Updates)
- Service Workers (PWA)

### Infrastructure
- PostgreSQL 16
- Apache Kafka 3.6
- Docker & Docker Compose

## 📖 Project Structure

```
restaurant-pos/
├── docker/                 # Docker configuration
├── backend/               # Spring Boot application
├── frontend/              # React application
└── Restaurant_POS_Implementation_Guide.md
```

## 🧪 Testing

### Backend Tests
```bash
cd backend
./mvnw test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 📝 Development Workflow

1. **Phase 1**: Basic order creation and display
2. **Phase 2**: Kitchen Display System integration
3. **Phase 3**: Payment processing
4. **Phase 4**: Offline mode and sync
5. **Phase 5**: Advanced features (reporting, analytics)

## 🔧 Configuration

Environment variables can be configured in:
- Backend: `backend/src/main/resources/application.yml`
- Frontend: `frontend/.env`
- Docker: `docker/.env`

## 📄 License

This is a learning project for educational purposes.

## 👤 Author

Created as a portfolio project to demonstrate production-grade software engineering practices.
