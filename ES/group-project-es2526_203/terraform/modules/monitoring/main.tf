terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = ">= 3.0.0"
    }
  }
}

locals {
  monitoring_dir = abspath("${path.module}/generated")
  otel_config = <<-EOT
    receivers:
      otlp:
        protocols:
          http:
            endpoint: 0.0.0.0:4318
            cors:
              allowed_origins:
                - "http://*"
                - "https://*"
              allowed_headers:
                - "*"
          grpc:
            endpoint: 0.0.0.0:4317

    processors:
      batch: {}
      spanmetrics:
        metrics_exporter: prometheus
        latency_histogram_buckets: [2ms, 6ms, 10ms, 100ms, 250ms, 500ms, 1s, 2s, 5s, 10s]
        dimensions:
          - name: http.method
          - name: http.status_code
          - name: http.route

    exporters:
      logging:
        loglevel: info
      prometheus:
        endpoint: 0.0.0.0:${var.collector_metrics_port}

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [batch, spanmetrics]
          exporters: [logging]
        metrics:
          receivers: [otlp]
          processors: [batch]
          exporters: [prometheus, logging]
  EOT

  prometheus_config = <<-EOT
    global:
      scrape_interval: 15s

    scrape_configs:
      - job_name: 'otel-collector'
        static_configs:
          - targets: ['${var.project_name}-otel-collector:${var.collector_metrics_port}']
      
      - job_name: 'backend'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets: ['${var.project_name}-backend-1:5000', '${var.project_name}-backend-2:5000']
      
      - job_name: 'frontend'
        static_configs:
          - targets: ['${var.project_name}-frontend-1:3000', '${var.project_name}-frontend-2:3000']
      
      - job_name: 'postgres'
        static_configs:
          - targets: ['${var.project_name}-postgres:5432']
  EOT

  grafana_datasource = <<-EOT
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        access: proxy
        url: http://${var.project_name}-prometheus:9090
        isDefault: true
        editable: false
  EOT
}

resource "null_resource" "monitoring_dirs" {
  triggers = {
    base_dir = local.monitoring_dir
  }

  provisioner "local-exec" {
    command = "mkdir -p \"${local.monitoring_dir}/grafana/datasources\" \"${local.monitoring_dir}/prometheus\" \"${local.monitoring_dir}/otel\""
  }
}

resource "local_file" "otel_config" {
  content  = local.otel_config
  filename = "${local.monitoring_dir}/otel/config.yaml"
  depends_on = [null_resource.monitoring_dirs]
}

resource "local_file" "prometheus_config" {
  content  = local.prometheus_config
  filename = "${local.monitoring_dir}/prometheus/prometheus.yml"
  depends_on = [null_resource.monitoring_dirs]
}

resource "local_file" "grafana_datasource" {
  content  = local.grafana_datasource
  filename = "${local.monitoring_dir}/grafana/datasources/datasource.yml"
  depends_on = [null_resource.monitoring_dirs]
}

resource "docker_image" "otel_collector" {
  name         = var.otel_collector_image
  keep_locally = true
}

resource "docker_image" "prometheus" {
  name         = var.prometheus_image
  keep_locally = true
}

resource "docker_image" "grafana" {
  name         = var.grafana_image
  keep_locally = true
}

resource "docker_container" "otel_collector" {
  name  = "${var.project_name}-otel-collector"
  image = docker_image.otel_collector.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 4318
    external = var.collector_http_port
  }

  ports {
    internal = 4317
    external = var.collector_grpc_port
  }

  ports {
    internal = var.collector_metrics_port
    external = var.collector_metrics_port
  }

  mounts {
    target = "/etc/otelcol"
    source = "${local.monitoring_dir}/otel"
    type   = "bind"
    read_only = true
  }

  command = ["--config", "/etc/otelcol/config.yaml"]
  restart = "unless-stopped"

  depends_on = [local_file.otel_config]
}

resource "docker_container" "prometheus" {
  name  = "${var.project_name}-prometheus"
  image = docker_image.prometheus.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 9090
    external = var.prometheus_port
  }

  mounts {
    target = "/etc/prometheus"
    source = "${local.monitoring_dir}/prometheus"
    type   = "bind"
    read_only = true
  }

  command = ["--config.file=/etc/prometheus/prometheus.yml"]
  restart = "unless-stopped"

  depends_on = [docker_container.otel_collector, local_file.prometheus_config]
}

resource "docker_container" "grafana" {
  name  = "${var.project_name}-grafana"
  image = docker_image.grafana.image_id

  networks_advanced {
    name = var.network_name
  }

  ports {
    internal = 3000
    external = var.grafana_port
  }

  mounts {
    target = "/etc/grafana/provisioning"
    source = "${local.monitoring_dir}/grafana"
    type   = "bind"
    read_only = true
  }

  env = [
    "GF_SECURITY_ADMIN_USER=${var.grafana_admin_user}",
    "GF_SECURITY_ADMIN_PASSWORD=${var.grafana_admin_password}",
    "GF_AUTH_ANONYMOUS_ENABLED=false"
  ]

  restart = "unless-stopped"

  depends_on = [docker_container.prometheus, local_file.grafana_datasource]
}
