variable "project_name" {
  description = "Project name prefix for resources"
  type        = string
}

variable "network_name" {
  description = "Docker network name"
  type        = string
}

variable "ollama_port" {
  description = "External port for Ollama API"
  type        = number
  default     = 11434
}
