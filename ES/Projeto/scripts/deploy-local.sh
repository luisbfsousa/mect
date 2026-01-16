#!/bin/bash

# ============================================
# ShopHub - Local Deployment Script
# Deploys the application on localhost using Terraform
# ============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}======================================"
echo "ShopHub - Local Deployment"
echo -e "======================================${NC}"

# Configuration
DEPLOY_HOST="localhost"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-shophub_password}"

# Step 1: Cleanup existing containers (including docker-compose containers)
echo -e "\n${YELLOW}[1/8] Limpando containers existentes...${NC}"

# Stop docker-compose services if they exist
if [ -f "docker-compose.yml" ]; then
  echo "Parando serviços docker-compose..."
  docker-compose down -v 2>/dev/null || docker compose down -v 2>/dev/null || true
fi

# Remove all ShopHub containers (both Terraform and docker-compose)
docker stop $(docker ps -aq --filter "name=shophub") 2>/dev/null || true
docker stop $(docker ps -aq --filter "name=group-project-es2526_203") 2>/dev/null || true
docker rm -f $(docker ps -aq --filter "name=shophub") 2>/dev/null || true
docker rm -f $(docker ps -aq --filter "name=group-project-es2526_203") 2>/dev/null || true

# Remove specific containers by name (docker-compose style)
docker rm -f shophub-backend shophub-frontend shophub-postgres 2>/dev/null || true
docker rm -f group-project-es2526_203-backend-1 group-project-es2526_203-frontend-1 group-project-es2526_203-postgres-1 2>/dev/null || true

# Remove networks
docker network rm shophub-network 2>/dev/null || true
docker network rm group-project-es2526_203_default 2>/dev/null || true

# Cleanup unused resources
docker system prune -f || true
sleep 5
echo -e "${GREEN}✓ Limpeza concluída${NC}"

# Step 2: Create volumes
echo -e "\n${YELLOW}[2/6] Criando volumes Docker...${NC}"
docker volume create shophub-keycloak-data 2>/dev/null || true
# docker volume create shophub-flagsmith-data 2>/dev/null || true
echo -e "${GREEN}✓ Volumes criados${NC}"

# Step 3: Deploy Keycloak PostgreSQL
echo -e "\n${YELLOW}[3/6] Iniciando Keycloak PostgreSQL...${NC}"
docker run -d \
  --name shophub-keycloak-postgres \
  -p 5434:5432 \
  -e POSTGRES_DB=keycloak \
  -e POSTGRES_USER=keycloak_user \
  -e POSTGRES_PASSWORD=keycloak_password \
  -v shophub-keycloak-data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:15-alpine
echo -e "${GREEN}✓ Keycloak PostgreSQL iniciado na porta 5434${NC}"

# Step 4: Wait for databases
echo -e "\n${YELLOW}[4/6] Aguardando databases...${NC}"
sleep 10
timeout 60 bash -c 'until docker exec shophub-keycloak-postgres pg_isready -U keycloak_user > /dev/null 2>&1; do sleep 2; done' || true
# timeout 60 bash -c 'until docker exec shophub-flagsmith-postgres pg_isready -U postgres -d flagsmith > /dev/null 2>&1; do sleep 2; done' || true
echo -e "${GREEN}✓ Databases prontas${NC}"

# Step 5: Deploy Keycloak
echo -e "\n${YELLOW}[5/6] Iniciando Keycloak...${NC}"

# Get project root directory (script is in scripts/ subdirectory)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

docker run -d \
  --name shophub-keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://host.docker.internal:5434/keycloak \
  -e KC_DB_USERNAME=keycloak_user \
  -e KC_DB_PASSWORD=keycloak_password \
  -e KC_HEALTH_ENABLED=true \
  -e KC_HOSTNAME_STRICT=false \
  -e KC_HOSTNAME_STRICT_HTTPS=false \
  -e KC_HTTP_ENABLED=true \
  -e KC_PROXY=edge \
  -e KEYCLOAK_FRONTEND_URL=http://localhost:8080 \
  -v "${PROJECT_ROOT}/database/realm-export-local.json:/opt/keycloak/data/import/realm-export.json:ro" \
  --add-host=host.docker.internal:host-gateway \
  --restart unless-stopped \
  quay.io/keycloak/keycloak:25.0.6 start-dev \
  --hostname-strict=false \
  --http-enabled=true \
  --import-realm

