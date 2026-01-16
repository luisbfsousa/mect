#!/bin/bash
# ============================================
# Terraform State Cleanup Script
# ============================================
# This script removes Ollama and Monitoring from Terraform state
# since they are now managed by infrastructure.yml workflow
#
# IMPORTANT: Run this BEFORE activating the new main.tf
# ============================================

set -e  # Exit on error

echo "================================================"
echo "Terraform State Cleanup"
echo "================================================"
echo ""
echo "This script will:"
echo "  1. Remove Ollama module from Terraform state"
echo "  2. Remove Monitoring module from Terraform state"
echo "  3. Keep services running (no containers deleted)"
echo ""
echo "⚠️  WARNING: This modifies Terraform state!"
echo "   Make sure you have a backup."
echo ""

# Confirm
read -p "Continue? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted."
  exit 1
fi

echo ""
echo "================================================"
echo "Step 1: Backup Current State"
echo "================================================"

cd "$(dirname "$0")/../terraform" || exit 1

if [ -f "terraform.tfstate" ]; then
  BACKUP_FILE="terraform.tfstate.backup-$(date +%Y%m%d-%H%M%S)"
  cp terraform.tfstate "$BACKUP_FILE"
  echo "✅ State backed up to: $BACKUP_FILE"
else
  echo "ℹ️  No terraform.tfstate found (this is okay if state is remote)"
fi

echo ""
echo "================================================"
echo "Step 2: Initialize Terraform"
echo "================================================"

terraform init -reconfigure
echo "✅ Terraform initialized"

echo ""
echo "================================================"
echo "Step 3: Remove Ollama from State"
echo "================================================"

if terraform state list | grep -q "module.ollama"; then
  echo "Removing module.ollama from state..."
  terraform state rm module.ollama || echo "⚠️  Failed to remove module.ollama (may not exist)"
  echo "✅ Ollama module removed from state"
else
  echo "ℹ️  module.ollama not found in state (already removed or doesn't exist)"
fi

echo ""
echo "================================================"
echo "Step 4: Remove Monitoring from State"
echo "================================================"

if terraform state list | grep -q "module.monitoring"; then
  echo "Removing module.monitoring from state..."
  terraform state rm module.monitoring || echo "⚠️  Failed to remove module.monitoring (may not exist)"
  echo "✅ Monitoring module removed from state"
else
  echo "ℹ️  module.monitoring not found in state (already removed or doesn't exist)"
fi

echo ""
echo "================================================"
echo "Step 5: Verify State"
echo "================================================"

echo "Current Terraform state contains:"
terraform state list

echo ""
echo "Expected modules:"
echo "  ✅ module.database"
echo "  ✅ module.backend"
echo "  ✅ module.frontend"
echo "  ✅ module.loadbalancer"
echo "  ❌ module.ollama (should be removed)"
echo "  ❌ module.monitoring (should be removed)"

echo ""
echo "================================================"
echo "✅ CLEANUP COMPLETE"
echo "================================================"
echo ""
echo "Next steps:"
echo "  1. Verify containers are still running:"
echo "     docker ps | grep shophub"
echo ""
echo "  2. Activate new Terraform files:"
echo "     cd terraform/"
echo "     mv main.tf main.tf.old"
echo "     mv main.tf.new main.tf"
echo "     mv outputs.tf outputs.tf.old"
echo "     mv outputs.tf.new outputs.tf"
echo ""
echo "  3. Test Terraform plan:"
echo "     terraform plan"
echo ""
echo "  4. If plan looks good, apply:"
echo "     terraform apply"
echo ""
echo "================================================"
