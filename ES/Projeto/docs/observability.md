# Observability - SLOs and SLIs

## Service Level Indicators (SLIs)

SLIs are quantitative measures of service behavior that we track continuously.

### Availability SLIs

|   SLI ID  |     Metric                   | Measurement |
|-----------|------------------------------|-------------|
| SLI-AV-01 | API Availability             | % of successful health check responses (HTTP 200) |
| SLI-AV-02 | Frontend Availability        | % of successful page loads |
| SLI-AV-03 | Database Availability        | % of successful database connections |

### Performance SLIs

|   SLI ID  |     Metric                  | Measurement |
|-----------|-----------------------------|-------------|
| SLI-PF-01 | API Response Time           | 95% of requests complete within X ms |
| SLI-PF-02 | Page Load Time              | 95% of pages load within X seconds |
| SLI-PF-03 | Search Query Time           | 95% of searches complete within X ms |
| SLI-PF-04 | Cart Update Time            | 95% of cart operations complete within X ms |
| SLI-PF-05 | Database Query Time         | 95% of queries execute within X ms |

### Reliability SLIs

|   SLI ID  |     Metric                  | Measurement |
|-----------|-----------------------------|-------------|
| SLI-RL-01 | Error Rate                  | % of HTTP 5xx responses vs total requests |
| SLI-RL-02 | Order Processing Success    | % of successfully processed orders |
| SLI-RL-03 | Payment Success Rate        | % of successful payment transactions |
| SLI-RL-04 | Inventory Accuracy          | % of inventory operations with correct stock updates |

### Throughput SLIs

|   SLI ID  |     Metric                  | Measurement |
|-----------|-----------------------------|-------------|
| SLI-TP-01 | Request Throughput          | Requests per second handled by API |
| SLI-TP-02 | Concurrent Users            | Number of concurrent active sessions |
| SLI-TP-03 | Order Processing Rate       | Orders processed per minute |

---

## Service Level Objectives (SLOs)

SLOs define target values for our SLIs.

### Availability SLOs

| SLO ID    | Description               | Target | Period |
|-----------|---------------------------|--------|--------|
| SLO-AV-01 | API Uptime                | ≥ 99.5% | Monthly |
| SLO-AV-02 | Frontend Uptime           | ≥ 99.5% | Monthly |
| SLO-AV-03 | Database Uptime           | ≥ 99.9% | Monthly |

### Performance SLOs

| SLO ID    | Description               | Target                  | Period |
|-----------|---------------------------|-------------------------|--------|
| SLO-PF-01 | API Response Time         | 95% of requests ≤ 200ms | Daily |
| SLO-PF-02 | Page Load Time            | 95% of pages ≤ 2s       | Daily |
| SLO-PF-03 | Search Query Time         | 95% of searches ≤ 1s    | Daily |
| SLO-PF-04 | Cart Update Time          | 95% of operations ≤ 500ms | Daily |
| SLO-PF-05 | Database Query Time       | 95% of queries ≤ 100ms  | Daily |

### Reliability SLOs

| SLO ID    | Description               | Target                  | Period |
|-----------|---------------------------|-------------------------|--------|
| SLO-RL-01 | API Error Rate            | ≤ 1%                    | Daily  |
| SLO-RL-02 | Order Processing Success Rate | ≥ 99.5%             | Weekly |
| SLO-RL-03 | Payment Success Rate      | ≥ 98%                   | Weekly |
| SLO-RL-04 | Inventory Accuracy        | ≥ 99% (±1 unit)         | Daily  |

### Throughput SLOs

| SLO ID    | Description               | Target                  | Period |
|-----------|---------------------------|-------------------------|--------|
| SLO-TP-01 | Concurrent Users Support  | ≥ 100 users             | Peak hours |
| SLO-TP-02 | API Request Capacity      | ≥ 500 req/s             | Sustained |
| SLO-TP-03 | Order Processing Capacity | ≥ 50 orders/min         | Peak hours |

---

## Frontend Performance SLIs (Sprint #5)

Frontend-level observability to track user experience and interactions.

### Frontend Core Web Vitals

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-FE-01  | Page Load Time              | 95% of pages load within 2s |
| SLI-FE-02  | Time to Interactive (TTI)   | 95% achieve TTI within 3s |
| SLI-FE-03  | Largest Contentful Paint    | 95% ≤ 2.5s |
| SLI-FE-04  | First Input Delay (FID)     | 95% ≤ 100ms |
| SLI-FE-05  | Cumulative Layout Shift     | 95% ≤ 0.1 |

### Frontend Reliability SLIs

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-FE-06  | Frontend Error Rate         | % of unhandled exceptions |
| SLI-FE-07  | User Interaction Latency    | 95% of clicks respond within 500ms |
| SLI-FE-08  | Resource Load Success Rate  | % of successfully loaded assets |

### User Engagement SLIs

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-UE-01  | User Session Duration       | Average time spent per session |
| SLI-UE-02  | Product View Rate           | Number of product views per session |
| SLI-UE-03  | Add to Cart Rate            | % of product views resulting in cart additions |

---

## Chatbot Performance SLIs (Sprint #5)

