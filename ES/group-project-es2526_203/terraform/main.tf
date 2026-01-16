# ============================================
# FILE: terraform/main.tf (ROOT)
# UPDATED: Removed infrastructure services (Ollama, Monitoring)
# These are now managed by infrastructure.yml workflow
# ============================================

terraform {
  required_version = "~> 1.14.0"
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
    local = {
      source  = "hashicorp/local"
      version = "2.5.3"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
  }
}

provider "docker" {
  host = "unix:///var/run/docker.sock"

  # registry_auth {
  #   address  = "ghcr.io"
  #   username = var.github_username
  #   password = var.github_token
  # }
}

# ============================================
# LOCALS
# ============================================
# Note: Monitoring is now managed by infrastructure.yml
# If you need OTEL endpoints, use external infrastructure URLs
locals {
  # Default to disabled/empty if monitoring module is not used
  otel_traces_endpoint  = var.otel_traces_endpoint != "" ? var.otel_traces_endpoint : ""
  otel_metrics_endpoint = var.otel_metrics_endpoint != "" ? var.otel_metrics_endpoint : ""
}

# ============================================
# NETWORK
# ============================================
# Network is created by workflow before Terraform runs
data "docker_network" "shophub" {
  name = "shophub-network"
}

# ============================================
# DATABASE MODULE (Application PostgreSQL)
# ============================================
module "database" {
  source = "./modules/database"

  project_name        = var.project_name_prefix
  network_name        = data.docker_network.shophub.name
  postgres_port       = 5432
  postgres_password   = var.postgres_password
}

# ============================================
# REMOVED: Ollama Module
# ============================================
# Ollama is now managed by infrastructure.yml workflow
# This prevents state conflicts and deployment delays (model downloads)
# The Ollama service will be available at: http://shophub-ollama:11434
#
# If you need to manage it with Terraform, uncomment below:
# module "ollama" {
#   source = "./modules/ollama"
#
#   project_name  = var.project_name_prefix
#   network_name  = data.docker_network.shophub.name
#   ollama_port   = 11434
# }

# ============================================
# REMOVED: Monitoring Module
# ============================================
# Monitoring (Grafana, Prometheus, OTEL) is now managed by infrastructure.yml
# This is because monitoring is infrastructure that rarely changes
#
# If you need to manage it with Terraform, uncomment below:
# module "monitoring" {
#   source = "./modules/monitoring"
#
#   project_name           = var.project_name_prefix
#   network_name           = data.docker_network.shophub.name
#   grafana_port           = var.grafana_port
#   prometheus_port        = var.prometheus_port
#   collector_http_port    = var.otel_collector_http_port
#   collector_grpc_port    = var.otel_collector_grpc_port
#   collector_metrics_port = var.otel_collector_metrics_port
#   grafana_admin_user     = var.grafana_admin_user
#   grafana_admin_password = var.grafana_admin_password
# }

# ============================================
# BACKEND MODULE (Application Services)
# ============================================
module "backend" {
  source = "./modules/backend"

  project_name          = var.project_name_prefix
  network_name          = data.docker_network.shophub.name
  backend_image         = var.backend_image
  backend_port_start    = var.backend_port_start
  replica_count         = var.replica_count

  # Database connection
  database_url          = module.database.postgres_connection_string
  database_user         = "shophub_user"
  database_password     = var.postgres_password

  # Keycloak configuration (managed by infrastructure.yml)
  keycloak_url          = "http://shophub-keycloak:8080"
  keycloak_public_url   = "http://${var.deploy_host_ip}:8080"
  keycloak_realm        = "ShopHub"

  # CORS configuration
  # Allows Load Balancer (port 80) + individual replicas (for debugging)
  cors_allowed_origins  = "http://${var.deploy_host_ip},http://${var.deploy_host_ip}:${var.frontend_port_start},http://${var.deploy_host_ip}:${var.frontend_port_start + 1}"

  # Flagsmith configuration (managed by infrastructure.yml)
  flagsmith_enabled         = var.flagsmith_enabled
  flagsmith_environment_key = var.flagsmith_environment_key
  flagsmith_api_url         = var.flagsmith_api_url
  flagsmith_timeout         = var.flagsmith_timeout

  # Logging
  log_dir                  = var.backend_log_dir

  # OpenTelemetry endpoints (optional - empty if monitoring not in use)
  otel_traces_endpoint     = local.otel_traces_endpoint
  otel_metrics_endpoint    = local.otel_metrics_endpoint

  # Ollama configuration (managed by infrastructure.yml)
  ollama_base_url          = "http://${var.project_name_prefix}-ollama:11434"
  ollama_model             = "gemma3:1b"

  # Dependencies: Only database now
  # Removed: module.monitoring, module.ollama (managed externally)
  depends_on = [module.database]
}

# ============================================
# FRONTEND MODULE (Application Services)
# ============================================
module "frontend" {
  source = "./modules/frontend"

  project_name       = var.project_name_prefix
  network_name       = data.docker_network.shophub.name
  frontend_image     = var.frontend_image
  frontend_port_start = var.frontend_port_start
  replica_count      = var.replica_count

  # Keycloak configuration (managed by infrastructure.yml)
  keycloak_url       = "http://${var.deploy_host_ip}:8080"
  keycloak_realm     = "ShopHub"
  keycloak_client_id = "shophub-frontend"

  # Backend API URL (via load balancer)
  api_url           = "http://${var.deploy_host_ip}/api"

  # Flagsmith configuration (managed by infrastructure.yml)
  flagsmith_api_url              = var.react_app_flagsmith_api_url
  flagsmith_environment_id       = var.react_app_flagsmith_environment_id
  flagsmith_default_identity     = var.react_app_flagsmith_default_identity
  flagsmith_enable_analytics     = var.react_app_flagsmith_enable_analytics

  depends_on = [module.backend]
}

# ============================================
# LOAD BALANCER MODULE (NGINX)
# ============================================
module "loadbalancer" {
  source = "./modules/loadbalancer"

  project_name         = var.project_name_prefix
  network_name         = data.docker_network.shophub.name
  backend_port_start   = var.backend_port_start
  frontend_port_start  = var.frontend_port_start
  replica_count        = var.replica_count

  depends_on = [module.backend, module.frontend]
}
