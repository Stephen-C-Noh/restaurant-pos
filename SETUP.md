# Restaurant POS - Setup Guide

## Prerequisites

Make sure you have the following installed:
- ✅ Java JDK 21 (verify: `java -version`)
- ✅ Node.js 20.x LTS (verify: `node -v`)
- ✅ Docker Desktop (verify: `docker --version`)
- ✅ Maven 3.8+ (verify: `mvn -v`)

## Quick Start (5 Minutes)

### Step 1: Start Infrastructure

```bash
cd docker
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Kafka on port 9092
- Kafka UI on port 8080
- pgAdmin on port 5050

**Verify services are running:**
```bash
docker-compose ps
```

All services should show "Up" status.

### Step 2: Start Backend

```bash
cd backend

# First time only: package the application
./mvn clean package -DskipTests

# Start the Spring Boot application
./mvn spring-boot:run
```

**Backend will be available at:** http://localhost:8090

**Check health:** http://localhost:8090/actuator/health

### Step 3: Start Frontend

```bash
cd frontend

# First time only: install dependencies
npm install

# Start development server
npm run dev
```

**Frontend will be available at:** http://localhost:5173

## Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | N/A |
| Backend API | http://localhost:8090/api | N/A |
| pgAdmin | http://localhost:5050 | admin@restaurant.com / admin123 |
| Kafka UI | http://localhost:8080 | N/A |

## Database Connection (pgAdmin)

1. Open http://localhost:5050
2. Login with: admin@restaurant.com / admin123
3. Add New Server:
   - Name: `POS Database`
   - Host: `postgres`
   - Port: `5432`
   - Database: `restaurant_pos`
   - Username: `posuser`
   - Password: `pospass123`

## Verify Everything Works

### 1. Check Database
```bash
docker exec -it pos-postgres psql -U posuser -d restaurant_pos -c "\dt"
```

You should see tables like: users, orders, menu_items, etc.

### 2. Check Kafka Topics
Visit http://localhost:8080 and you should see Kafka topics.

### 3. Test Backend API
```bash
curl http://localhost:8090/actuator/health
```

Should return: `{"status":"UP"}`

### 4. Test Frontend
Open http://localhost:5173 in your browser. You should see the login page.

## Common Issues

### Issue: Port already in use
**Solution:** Stop the conflicting service or change the port in docker-compose.yml

### Issue: Cannot connect to Docker daemon
**Solution:** Make sure Docker Desktop is running

### Issue: Database connection failed
**Solution:** 
```bash
cd docker
docker-compose down
docker-compose up -d
```

Wait 30 seconds for PostgreSQL to fully start, then restart the backend.

### Issue: Frontend can't connect to backend
**Solution:** Check that backend is running on port 8090. Check browser console for CORS errors.

## Next Steps

1. ✅ Run through this setup guide
2. 📖 Read the [Restaurant_POS_Implementation_Guide.md](./Restaurant_POS_Implementation_Guide.md)
3. 🏗️ Start building! Follow the phased implementation plan
4. 🧪 Write tests as you go

## Stopping Services

```bash
# Stop backend: Ctrl+C in terminal

# Stop frontend: Ctrl+C in terminal

# Stop Docker services
cd docker
docker-compose down

# To remove volumes (fresh start)
docker-compose down -v
```

## Development Workflow

1. Make code changes
2. Backend auto-reloads (Spring DevTools)
3. Frontend auto-reloads (Vite HMR)
4. Test manually or with unit tests
5. Commit when feature works

## Need Help?

- Check logs: `docker-compose logs -f [service-name]`
- Backend logs: In the terminal where you ran `./mvnw spring-boot:run`
- Frontend logs: Browser developer console
- Database issues: Check pgAdmin or run SQL directly

Happy coding! 🚀
