variable "project_name" {
  description = "Prefix applied to monitoring container names"
  type        = string
}

variable "network_name" {
  description = "Docker network used by monitoring stack"
  type        = string
}

variable "grafana_port" {
  description = "Host port exposed for Grafana"
  type        = number
  default     = 3100
}

variable "prometheus_port" {
  description = "Host port exposed for Prometheus"
  type        = number
  default     = 9090
}

variable "collector_http_port" {
  description = "Host port mapped to the OTLP/HTTP receiver"
  type        = number
  default     = 4318
}

variable "collector_grpc_port" {
  description = "Host port mapped to the OTLP/gRPC receiver"
  type        = number
  default     = 4317
}

variable "collector_metrics_port" {
  description = "Port exposed by the collector Prometheus exporter"
  type        = number
  default     = 9464
}

variable "grafana_admin_user" {
  description = "Grafana administrator username"
  type        = string
  default     = "admin"
}

variable "grafana_admin_password" {
  description = "Grafana administrator password"
  type        = string
  sensitive   = true
  default     = "admin"
}

variable "grafana_image" {
  description = "Grafana container image"
  type        = string
  default     = "grafana/grafana:10.4.3"
}

variable "prometheus_image" {
  description = "Prometheus container image"
  type        = string
  default     = "prom/prometheus:v2.54.0"
}

variable "otel_collector_image" {
  description = "OpenTelemetry Collector container image"
  type        = string
  default     = "otel/opentelemetry-collector-contrib:0.94.0"
}
