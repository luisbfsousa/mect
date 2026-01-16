terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

resource "docker_image" "backend" {
  name         = var.backend_image
  keep_locally = false
}

resource "docker_container" "backend" {
  count = var.replica_count
  
  name  = "${var.project_name}-backend-${count.index + 1}"
  image = docker_image.backend.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 5000
    external = var.backend_port_start + count.index
  }

  env = [
    "SPRING_DATASOURCE_URL=${var.database_url}",
    "SPRING_DATASOURCE_USERNAME=${var.database_user}",
    "SPRING_DATASOURCE_PASSWORD=${var.database_password}",
    "LOG_DIR=${var.log_dir}",
    "KEYCLOAK_URL=${var.keycloak_url}",
    "KEYCLOAK_PUBLIC_URL=${var.keycloak_public_url != "" ? var.keycloak_public_url : var.keycloak_url}",
    "KEYCLOAK_REALM=${var.keycloak_realm}",
    "CORS_ALLOWED_ORIGINS=${var.cors_allowed_origins}",
    "SPRING_JPA_HIBERNATE_DDL_AUTO=update",
    "SPRING_JPA_SHOW_SQL=false",
    "FLAGSMITH_ENABLED=${var.flagsmith_enabled}",
    "FLAGSMITH_ENVIRONMENT_KEY=${var.flagsmith_environment_key}",
    "FLAGSMITH_API_URL=${var.flagsmith_api_url}",
    "FLAGSMITH_TIMEOUT=${var.flagsmith_timeout}",
    # OpenTelemetry overrideable endpoints
    "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=${var.otel_traces_endpoint}",
    "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=${var.otel_metrics_endpoint}",
    # Ollama configuration for chatbot
    "OLLAMA_BASE_URL=${var.ollama_base_url}",
    "OLLAMA_MODEL=${var.ollama_model}"
  ]

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://127.0.0.1:5000/actuator/health"]
    interval = "30s"
    timeout  = "10s"
    retries  = 5
    start_period = "40s"
  }

  volumes {
    host_path      = var.log_dir
    container_path = var.log_dir
    read_only      = false
  }

  depends_on = [docker_image.backend]
}
