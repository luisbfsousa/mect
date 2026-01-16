terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

resource "docker_image" "ollama" {
  name         = "ollama/ollama:latest"
  keep_locally = true
}

resource "docker_volume" "ollama_models" {
  name = "${var.project_name}-ollama-models"
}

resource "docker_container" "ollama" {
  name  = "${var.project_name}-ollama"
  image = docker_image.ollama.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 11434
    external = var.ollama_port
  }

  volumes {
    volume_name    = docker_volume.ollama_models.name
    container_path = "/root/.ollama"
  }

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
    interval = "30s"
    timeout  = "10s"
    retries  = 3
  }
}
