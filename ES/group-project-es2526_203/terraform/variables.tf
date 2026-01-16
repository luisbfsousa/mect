variable "backend_image" {
  description = "Backend Docker image"
  type        = string
}

variable "frontend_image" {
  description = "Frontend Docker image"
  type        = string
}

variable "deploy_host_ip" {
  description = "Deployment host IP"
  type        = string
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "github_username" {
  description = "GitHub username for registry"
  type        = string
  default     = ""
}

variable "github_token" {
  description = "GitHub token for registry"
  type        = string
  sensitive   = true
  default     = ""
}

# Port configuration (for Blue-Green deployment)
variable "project_name_prefix" {
  description = "Project name prefix (e.g., 'shophub' or 'shophub-blue')"
  type        = string
  default     = "shophub"
}

variable "backend_port_start" {
  description = "Starting port for backend replicas"
  type        = number
  default     = 5000
}

variable "frontend_port_start" {
  description = "Starting port for frontend replicas"
  type        = number
  default     = 3000
}

variable "replica_count" {
  description = "Number of replicas for backend and frontend"
  type        = number
  default     = 2
}

# Flagsmith Backend
variable "flagsmith_enabled" {
  description = "Enable Flagsmith"
  type        = string
  default     = "false"
}

variable "flagsmith_environment_key" {
  description = "Flagsmith environment key"
  type        = string
  default     = ""
}

variable "flagsmith_api_url" {
  description = "Flagsmith API URL"
  type        = string
  default     = "http://shophub-flagsmith:8000/api/v1"
}

variable "flagsmith_timeout" {
  description = "Flagsmith timeout"
  type        = string
  default     = "5s"
}

# Flagsmith Frontend (React)
variable "react_app_flagsmith_api_url" {
  description = "Flagsmith API URL for React frontend"
  type        = string
  default     = "http://131.163.96.54:8000/api/v1"
}

variable "react_app_flagsmith_environment_id" {
  description = "Flagsmith environment ID for React frontend"
  type        = string
  default     = ""
}

variable "react_app_flagsmith_default_identity" {
  description = "Default identity used by the React Flagsmith SDK"
  type        = string
  default     = ""
}

variable "react_app_flagsmith_enable_analytics" {
  description = "Enable Flagsmith analytics in the React app"
  type        = string
  default     = "false"
}

variable "backend_log_dir" {
  description = "Host path where backend structured logs are stored"
  type        = string
  default     = "/var/log/shophub/backend"
}

variable "otel_traces_endpoint" {
  description = "OpenTelemetry traces endpoint URL"
  type        = string
  default     = ""
}

variable "otel_metrics_endpoint" {
  description = "OpenTelemetry metrics endpoint URL"
  type        = string
  default     = ""
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

variable "grafana_port" {
  description = "Host port that exposes Grafana"
  type        = number
  default     = 3100
}

variable "prometheus_port" {
  description = "Host port that exposes Prometheus"
  type        = number
  default     = 9090
}

variable "otel_collector_http_port" {
  description = "Host port bound to the OTLP/HTTP receiver"
  type        = number
  default     = 4318
}

variable "otel_collector_grpc_port" {
  description = "Host port bound to the OTLP/gRPC receiver"
  type        = number
  default     = 4317
}

variable "otel_collector_metrics_port" {
  description = "Host port bound to the OTEL Prometheus exporter"
  type        = number
  default     = 9464
}
