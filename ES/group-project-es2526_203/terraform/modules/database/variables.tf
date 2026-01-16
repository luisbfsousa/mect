variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "network_name" {
  description = "Docker network name"
  type        = string
}

variable "postgres_port" {
  description = "PostgreSQL external port"
  type        = number
  default     = 5432
}

variable "postgres_db" {
  description = "PostgreSQL database name"
  type        = string
  default     = "shophub"
}

variable "postgres_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "shophub_user"
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}
