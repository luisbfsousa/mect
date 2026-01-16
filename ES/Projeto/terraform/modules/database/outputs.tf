output "postgres_host" {
  description = "PostgreSQL container name for internal connections"
  value       = docker_container.postgres.name
}

output "postgres_port" {
  description = "PostgreSQL port"
  value       = var.postgres_port
}

output "postgres_connection_string" {
  description = "PostgreSQL JDBC connection string"
  value       = "jdbc:postgresql://${docker_container.postgres.name}:5432/${var.postgres_db}"
  sensitive   = true
}