echo "Aguardando Keycloak iniciar..."
timeout 120 bash -c 'until curl -sf http://localhost:8080/health/ready > /dev/null 2>&1; do sleep 5; done' || true
sleep 10
echo -e "${GREEN}✓ Keycloak pronto na porta 8080${NC}"

# Step 6: Deploy with Terraform (Database, Backend, Frontend)
echo -e "\n${YELLOW}[6/6] Iniciando Terraform Deploy...${NC}"

# Go to project root first
cd "${PROJECT_ROOT}"
cd terraform

# Create terraform.tfvars for localhost
cat > terraform.tfvars <<EOF
# Docker Images
backend_image  = "ghcr.io/detiuaveiro/group-project-es2526_203/backend:latest"
frontend_image = "ghcr.io/detiuaveiro/group-project-es2526_203/frontend:latest"

# Network Configuration (localhost)
deploy_host_ip = "localhost"

# Database Password
postgres_password = "${POSTGRES_PASSWORD}"

# GitHub Container Registry (optional for public images)
github_token    = ""
github_username = ""

# Flagsmith Configuration (Backend)
flagsmith_enabled         = "false"
flagsmith_environment_key = ""
flagsmith_api_url        = "http://host.docker.internal:8000/api/v1"
flagsmith_timeout        = "5s"

# Flagsmith Configuration (Frontend)
react_app_flagsmith_environment_id    = ""
react_app_flagsmith_api_url          = "http://localhost:8000/api/v1"
react_app_flagsmith_default_identity = ""
react_app_flagsmith_enable_analytics = "false"
EOF

# Initialize and apply Terraform
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply -auto-approve tfplan

cd "${PROJECT_ROOT}"

# Wait for services
echo -e "\n${YELLOW}Aguardando serviços iniciarem...${NC}"
sleep 10

# Health checks
echo -e "\n${YELLOW}Verificando saúde dos serviços...${NC}"
echo -n "Backend (porta 5000): "
if timeout 120 bash -c 'until curl -sf http://localhost:5000/actuator/health > /dev/null 2>&1; do sleep 5; done' 2>/dev/null; then
  echo -e "${GREEN}✓ Saudável${NC}"
else
  echo -e "${RED}✗ Não respondeu${NC}"
fi

echo -n "Backend (porta 5001): "
if timeout 60 bash -c 'until curl -sf http://localhost:5001/actuator/health > /dev/null 2>&1; do sleep 5; done' 2>/dev/null; then
  echo -e "${GREEN}✓ Saudável${NC}"
else
  echo -e "${RED}✗ Não respondeu${NC}"
fi

echo -n "Frontend (porta 3000): "
if timeout 60 bash -c 'until curl -sf http://localhost:3000 > /dev/null 2>&1; do sleep 3; done' 2>/dev/null; then
  echo -e "${GREEN}✓ Saudável${NC}"
else
  echo -e "${RED}✗ Não respondeu${NC}"
fi

echo -n "Frontend (porta 3001): "
if timeout 60 bash -c 'until curl -sf http://localhost:3001 > /dev/null 2>&1; do sleep 3; done' 2>/dev/null; then
  echo -e "${GREEN}✓ Saudável${NC}"
else
  echo -e "${RED}✗ Não respondeu${NC}"
fi

# Success message
echo -e "\n${GREEN}======================================"
echo "DEPLOYMENT BEM SUCEDIDO!"
echo -e "======================================${NC}"
echo ""
echo -e "${GREEN}Aceda à aplicação:${NC}"
echo "   Frontend (2 replicas):"
echo "      - http://localhost:3000"
echo "      - http://localhost:3001"
echo ""
echo "   Backend (2 replicas):"
echo "      - http://localhost:5000"
echo "      - http://localhost:5001"
echo ""
echo "   Database:"
echo "      - postgresql://localhost:5432/shophub"
echo ""
echo "   Keycloak:"
echo "      - http://localhost:8080"
echo "      - Admin: admin / admin"
echo "      - Realm: ShopHub"
echo ""
echo -e "${GREEN}======================================"
echo ""
echo "Para ver o status dos containers:"
echo "   docker ps --filter 'name=shophub'"
echo ""
echo "Para ver logs:"
echo "   docker logs shophub-backend-1"
echo "   docker logs shophub-frontend-1"
echo "   docker logs shophub-postgres"
echo ""
echo "Para destruir tudo:"
echo "   ./scripts/destroy-local.sh"
echo -e "${GREEN}======================================${NC}"
