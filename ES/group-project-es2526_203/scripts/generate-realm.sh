#!/bin/bash

# Generate realm-export.json from template using .env configuration

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load .env file
if [ -f "$PROJECT_ROOT/.env" ]; then
    source "$PROJECT_ROOT/.env"
else
    echo "Error: .env file not found at $PROJECT_ROOT/.env"
    echo "Please create it from .env.example"
    exit 1
fi

# Check if DEPLOY_HOST_IP is set
if [ -z "$DEPLOY_HOST_IP" ]; then
    echo "Error: DEPLOY_HOST_IP not set in .env"
    exit 1
fi

echo "Generating realm-export.json with IP: $DEPLOY_HOST_IP"

# Replace IP in template and create realm-export.json
sed "s/131\.163\.96\.54/$DEPLOY_HOST_IP/g" \
    "$PROJECT_ROOT/realm-export.json.template" > \
    "$PROJECT_ROOT/realm-export.json"

echo "✅ realm-export.json generated successfully"
