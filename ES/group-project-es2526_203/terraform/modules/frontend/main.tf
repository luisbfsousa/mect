terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

resource "docker_image" "frontend" {
  name         = var.frontend_image
  keep_locally = false
}

resource "docker_container" "frontend" {
  count = var.replica_count
  
  name  = "${var.project_name}-frontend-${count.index + 1}"
  image = docker_image.frontend.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 80
    external = var.frontend_port_start + count.index
  }

  env = [
    "REACT_APP_KEYCLOAK_URL=${var.keycloak_url}",
    "REACT_APP_KEYCLOAK_REALM=${var.keycloak_realm}",
    "REACT_APP_KEYCLOAK_CLIENT_ID=${var.keycloak_client_id}",
    "REACT_APP_API_URL=${var.api_url}",
    "REACT_APP_FLAGSMITH_API_URL=${var.flagsmith_api_url}",
    "REACT_APP_FLAGSMITH_ENVIRONMENT_ID=${var.flagsmith_environment_id}",
    "REACT_APP_FLAGSMITH_DEFAULT_IDENTITY=${var.flagsmith_default_identity}",
    "REACT_APP_FLAGSMITH_ENABLE_ANALYTICS=${var.flagsmith_enable_analytics}"
  ]

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://127.0.0.1:80"]
    interval = "30s"
    timeout  = "10s"
    retries  = 3
  }

  depends_on = [docker_image.frontend]
}
