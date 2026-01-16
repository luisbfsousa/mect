terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

# Generate NGINX configuration
resource "local_file" "nginx_config" {
  content = templatefile("${path.module}/nginx.conf.tpl", {
    project_name         = var.project_name
    backend_port_start   = var.backend_port_start
    frontend_port_start  = var.frontend_port_start
    replica_count        = var.replica_count
  })
  filename = "${path.module}/nginx.conf"
}

# NGINX Docker Image
resource "docker_image" "nginx" {
  name         = "nginx:alpine"
  keep_locally = true
}

# NGINX Load Balancer Container
resource "docker_container" "nginx_lb" {
  name  = "${var.project_name}-nginx-lb"
  image = docker_image.nginx.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 80
    external = 80
  }

  upload {
    content = local_file.nginx_config.content
    file    = "/etc/nginx/nginx.conf"
  }

  restart = "unless-stopped"

  healthcheck {
    test     = ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://127.0.0.1:80/health"]
    interval = "30s"
    timeout  = "10s"
    retries  = 3
  }

  depends_on = [local_file.nginx_config]
}
