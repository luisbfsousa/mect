import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';

// Get OTEL endpoint from environment or use default
// Extract base URL from API URL and construct OTEL endpoint
const API_BASE_URL = process.env.REACT_APP_API_URL
  ? process.env.REACT_APP_API_URL.replace('/api', '')
  : 'http://localhost';
const OTEL_EXPORTER_OTLP_ENDPOINT =
  process.env.REACT_APP_OTEL_ENDPOINT ||
  `${API_BASE_URL}:4318/v1/traces`;

console.log('🔭 Initializing OpenTelemetry...');
console.log('📍 OTLP Endpoint:', OTEL_EXPORTER_OTLP_ENDPOINT);

// Build URL patterns from environment variables
const KEYCLOAK_BASE_URL = process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080';
const FLAGSMITH_BASE_URL = process.env.REACT_APP_FLAGSMITH_API_URL
  ? process.env.REACT_APP_FLAGSMITH_API_URL.replace('/api/v1', '')
  : 'http://localhost:8000';

// Escape special regex characters and create patterns
const escapeRegex = (str) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const apiUrlPattern = new RegExp(escapeRegex(process.env.REACT_APP_API_URL || 'http://localhost/api') + '.*');
const keycloakTokenPattern = new RegExp(escapeRegex(KEYCLOAK_BASE_URL) + '/realms/.*/protocol/openid-connect/token');
const flagsmithPattern = new RegExp(escapeRegex(FLAGSMITH_BASE_URL) + ':8000/api/v1/flags/?');

// Create resource with service information
const resource = new Resource({
  [SemanticResourceAttributes.SERVICE_NAME]: 'shophub-frontend',
  [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
  [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.NODE_ENV || 'development',
});

// Create OTLP exporter
const exporter = new OTLPTraceExporter({
  url: OTEL_EXPORTER_OTLP_ENDPOINT,
  headers: {},
});

// Create tracer provider
const provider = new WebTracerProvider({
  resource: resource,
});

// Add batch span processor
provider.addSpanProcessor(new BatchSpanProcessor(exporter, {
  maxQueueSize: 100,
  maxExportBatchSize: 10,
  scheduledDelayMillis: 500,
  exportTimeoutMillis: 30000,
}));

// Register the provider
provider.register();

// Auto-instrument web APIs
registerInstrumentations({
  instrumentations: [
    getWebAutoInstrumentations({
      '@opentelemetry/instrumentation-document-load': {
        enabled: true,
      },
      '@opentelemetry/instrumentation-fetch': {
        enabled: true,
        propagateTraceHeaderCorsUrls: [
          apiUrlPattern,
          /http:\/\/localhost\/api.*/,
        ],
        ignoreUrls: [
          keycloakTokenPattern,
          flagsmithPattern,
        ],
        clearTimingResources: true,
      },
      '@opentelemetry/instrumentation-xml-http-request': {
        enabled: true,
        propagateTraceHeaderCorsUrls: [
          apiUrlPattern,
          /http:\/\/localhost\/api.*/,
        ],
        ignoreUrls: [
          keycloakTokenPattern,
          flagsmithPattern,
        ],
      },
      '@opentelemetry/instrumentation-user-interaction': {
        enabled: true,
        eventNames: ['click', 'submit'],
      },
    }),
  ],
});

console.log('✅ OpenTelemetry initialized successfully');

export default provider;
