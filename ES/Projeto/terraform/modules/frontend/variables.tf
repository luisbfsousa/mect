variable "project_name" {
  type = string
}

variable "network_name" {
  type = string
}

variable "frontend_image" {
  type = string
}

variable "frontend_port_start" {
  type    = number
  default = 3000
}

variable "replica_count" {
  type    = number
  default = 2
}

variable "keycloak_url" {
  type = string
}

variable "keycloak_realm" {
  type    = string
  default = "ShopHub"
}

variable "keycloak_client_id" {
  type    = string
  default = "shophub-frontend"
}

variable "api_url" {
  type = string
}

variable "flagsmith_api_url" {
  type = string
}

variable "flagsmith_environment_id" {
  type = string
}

variable "flagsmith_default_identity" {
  type = string
  default = ""
}

variable "flagsmith_enable_analytics" {
  type    = string
  default = "false"
}
