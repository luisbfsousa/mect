output "grafana_port" {
  description = "Host port exposed for Grafana"
  value       = var.grafana_port
}

output "prometheus_port" {
  description = "Host port exposed for Prometheus"
  value       = var.prometheus_port
}

output "collector_http_port" {
  description = "Host port for OTLP/HTTP"
  value       = var.collector_http_port
}

output "collector_grpc_port" {
  description = "Host port for OTLP/gRPC"
  value       = var.collector_grpc_port
}

output "collector_metrics_port" {
  description = "Host port for the Prometheus scrape endpoint"
  value       = var.collector_metrics_port
}

output "grafana_container_name" {
  description = "Name of the Grafana container"
  value       = docker_container.grafana.name
}

output "prometheus_container_name" {
  description = "Name of the Prometheus container"
  value       = docker_container.prometheus.name
}

output "otel_collector_container_name" {
  description = "Name of the OpenTelemetry Collector container"
  value       = docker_container.otel_collector.name
}
