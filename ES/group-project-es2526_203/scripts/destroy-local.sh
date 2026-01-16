#!/bin/bash

# ============================================
# ShopHub - Local Cleanup Script
# Destroys all local deployment resources
# ============================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}======================================"
echo "ShopHub - Destroying Local Deployment"
echo -e "======================================${NC}"

# Step 1: Terraform Destroy
echo -e "\n${YELLOW}[1/4] Destruindo recursos Terraform...${NC}"
if [ -d "terraform" ]; then
  cd terraform
  if terraform state list 2>/dev/null | grep -q .; then
    terraform destroy -auto-approve || true
  else
    echo "Nenhum recurso Terraform encontrado"
  fi
  cd ..
fi
echo -e "${GREEN}✓ Recursos Terraform destruídos${NC}"

# Step 2: Stop and remove all ShopHub containers
echo -e "\n${YELLOW}[2/4] Parando e removendo containers...${NC}"
docker stop $(docker ps -aq --filter "name=shophub") 2>/dev/null || true
docker rm -f $(docker ps -aq --filter "name=shophub") 2>/dev/null || true
echo -e "${GREEN}✓ Containers removidos${NC}"

# Step 3: Remove network
echo -e "\n${YELLOW}[3/4] Removendo rede Docker...${NC}"
docker network rm shophub-network 2>/dev/null || true
echo -e "${GREEN}✓ Rede removida${NC}"

# Step 4: Optional - Remove volumes (data will be lost!)
read -p "Remover volumes persistentes? (dados serão PERDIDOS) [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${YELLOW}[4/4] Removendo volumes...${NC}"
  docker volume rm shophub-postgres-data 2>/dev/null || true
  docker volume rm shophub-keycloak-data 2>/dev/null || true
  docker volume rm shophub-flagsmith-data 2>/dev/null || true
  echo -e "${GREEN}✓ Volumes removidos${NC}"
else
  echo -e "${GREEN}✓ Volumes mantidos (dados preservados)${NC}"
fi

# Cleanup
echo -e "\n${YELLOW}Limpando recursos Docker não utilizados...${NC}"
docker system prune -f || true

echo -e "\n${GREEN}======================================"
echo "LIMPEZA CONCLUÍDA!"
echo -e "======================================${NC}"
echo ""
echo "Todos os containers ShopHub foram removidos."
echo ""
echo "Para iniciar novamente:"
echo "   ./scripts/deploy-local.sh"
echo -e "${GREEN}======================================${NC}"
