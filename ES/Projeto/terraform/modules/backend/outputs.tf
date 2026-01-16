output "backend_urls" {
  value = [
    for i, container in docker_container.backend :
    "http://localhost:${var.backend_port_start + i}"
  ]
}

output "backend_container_names" {
  value = [for c in docker_container.backend : c.name]
}
