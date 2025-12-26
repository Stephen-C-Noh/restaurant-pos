# Restaurant POS - Project Structure

## Directory Tree

```
restaurant-pos/
├── README.md                              # Project overview
├── SETUP.md                              # Quick setup guide
├── Restaurant_POS_Implementation_Guide.md # Complete technical documentation
├── .gitignore                            # Git ignore rules
│
├── docker/                               # Infrastructure configuration
│   ├── docker-compose.yml               # All services (PostgreSQL, Kafka, etc.)
│   ├── .env                             # Docker environment variables
│   └── postgres/
│       └── init.sql                     # Database initialization
│
├── backend/                             # Spring Boot application
│   ├── pom.xml                         # Maven dependencies
│   ├── .mvn/
│   │   └── wrapper/
│   │       └── maven-wrapper.properties
│   └── src/
│       ├── main/
│       │   ├── java/com/restaurant/pos/
│       │   │   ├── PosApplication.java          # Main application class
│       │   │   ├── config/                      # Configuration classes
│       │   │   │   ├── SecurityConfig.java      # Spring Security setup
│       │   │   │   ├── KafkaConfig.java        # Kafka producer/consumer
│       │   │   │   └── WebSocketConfig.java    # WebSocket for KDS
│       │   │   ├── auth/                        # Authentication (TODO)
│       │   │   ├── order/                       # Order management (TODO)
│       │   │   ├── menu/                        # Menu service (TODO)
│       │   │   ├── payment/                     # Payment processing (TODO)
│       │   │   ├── kds/                         # Kitchen Display (TODO)
│       │   │   ├── common/                      # Shared utilities (TODO)
│       │   │   └── events/                      # Event classes
│       │   │       └── DomainEvent.java        # Base event class
│       │   └── resources/
│       │       ├── application.yml              # Application configuration
│       │       └── db/migration/                # Flyway migrations
│       │           ├── V1__initial_schema.sql   # Create tables
│       │           └── V2__add_idempotency.sql # Add offline support
│       └── test/                                # Test classes (TODO)
│
└── frontend/                            # React application
    ├── package.json                     # NPM dependencies
    ├── vite.config.js                  # Vite configuration
    ├── tailwind.config.js              # Tailwind CSS config
    ├── postcss.config.js               # PostCSS config
    ├── index.html                      # HTML entry point
    ├── public/                         # Static assets
    └── src/
        ├── main.jsx                    # React entry point
        ├── App.jsx                     # Main app component
        ├── index.css                   # Global styles + Tailwind
        ├── components/                 # React components
        │   ├── POSTerminal.jsx        # POS interface (basic UI)
        │   ├── KitchenDisplay.jsx     # KDS interface (basic UI)
        │   ├── AdminDashboard.jsx     # Admin panel (basic UI)
        │   └── Login.jsx              # Login page (basic UI)
        ├── services/                   # API services (TODO)
        ├── hooks/                      # Custom React hooks (TODO)
        ├── contexts/                   # React contexts (TODO)
        └── utils/                      # Utility functions (TODO)
```

## What's Included

### ✅ Complete Infrastructure
- Docker Compose with PostgreSQL, Kafka, Zookeeper, pgAdmin, Kafka UI
- Database migrations (Flyway) with complete schema
- Full configuration files

### ✅ Backend Foundation
- Spring Boot 3.4 with Java 21
- Security, WebSocket, Kafka configurations
- Database schema with idempotency support
- Base event architecture

### ✅ Frontend Foundation
- React 18 + Vite
- TailwindCSS styling
- Basic UI components for POS, KDS, Admin
- Routing setup

### ⏳ To Be Implemented (Following the Guide)

The implementation guide provides detailed code for:
- Authentication & Authorization
- Order Service (CRUD, state management)
- Menu Service
- Payment Service
- KDS Consumer (Kafka → WebSocket)
- Offline mode (IndexedDB + Service Workers)
- Full UI implementations

## Current State

**What works now:**
1. ✅ All infrastructure services start up
2. ✅ Database schema is created automatically
3. ✅ Backend starts and connects to database
4. ✅ Frontend starts with basic navigation
5. ✅ Configuration is production-ready

**What needs implementation:**
1. ⏳ REST API endpoints
2. ⏳ Business logic services
3. ⏳ Kafka event producers/consumers
4. ⏳ Full frontend functionality
5. ⏳ Authentication flow
6. ⏳ Offline mode
7. ⏳ Tests

## Key Files to Start With

For **backend** development:
1. `backend/src/main/java/com/restaurant/pos/order/OrderController.java` (create this)
2. `backend/src/main/java/com/restaurant/pos/order/OrderService.java` (create this)
3. `backend/src/main/java/com/restaurant/pos/auth/AuthController.java` (create this)

For **frontend** development:
1. `frontend/src/services/api.js` (create this for API calls)
2. `frontend/src/components/POSTerminal.jsx` (enhance existing)
3. `frontend/src/hooks/useOfflineSync.js` (create this)

## Next Steps

1. **Read SETUP.md** to get everything running
2. **Read Restaurant_POS_Implementation_Guide.md** sections 10-13 for implementation details
3. **Start with Phase 1** (Week 1): Basic order creation
4. **Follow the phased approach** - don't try to build everything at once

## Configuration

### Environment Variables

**Backend** (`backend/src/main/resources/application.yml`):
- Database connection
- Kafka brokers
- JWT secret
- CORS origins

**Frontend** (`frontend/.env` - create if needed):
- API base URL
- Feature flags

**Docker** (`docker/.env`):
- Database credentials
- Service ports

## Technology Choices Explained

| Technology | Why? |
|------------|------|
| Java 21 | Latest LTS, virtual threads, pattern matching |
| Spring Boot 3.4 | Industry standard, excellent ecosystem |
| PostgreSQL 16 | ACID compliance, JSONB, great for transactional data |
| Kafka | Event-driven architecture, scalable messaging |
| React 18 | Modern, component-based, excellent ecosystem |
| Vite | Fast dev server, optimized production builds |
| TailwindCSS | Utility-first, fast development, consistent design |
| IndexedDB | Browser database for offline storage |

## Getting Help

- **Setup issues:** See SETUP.md
- **Architecture questions:** See Restaurant_POS_Implementation_Guide.md
- **Code examples:** Check sections 13 (Backend) and 9 (Frontend) in the guide
- **Database schema:** See `backend/src/main/resources/db/migration/V1__initial_schema.sql`

Happy coding! 🎉
