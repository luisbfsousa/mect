variable "project_name" {
  description = "Project name prefix"
  type        = string
}

variable "network_name" {
  description = "Docker network name"
  type        = string
}

variable "backend_port_start" {
  description = "Starting port for backend replicas"
  type        = number
}

variable "frontend_port_start" {
  description = "Starting port for frontend replicas"
  type        = number
}

variable "replica_count" {
  description = "Number of replicas"
  type        = number
}
