output "frontend_urls" {
  value = [
    for i, container in docker_container.frontend :
    "http://localhost:${var.frontend_port_start + i}"
  ]
}

output "frontend_container_names" {
  value = [for c in docker_container.frontend : c.name]
}
