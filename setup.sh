#!/bin/bash

# Restaurant POS Project Setup Script
# This script sets up the development environment for the Restaurant POS system

set -e  # Exit on error

echo "=========================================="
echo "Restaurant POS - Development Setup"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if Docker is running
echo "Checking Docker..."
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop and run this script again."
    exit 1
fi
print_success "Docker is running"

# Check if Docker Compose is available
echo "Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    print_error "Docker Compose is not available"
    exit 1
fi
print_success "Docker Compose is available"

# Check Java installation
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install JDK 17 or 21."
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
print_success "Java $JAVA_VERSION is installed"

# Check Maven installation (if using Maven)
if [ -f "pom.xml" ]; then
    echo "Checking Maven installation..."
    if ! command -v mvn &> /dev/null; then
        print_info "Maven not found in PATH. IntelliJ will use embedded Maven."
    else
        MVN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
        print_success "Maven $MVN_VERSION is installed"
    fi
fi

# Check Gradle installation (if using Gradle)
if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    echo "Checking Gradle..."
    if [ -f "gradlew" ]; then
        print_success "Gradle wrapper found"
        chmod +x gradlew
    elif ! command -v gradle &> /dev/null; then
        print_info "Gradle not found. Please use the Gradle wrapper or install Gradle."
    else
        GRADLE_VERSION=$(gradle -version | grep "Gradle" | cut -d' ' -f2)
        print_success "Gradle $GRADLE_VERSION is installed"
    fi
fi

# Set up environment variables
echo ""
echo "Setting up environment variables..."
if [ -f ".env.example" ]; then
    if [ ! -f ".env" ]; then
        cp .env.example .env
        print_success "Created .env file from .env.example"
        print_info "Please review and update .env file with your configuration"
    else
        print_info ".env file already exists"
    fi
fi

# Start Docker containers
echo ""
echo "Starting Docker containers..."
if [ -f "docker-compose.yml" ]; then
    print_info "Running docker-compose up -d..."
    docker-compose up -d
    print_success "Docker containers started"
    
    echo ""
    echo "Waiting for services to be ready..."
    sleep 5
    
    # Show running containers
    echo ""
    echo "Running containers:"
    docker-compose ps
else
    print_info "No docker-compose.yml found. Skipping Docker setup."
fi

# Download dependencies
echo ""
echo "Downloading project dependencies..."
if [ -f "pom.xml" ]; then
    print_info "Downloading Maven dependencies..."
    if command -v mvn &> /dev/null; then
        mvn dependency:resolve
        print_success "Maven dependencies downloaded"
    else
        print_info "Maven not in PATH. Dependencies will be downloaded when you open the project in IntelliJ."
    fi
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    print_info "Downloading Gradle dependencies..."
    if [ -f "gradlew" ]; then
        ./gradlew build -x test
        print_success "Gradle dependencies downloaded"
    else
        print_info "Dependencies will be downloaded when you open the project in IntelliJ."
    fi
fi

# Create necessary directories
echo ""
echo "Creating necessary directories..."
mkdir -p logs
mkdir -p uploads
print_success "Directories created"

echo ""
echo "=========================================="
print_success "Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Open the project in IntelliJ IDEA"
echo "2. Review and update .env file (if created)"
echo "3. Review application.properties or application.yml for configuration"
echo "4. Run the main application class from IntelliJ"
echo ""
echo "Useful commands:"
echo "  - View Docker logs: docker-compose logs -f"
echo "  - Stop Docker containers: docker-compose down"
echo "  - Restart containers: docker-compose restart"
if [ -f "pom.xml" ]; then
    echo "  - Run application: mvn spring-boot:run"
    echo "  - Run tests: mvn test"
elif [ -f "gradlew" ]; then
    echo "  - Run application: ./gradlew bootRun"
    echo "  - Run tests: ./gradlew test"
fi
echo ""
