#!/bin/bash

# ShopHub Run Script
# This script starts Docker Compose services and runs the Spring Boot backend

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
MAVEN_WRAPPER="./mvnw"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

# Parse command line arguments
RESET_DB=false
if [ "$1" == "--reset-db" ] || [ "$1" == "-r" ]; then
    RESET_DB=true
fi

# Reuse host Ollama models if present (avoids re-downloading)
if [ -z "$OLLAMA_MODELS_DIR" ] && [ -d "$HOME/.ollama" ]; then
    export OLLAMA_MODELS_DIR="$HOME/.ollama"
fi

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  ShopHub Run Script${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Function to check if Docker is running
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker is not installed!${NC}"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        echo -e "${RED}Error: Docker daemon is not running!${NC}"
        echo -e "${YELLOW}Please start Docker and try again.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker is running${NC}"
}

# Function to check if docker-compose is available
check_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE_CMD="docker-compose"
    elif docker compose version &> /dev/null 2>&1; then
        DOCKER_COMPOSE_CMD="docker compose"
    else
        echo -e "${RED}Error: docker-compose is not available!${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker Compose is available${NC}"
}

port_in_use() {
    local port=$1
    if command -v ss >/dev/null 2>&1; then
        if ss -tulpn 2>/dev/null | grep -q ":$port "; then
            return 0
        else
            return 1
        fi
    elif command -v lsof >/dev/null 2>&1; then
        if lsof -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then
            return 0
        else
            return 1
        fi
    elif command -v nc >/dev/null 2>&1; then
        if nc -z localhost $port >/dev/null 2>&1; then
            return 0
        else
            return 1
        fi
    fi

    # Fallback: assume free if we can't check
    return 1
}

