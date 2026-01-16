# ============================================
# OUTPUTS
# UPDATED: Removed monitoring outputs (now in infrastructure.yml)
# ============================================

output "database_host" {
  description = "Database host"
  value       = module.database.postgres_host
}

output "backend_urls" {
  description = "Backend service URLs"
  value       = module.backend.backend_urls
}

output "frontend_urls" {
  description = "Frontend service URLs"
  value       = module.frontend.frontend_urls
}

output "lb_url" {
  description = "Load balancer URL (serves frontend and backend API at /api)"
  value       = module.loadbalancer.lb_url
}

# ============================================
# REMOVED: Monitoring Outputs
# ============================================
# Monitoring services are now managed by infrastructure.yml
# Access them at these URLs (if infrastructure is running):
#   Grafana:    http://131.163.96.54:3100
#   Prometheus: http://131.163.96.54:9090
#   OTLP HTTP:  http://131.163.96.54:4318
#   OTLP gRPC:  131.163.96.54:4317

output "deployment_summary" {
  description = "Deployment summary"
  value = <<-EOT

    ====================================
    ShopHub Deployment Summary
    ====================================

    Database:
      - Host: ${module.database.postgres_host}
      - Port: ${module.database.postgres_port}

    Load Balancer:
      - URL:      http://${var.deploy_host_ip}
      - Frontend: http://${var.deploy_host_ip}/
      - Backend:  http://${var.deploy_host_ip}/api

    Backend Replicas:
      ${join("\n      ", module.backend.backend_urls)}

    Frontend Replicas:
      ${join("\n      ", module.frontend.frontend_urls)}

    Infrastructure (managed separately):
      - Keycloak:  http://${var.deploy_host_ip}:8080
      - Flagsmith: http://${var.deploy_host_ip}:8000
      - Ollama:    http://${var.deploy_host_ip}:11434
      - Grafana:   http://${var.deploy_host_ip}:3100 (if enabled)
      - Prometheus: http://${var.deploy_host_ip}:9090 (if enabled)

    ====================================

    💡 Use the Load Balancer URLs for access.
       Traffic will be distributed across all replicas.

    ⚠️  Infrastructure services (Keycloak, Flagsmith, Ollama, Monitoring)
       are managed by the infrastructure.yml workflow, not Terraform.

    ====================================
  EOT
}
