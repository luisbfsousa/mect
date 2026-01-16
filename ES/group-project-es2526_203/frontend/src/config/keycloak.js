import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8080',
  realm: process.env.REACT_APP_KEYCLOAK_REALM || 'ShopHub',
  clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'shophub-frontend',
};

if (process.env.NODE_ENV === 'development') {
  console.log('=== KEYCLOAK DEBUG INFO ===');
  console.log('Keycloak URL:', keycloakConfig.url);
  console.log('Realm:', keycloakConfig.realm);
  console.log('Client ID:', keycloakConfig.clientId);
  console.log('Environment variables:');
  console.log('REACT_APP_KEYCLOAK_URL:', process.env.REACT_APP_KEYCLOAK_URL);
  console.log('REACT_APP_KEYCLOAK_REALM:', process.env.REACT_APP_KEYCLOAK_REALM);
  console.log('REACT_APP_KEYCLOAK_CLIENT_ID:', process.env.REACT_APP_KEYCLOAK_CLIENT_ID);
  console.log('=========================');
}

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;           
 