import React from 'react';
import { Navigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { ShieldAlert } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';

const ProtectedRoute = ({ children, roles = [] }) => {
  const { keycloak, initialized } = useKeycloak();

  // Wait for Keycloak to initialize
  if (!initialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // If not authenticated, redirect to login
  if (!keycloak.authenticated) {
    keycloak.login();
    return null;
  }

  // If roles are specified, check if user has at least one required role
  if (roles.length > 0) {
    const rawRoles = keycloak.tokenParsed?.realm_access?.roles || [];
    const normalized = rawRoles.map(r => r.replace(/_/g, '-'));
    const userRoles = Array.from(new Set([...rawRoles, ...normalized]));
    const hasRequiredRole = roles.some(role => userRoles.includes(role));

    if (!hasRequiredRole) {
      return (
        <div className="flex items-center justify-center min-h-screen">
          <Card className="max-w-md">
            <CardHeader className="text-center">
              <ShieldAlert className="mx-auto h-16 w-16 text-destructive mb-4" />
              <CardTitle className="text-2xl">Access Denied</CardTitle>
              <CardDescription>
                You don't have permission to access this page.
              </CardDescription>
            </CardHeader>
            <CardContent className="text-center">
              <p className="text-sm text-muted-foreground mb-4">
                Required role: {roles.join(' or ')}
              </p>
              <Button onClick={() => window.history.back()}>
                Go Back
              </Button>
            </CardContent>
          </Card>
        </div>
      );
    }
  }

  return children;
};

export default ProtectedRoute;