Conversational assistant-specific metrics for quality and performance monitoring.

### Chatbot Performance

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-CB-01  | Chat Response Time          | 95% of responses ≤ 3s |
| SLI-CB-02  | Chat API Availability       | % of successful API responses |
| SLI-CB-03  | Ollama Service Uptime       | % of time LLM service is available |

### Chatbot Quality SLIs

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-CB-04  | Chat Helpfulness Rate       | % of "helpful" feedback from users |
| SLI-CB-05  | Chat Error Rate             | % of failed generation attempts |
| SLI-CB-06  | Context Relevance           | % of responses using product context |
| SLI-CB-07  | Conversation Completion     | % of conversations with >3 exchanges |

### Chatbot Efficiency SLIs

| SLI ID     | Metric                      | Measurement |
|------------|-----------------------------|-------------|
| SLI-CB-08  | Token Usage Efficiency      | Average tokens per conversation |
| SLI-CB-09  | RAG Context Hit Rate        | % of queries where products are found |
| SLI-CB-10  | Output Validation Pass Rate | % of LLM outputs passing quality checks |

---

## Updated Service Level Objectives (SLOs)

### Frontend Performance SLOs

| SLO ID     | Description               | Target                  | Period |
|------------|---------------------------|-------------------------|--------|
| SLO-FE-01  | Page Load Performance     | 95% ≤ 2s                | Daily  |
| SLO-FE-02  | Largest Contentful Paint  | 95% ≤ 2.5s              | Daily  |
| SLO-FE-03  | First Input Delay         | 95% ≤ 100ms             | Daily  |
| SLO-FE-04  | Cumulative Layout Shift   | 95% ≤ 0.1               | Daily  |
| SLO-FE-05  | Frontend Stability        | Error rate ≤ 0.5%       | Daily  |
| SLO-FE-06  | User Interaction Response | 95% ≤ 500ms             | Daily  |

### Chatbot SLOs

| SLO ID     | Description               | Target                  | Period  |
|------------|---------------------------|-------------------------|---------|
| SLO-CB-01  | Chatbot Responsiveness    | 95% ≤ 3s                | Daily   |
| SLO-CB-02  | Chatbot Helpfulness       | ≥ 70% positive feedback | Weekly  |
| SLO-CB-03  | Chatbot Availability      | ≥ 99% uptime            | Monthly |
| SLO-CB-04  | Chat API Success Rate     | ≥ 99.5%                 | Daily   |
| SLO-CB-05  | Output Quality            | ≥ 95% pass validation   | Weekly  |
| SLO-CB-06  | RAG Context Accuracy      | ≥ 80% hit rate          | Weekly  |

---

## Implementation Notes (Sprint #5)

### Frontend Observability

The frontend now sends telemetry data to the backend via:
- **OpenTelemetry Web SDK** for traces and events
- **Core Web Vitals API** for performance metrics
- **Custom analytics endpoint** at `/api/analytics/track`

Events tracked:
- Page views
- User interactions (clicks, navigation)
- Product views and cart actions
- Chatbot interactions
- Client-side errors

### Chatbot Observability

Chatbot metrics are collected from:
- **Response latency** from OllamaService
- **Token usage** from LLM API responses
- **User feedback** via feedback buttons
- **Output validation** pass/fail rates
- **RAG context** retrieval success

Metrics are stored in:
- `chatbot_messages` table (latency, tokens)
- `chatbot_quality_metrics` table (feedback, scores)
- Prometheus metrics exposed by backend

### Grafana Dashboards

Two new dashboards have been created:
1. **Frontend Performance Dashboard** - Core Web Vitals, page load times, user interactions
2. **Chatbot Analytics Dashboard** - Response times, feedback distribution, token usage

Dashboard JSON files located at:
- `terraform/modules/monitoring/dashboards/frontend-performance.json`
- `terraform/modules/monitoring/dashboards/chatbot-analytics.json`

### Alerting Rules

Alerts configured for:
- Frontend error rate > 1%
- Page load p95 > 3s
- Chatbot response time p95 > 5s
- Chatbot helpful feedback < 60%
- Ollama service down

---

## Monitoring Stack

Our observability stack consists of:

- **OpenTelemetry Collector** - Receives traces and metrics from frontend and backend
- **Prometheus** - Stores time-series metrics
- **Loki** - Log aggregation (optional for Level 1-2)
- **Grafana** - Visualization and dashboards

All services deployed via Terraform in `terraform/modules/monitoring/`.

---

## Querying Metrics

### Example PromQL Queries

**Frontend Page Load p95:**
```promql
histogram_quantile(0.95, rate(web_vitals_lcp_seconds_bucket[5m]))
```

**Chatbot Response Time p95:**
```promql
histogram_quantile(0.95, rate(chatbot_response_latency_ms_bucket[5m]))
```

**Chatbot Helpfulness Ratio:**
```promql
chatbot_feedback_total{type="helpful"} / chatbot_feedback_total
```

**Frontend Error Rate:**
```promql
rate(frontend_errors_total[5m]) / rate(frontend_events_total[5m])
```

---

**Last Updated**: Sprint #5 - 2025-11-26
**Maintained By**: ShopHub Engineering Team