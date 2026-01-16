variable "project_name" {
  type = string
}

variable "network_name" {
  type = string
}

variable "backend_image" {
  type = string
}

variable "backend_port_start" {
  type    = number
  default = 5000
}

variable "replica_count" {
  type    = number
  default = 2
}

variable "database_url" {
  type = string
}

variable "database_user" {
  type = string
}

variable "database_password" {
  type      = string
  sensitive = true
}

variable "keycloak_url" {
  type = string
}

variable "keycloak_public_url" {
  type    = string
  default = ""
}

variable "keycloak_realm" {
  type    = string
  default = "ShopHub"
}

variable "cors_allowed_origins" {
  type = string
}

variable "flagsmith_enabled" {
  type    = string
  default = "false"
}

variable "flagsmith_environment_key" {
  type    = string
  default = ""
}

variable "flagsmith_api_url" {
  type    = string
  default = ""
}

variable "flagsmith_timeout" {
  type    = string
  default = "5s"
}

variable "log_dir" {
  description = "Host path for backend structured logs (mounted inside container)"
  type        = string
  default     = "/var/log/shophub/backend"
}

variable "otel_traces_endpoint" {
  description = "OpenTelemetry traces endpoint URL"
  type        = string
  default     = "http://jaeger:4318/v1/traces"
}

variable "otel_metrics_endpoint" {
  description = "OpenTelemetry metrics endpoint URL"
  type        = string
  default     = "http://otel-collector:4318/v1/metrics"
}

variable "ollama_base_url" {
  description = "Ollama API base URL"
  type        = string
  default     = "http://shophub-ollama:11434"
}

variable "ollama_model" {
  description = "Ollama model to use for chatbot"
  type        = string
  default     = "gemma3:1b"
}
