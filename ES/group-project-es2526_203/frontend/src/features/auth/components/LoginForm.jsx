import React from 'react';
import { Button } from '../../../components/ui/button';

const LoginForm = ({
  isRegister,
  loading,
  onSubmit
}) => {
  return (
    <div className="space-y-4">
      <div className="text-center mb-6">
        <p className="text-muted-foreground">
          {isRegister
            ? 'Create a new account with secure authentication'
            : 'Sign in with your secure account'}
        </p>
      </div>

      <Button
        type="button"
        onClick={onSubmit}
        disabled={loading}
        className="w-full"
        size="lg"
      >
        {loading ? 'Redirecting...' : (isRegister ? 'Create Account with Keycloak' : 'Sign In with Keycloak')}
      </Button>

      <div className="text-center text-sm text-muted-foreground mt-4">
        <p>🔒 Secure authentication powered by Keycloak</p>
      </div>
    </div>
  );
};

export default LoginForm;