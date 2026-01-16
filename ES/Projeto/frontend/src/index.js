import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ReactKeycloakProvider } from '@react-keycloak/web';
import flagsmith from 'flagsmith';
import { FlagsmithProvider } from 'flagsmith/react';
import './telemetry'; // Initialize OpenTelemetry
import './assets/styles/index.css';
import App from './App';
import keycloak from './config/keycloak';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));

// Detect if we're in a secure context (HTTPS or localhost)
// PKCE S256 requires Web Crypto API which is only available in secure contexts
const isSecureContext = window.isSecureContext;

console.log('=== KEYCLOAK INIT DEBUG ===');
console.log('Secure Context:', isSecureContext);
console.log('Protocol:', window.location.protocol);
console.log('PKCE Method:', isSecureContext ? 'S256' : 'disabled (HTTP mode)');
console.log('===========================');

const flagsmithEnvironmentId = process.env.REACT_APP_FLAGSMITH_ENVIRONMENT_ID;
const flagsmithApi = process.env.REACT_APP_FLAGSMITH_API_URL;
const flagsmithDefaultIdentity = process.env.REACT_APP_FLAGSMITH_DEFAULT_IDENTITY;
const flagsmithEnableAnalytics = process.env.REACT_APP_FLAGSMITH_ENABLE_ANALYTICS === 'true';

const buildFlagsmithOptions = () => {
  if (!flagsmithEnvironmentId) {
    console.warn('Flagsmith environment ID is not configured. Feature flags are disabled.');
    return null;
  }

  const options = {
    environmentID: flagsmithEnvironmentId,
  };

  if (flagsmithApi) {
    options.api = flagsmithApi;
  }

  if (flagsmithDefaultIdentity) {
    options.identity = flagsmithDefaultIdentity;
  }

  if (flagsmithEnableAnalytics) {
    options.enableAnalytics = true;
  }

  return options;
};

const flagsmithOptions = buildFlagsmithOptions();
if (flagsmithOptions) {
  console.log('Flagsmith initialized with environment:', flagsmithOptions.environmentID);
  if (flagsmithOptions.api) {
    console.log('Flagsmith API override:', flagsmithOptions.api);
  }
}

// Simplified initialization - no SSO check
const keycloakProviderInitConfig = {
  onLoad: 'check-sso',
  checkLoginIframe: false, // Disable iframe check to avoid CSP issues
  pkceMethod: isSecureContext ? 'S256' : false, // Only use PKCE in secure contexts (HTTPS/localhost)
};

// Event handler for Keycloak events
const handleKeycloakEvent = (event, error) => {
  console.log('Keycloak event:', event);
  if (error) {
    console.error('Keycloak error:', error);
  }
};

// Token refresh handler
const handleKeycloakTokens = (tokens) => {
  console.log('Keycloak tokens updated:', {
    token: tokens.token ? 'present' : 'missing',
    refreshToken: tokens.refreshToken ? 'present' : 'missing',
    idToken: tokens.idToken ? 'present' : 'missing',
  });
};

const applicationTree = (
  <BrowserRouter>
    <App />
  </BrowserRouter>
);

root.render(
  // StrictMode disabled to prevent Keycloak double-initialization in development
  // React StrictMode intentionally double-renders components which causes Keycloak to throw
  // "instance can only be initialized once" error
  // <React.StrictMode>
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={keycloakProviderInitConfig}
      onEvent={handleKeycloakEvent}
      onTokens={handleKeycloakTokens}
      LoadingComponent={
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading...</p>
          </div>
        </div>
      }
    >
      {flagsmithOptions ? (
        <FlagsmithProvider flagsmith={flagsmith} options={flagsmithOptions}>
          {applicationTree}
        </FlagsmithProvider>
      ) : (
        applicationTree
      )}
    </ReactKeycloakProvider>
  // </React.StrictMode>
);

reportWebVitals();