select_ollama_port() {
    local preferred=${OLLAMA_PORT:-11434}
    local candidate=$preferred

    for i in {0..5}; do
        if port_in_use "$candidate"; then
            echo -e "${YELLOW}Port $candidate is in use; trying next...${NC}"
            candidate=$((candidate + 1))
        else
            export OLLAMA_PORT=$candidate
            export OLLAMA_BASE_URL=${OLLAMA_BASE_URL:-http://localhost:${candidate}}
            echo -e "${GREEN}✓ Using Ollama on host port $candidate${NC}"
            return
        fi
    done

    echo -e "${RED}Error: Could not find a free port for Ollama (tried $preferred..$candidate).${NC}"
    exit 1
}

# Function to reset database
reset_database() {
    echo ""
    echo -e "${RED}============================================${NC}"
    echo -e "${RED}  DATABASE RESET WARNING${NC}"
    echo -e "${RED}============================================${NC}"
    echo ""
    echo -e "${YELLOW}This will:${NC}"
    echo -e "  • Stop all Docker containers"
    echo -e "  • Delete all database volumes"
    echo -e "  • Remove all data (users, products, orders, etc.)"
    echo -e "  • Recreate databases from init.sql"
    echo ""
    
    read -p "$(echo -e ${YELLOW}Are you sure? Type \'yes\' to continue: ${NC})" -r
    echo
    
    if [[ ! $REPLY == "yes" ]]; then
        echo -e "${GREEN}Database reset cancelled.${NC}"
        return
    fi
    
    echo ""
    echo -e "${YELLOW}Stopping containers and removing volumes...${NC}"
    cd "$PROJECT_ROOT"
    $DOCKER_COMPOSE_CMD down -v
    
    echo -e "${GREEN}✓ Database reset complete${NC}"
    echo ""
}

# Function to start Docker Compose services
start_docker_services() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Starting Docker Infrastructure${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    
    if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
        echo -e "${RED}Error: docker-compose.yml not found!${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Starting PostgreSQL, Keycloak, and Flagsmith services...${NC}"
    export GITHUB_REPOSITORY=${GITHUB_REPOSITORY:-local-dev}
    $DOCKER_COMPOSE_CMD up -d \
        postgres \
        keycloak-postgres \
        keycloak \
        flagsmith-postgres \
        flagsmith \
        flagsmith-task-processor \
        ollama
    
    echo ""
    echo -e "${GREEN}✓ Docker services started${NC}"
    echo ""
    echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
    
    # Wait for services to be healthy (max 60 seconds)
    local max_wait=60
    local elapsed=0
    local interval=5
    
    while [ $elapsed -lt $max_wait ]; do
        if $DOCKER_COMPOSE_CMD ps | grep -q "unhealthy"; then
            echo -e "${YELLOW}Services still starting... (${elapsed}s)${NC}"
            sleep $interval
            elapsed=$((elapsed + interval))
        else
            # Check if all services are running
            if $DOCKER_COMPOSE_CMD ps | grep -q "Up"; then
                echo -e "${GREEN}✓ All services are healthy and running${NC}"
                break
            fi
            sleep $interval
            elapsed=$((elapsed + interval))
        fi
    done
    
    if [ $elapsed -ge $max_wait ]; then
        echo -e "${YELLOW}Warning: Services may not be fully ready yet${NC}"
        echo -e "${YELLOW}You can check status with: docker-compose ps${NC}"
    fi
    
    echo ""
    echo -e "${GREEN}Docker Services Status:${NC}"
    $DOCKER_COMPOSE_CMD ps
    echo ""
}

ensure_ollama_model() {
    echo ""
    echo -e "${BLUE}Ensuring Ollama model is available...${NC}"
    MODEL=${OLLAMA_MODEL:-mistral}
    MODEL_DIR=${OLLAMA_MODELS_DIR:-$HOME/.ollama}
    MODEL_PATTERN="^${MODEL}(:|$)"

    if [ "${OLLAMA_SKIP_PULL:-false}" = "true" ]; then
        echo -e "${YELLOW}Skipping Ollama pull because OLLAMA_SKIP_PULL=true${NC}"
        return
    fi

    # If host models dir exists and already has the model, skip pulling
    if [ -d "$MODEL_DIR/models" ]; then
        if ls "$MODEL_DIR/models/manifests" 2>/dev/null | grep -Eq "$MODEL_PATTERN"; then
            echo -e "${GREEN}✓ Found '${MODEL}' in $MODEL_DIR/models, skipping pull${NC}"
            return
        fi
    fi

    # If the service isn't running, skip (set -e so the exec would fail)
    if ! $DOCKER_COMPOSE_CMD ps --services | grep -q "^ollama$"; then
        echo -e "${YELLOW}Ollama service not running; skipping model check.${NC}"
        return
    fi

    if $DOCKER_COMPOSE_CMD exec -T ollama ollama list | awk 'NR>1 {print $1}' | grep -Eq "$MODEL_PATTERN"; then
        echo -e "${GREEN}✓ Ollama model '${MODEL}' already present${NC}"
    else
        echo -e "${YELLOW}Pulling Ollama model '${MODEL}' (first run may take a while)...${NC}"
        $DOCKER_COMPOSE_CMD exec -T ollama ollama pull "$MODEL"
        echo -e "${GREEN}✓ Ollama model '${MODEL}' ready${NC}"
    fi
}

# Function to check if Node.js and npm are available
check_node() {
    if ! command -v node &> /dev/null; then
        echo -e "${RED}Error: Node.js is not installed!${NC}"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        echo -e "${RED}Error: npm is not installed!${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Node.js and npm are available${NC}"
}

# Function to install frontend dependencies if needed
install_frontend_deps() {
    if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
        echo ""
        echo -e "${YELLOW}Installing frontend dependencies...${NC}"
        cd "$FRONTEND_DIR"
        npm install
        echo -e "${GREEN}✓ Frontend dependencies installed${NC}"
    fi
}

# Function to run frontend
run_frontend() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Starting React Frontend${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    
    # Check if frontend directory exists
    if [ ! -d "$FRONTEND_DIR" ]; then
        echo -e "${RED}Error: Frontend directory '$FRONTEND_DIR' not found!${NC}"
        echo -e "${RED}Expected at: $FRONTEND_DIR${NC}"
        exit 1
    fi
    
    install_frontend_deps
    
    echo ""
    echo -e "${YELLOW}Starting React development server...${NC}"
    echo ""
    
    # Navigate to frontend directory and start
    cd "$FRONTEND_DIR"
    export REACT_APP_KEYCLOAK_URL=${REACT_APP_KEYCLOAK_URL:-http://localhost:8080}
    export REACT_APP_KEYCLOAK_REALM=${REACT_APP_KEYCLOAK_REALM:-ShopHub}
    export REACT_APP_KEYCLOAK_CLIENT_ID=${REACT_APP_KEYCLOAK_CLIENT_ID:-shophub-frontend}
    export REACT_APP_API_URL=${REACT_APP_API_URL:-http://localhost:5000/api}
    npm start &
    FRONTEND_PID=$!
    echo -e "${GREEN}✓ Frontend started (PID: $FRONTEND_PID)${NC}"
}

# Function to run Spring Boot backend in background
run_spring_boot() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Starting Spring Boot Backend${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    
    # Check if backend directory exists
    if [ ! -d "$BACKEND_DIR" ]; then
        echo -e "${RED}Error: Backend directory '$BACKEND_DIR' not found!${NC}"
        echo -e "${RED}Expected at: $BACKEND_DIR${NC}"
        exit 1
    fi
    
    # Navigate to backend directory
    cd "$BACKEND_DIR"
    
    # Check if Maven wrapper exists, otherwise use system maven
    if [ -x "$MAVEN_WRAPPER" ]; then
        MAVEN_CMD="$MAVEN_WRAPPER"
        echo -e "${GREEN}Using Maven wrapper${NC}"
    else
        if command -v mvn &> /dev/null; then
            MAVEN_CMD="mvn"
            echo -e "${YELLOW}Maven wrapper not found, using system Maven${NC}"
        else
            echo -e "${RED}Error: Neither Maven wrapper nor system Maven found!${NC}"
            echo -e "${YELLOW}Please install Maven or add the Maven wrapper to the project.${NC}"
            exit 1
        fi
    fi
    
    echo ""
    echo -e "${YELLOW}Starting Spring Boot application...${NC}"
    echo ""
    
export DB_HOST=${DB_HOST:-localhost}
export KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8080}
export KEYCLOAK_PUBLIC_URL=${KEYCLOAK_PUBLIC_URL:-$KEYCLOAK_URL}
export KEYCLOAK_REALM=${KEYCLOAK_REALM:-ShopHub}
export CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost:3000}
export OLLAMA_MODEL=${OLLAMA_MODEL:-gemma3:1b}
# Keep base URL aligned with selected port if not already set
export OLLAMA_BASE_URL=${OLLAMA_BASE_URL:-http://localhost:${OLLAMA_PORT:-11434}}
    # Run the Spring Boot application in background
    $MAVEN_CMD spring-boot:run &
    BACKEND_PID=$!
    echo -e "${GREEN}✓ Backend started (PID: $BACKEND_PID)${NC}"
    
    # Wait for backend to be ready
    echo ""
    echo -e "${YELLOW}Waiting for backend to be ready...${NC}"
    local max_wait=120
    local elapsed=0
    local interval=5
    local backend_url="http://localhost:5000/actuator/health"
    
    while [ $elapsed -lt $max_wait ]; do
        if curl -s -f "$backend_url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Backend is ready!${NC}"
            break
        fi
        sleep $interval
        elapsed=$((elapsed + interval))
        echo -e "${YELLOW}  Still waiting... (${elapsed}s)${NC}"
    done
    
    if [ $elapsed -ge $max_wait ]; then
        echo -e "${YELLOW}Warning: Backend health check timed out${NC}"
        echo -e "${YELLOW}Continuing anyway...${NC}"
    fi
}

# Function to wait for all background processes
wait_for_processes() {
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  All Services Running!${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${CYAN}Press Ctrl+C to stop all services${NC}"
    echo ""
    
    # Wait for backend process
    if [ ! -z "$BACKEND_PID" ]; then
        wait $BACKEND_PID
    fi
}

# Cleanup function for graceful shutdown
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    
    # Stop backend if running
    if [ ! -z "$BACKEND_PID" ] && kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${YELLOW}Stopping backend...${NC}"
        kill $BACKEND_PID 2>/dev/null
        wait $BACKEND_PID 2>/dev/null
    fi
    
    # Stop frontend if running
    if [ ! -z "$FRONTEND_PID" ] && kill -0 $FRONTEND_PID 2>/dev/null; then
        echo -e "${YELLOW}Stopping frontend...${NC}"
        kill $FRONTEND_PID 2>/dev/null
        wait $FRONTEND_PID 2>/dev/null
    fi
    
    echo -e "${YELLOW}All applications stopped.${NC}"
    echo ""
    echo -e "${CYAN}Docker services are still running.${NC}"
    echo -e "${CYAN}To stop them, run: docker-compose down${NC}"
    exit 0
}

# Set up trap for Ctrl+C
trap cleanup SIGINT SIGTERM

# Main execution
echo -e "${CYAN}Checking prerequisites...${NC}"
check_docker
check_docker_compose
check_node

# Reset database if requested
if [ "$RESET_DB" = true ]; then
    reset_database
fi

select_ollama_port
start_docker_services

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Infrastructure Ready!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${CYAN}Services available at:${NC}"
echo -e "  • PostgreSQL: ${GREEN}localhost:5432${NC}"
echo -e "  • Keycloak: ${GREEN}http://localhost:8080${NC}"
echo -e "    - Admin: ${YELLOW}admin / admin${NC}"
echo ""

run_spring_boot

run_frontend

echo ""
echo -e "${CYAN}Application URLs:${NC}"
echo -e "  • Backend: ${GREEN}http://localhost:5000${NC}"
echo -e "  • Frontend: ${GREEN}http://localhost:3000${NC}"
echo ""
echo -e "${CYAN}Tip: Run with --reset-db to reset the database${NC}"
echo ""

wait_for_processes

# If processes stop normally
cleanup
