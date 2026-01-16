#!/bin/bash

# ShopHub Build Script
# This script builds the Spring Boot backend

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
MAVEN_WRAPPER="./mvnw"

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  ShopHub Build Script${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Function to build Spring Boot backend
build_spring_boot() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Building Spring Boot Backend${NC}"
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
    echo -e "${YELLOW}Building Spring Boot application...${NC}"
    echo -e "${CYAN}Note: This may take a few minutes on first run...${NC}"
    echo ""
    
    # Build the Spring Boot application
    $MAVEN_CMD clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}============================================${NC}"
        echo -e "${GREEN}  Build Successful!${NC}"
        echo -e "${GREEN}============================================${NC}"
        echo ""
        echo -e "${CYAN}To run the application, use: ./scripts/run.sh${NC}"
    else
        echo ""
        echo -e "${RED}============================================${NC}"
        echo -e "${RED}  Build Failed!${NC}"
        echo -e "${RED}============================================${NC}"
        exit 1
    fi
}

# Function to build Docker images
build_docker_images() {
    echo ""
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}  Building Docker Images${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""

    # Image tags (matching deploy-local.sh expectations)
    BACKEND_IMAGE="ghcr.io/detiuaveiro/group-project-es2526_203/backend:latest"
    FRONTEND_IMAGE="ghcr.io/detiuaveiro/group-project-es2526_203/frontend:latest"

    # Build Backend Image
    echo -e "${YELLOW}Building Backend Docker image...${NC}"
    cd "$BACKEND_DIR"
    docker build -t "${BACKEND_IMAGE}" .
    echo -e "${GREEN}✓ Backend image built: ${BACKEND_IMAGE}${NC}"
    echo ""

    # Build Frontend Image
    echo -e "${YELLOW}Building Frontend Docker image...${NC}"
    cd "$PROJECT_ROOT/frontend"

    # Check which Dockerfile to use
    if [ -f "Dockerfile.local" ]; then
        echo -e "${CYAN}Using Dockerfile.local for local development...${NC}"
        docker build -f Dockerfile.local -t "${FRONTEND_IMAGE}" .
    else
        docker build -t "${FRONTEND_IMAGE}" .
    fi
    echo -e "${GREEN}✓ Frontend image built: ${FRONTEND_IMAGE}${NC}"

    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  Docker Images Built Successfully!${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${CYAN}Images created:${NC}"
    echo "  - ${BACKEND_IMAGE}"
    echo "  - ${FRONTEND_IMAGE}"
    echo ""
}

# Main execution
build_spring_boot
build_docker_images

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  All Builds Complete!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${CYAN}Next step: Deploy locally${NC}"
echo "  ./scripts/deploy-local.sh"
echo ""
