terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

resource "docker_volume" "postgres_data" {
  name = "${var.project_name}-postgres-data"
}

resource "docker_image" "postgres" {
  name = "postgres:15-alpine"
}

resource "docker_container" "postgres" {
  name  = "${var.project_name}-postgres"
  image = docker_image.postgres.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 5432
    external = var.postgres_port
  }

  env = [
    "POSTGRES_DB=${var.postgres_db}",
    "POSTGRES_USER=${var.postgres_user}",
    "POSTGRES_PASSWORD=${var.postgres_password}"
  ]

  volumes {
    volume_name    = docker_volume.postgres_data.name
    container_path = "/var/lib/postgresql/data"
  }

  upload {
    file    = "/docker-entrypoint-initdb.d/init.sql"
    content = file("${path.root}/../database/init.sql")
  }

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U ${var.postgres_user} -d ${var.postgres_db}"]
    interval = "10s"
    timeout  = "5s"
    retries  = 5
  }

  depends_on = [docker_volume.postgres_data, docker_image.postgres]
}
