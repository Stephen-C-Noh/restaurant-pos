-- Initial database setup
-- This script runs when the PostgreSQL container is first created

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE restaurant_pos TO posuser;

-- Note: Tables will be created by Flyway migrations in the Spring Boot application
