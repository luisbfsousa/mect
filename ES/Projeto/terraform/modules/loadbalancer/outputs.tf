output "lb_url" {
  description = "Load balancer URL (serves both frontend and backend API at /api)"
  value       = "http://localhost:80"
}

output "nginx_container_name" {
  description = "NGINX container name"
  value       = docker_container.nginx_lb.name
}
