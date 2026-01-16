#!/bin/bash

# Setup .env file with your server's IP

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=================================="
echo "ShopHub Environment Setup"
echo "=================================="
echo ""

# Get server IP
DEFAULT_IP=$(hostname -I | awk '{print $1}')

echo "Detected IP: $DEFAULT_IP"
read -p "Enter your server IP (or press Enter to use $DEFAULT_IP): " USER_IP
SERVER_IP=${USER_IP:-$DEFAULT_IP}

echo ""
echo "Using IP: $SERVER_IP"
echo ""

# Create .env from template
if [ ! -f "$PROJECT_ROOT/.env.example" ]; then
    echo "Error: .env.example not found!"
    exit 1
fi

# Copy template
cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"

# Replace DEPLOY_HOST_IP
sed -i "s/DEPLOY_HOST_IP=.*/DEPLOY_HOST_IP=$SERVER_IP/" "$PROJECT_ROOT/.env"

# Replace CORS_ALLOWED_ORIGINS with actual IP
CORS_ORIGINS="http://$SERVER_IP,http://$SERVER_IP:3000,http://$SERVER_IP:3001,http://localhost:3000,http://localhost:3001"
sed -i "s|CORS_ALLOWED_ORIGINS=.*|CORS_ALLOWED_ORIGINS=$CORS_ORIGINS|" "$PROJECT_ROOT/.env"

echo "✅ .env file created successfully!"
echo ""
echo "Configuration:"
echo "  Server IP: $SERVER_IP"
echo "  CORS Origins: $CORS_ORIGINS"
echo ""
echo "You can edit $PROJECT_ROOT/.env to customize further."
echo "=================================="
