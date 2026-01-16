import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import LoginForm from '../components/LoginForm';
import ErrorAlert from '../../../components/ErrorAlert';

const LoginView = ({ error, loading, handleRegister, handleLogin, setError }) => {
  const [isRegister, setIsRegister] = useState(false);
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();

  // Redirect if already authenticated
  useEffect(() => {
    if (keycloak.authenticated) {
      navigate('/');
    }
  }, [keycloak.authenticated, navigate]);

  const handleSubmit = () => {
    setError('');
    if (isRegister) {
      handleRegister();
    } else {
      handleLogin();
    }
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h2 className="text-3xl font-bold text-center mb-6 text-gray-800 dark:text-gray-100">
          {isRegister ? 'Create Account' : 'Welcome Back'}
        </h2>
        
        {error && <ErrorAlert message={error} />}
        
        <LoginForm
          isRegister={isRegister}
          loading={loading}
          onSubmit={handleSubmit}
        />
        
        <div className="mt-6 text-center">
          <button
            onClick={() => setIsRegister(!isRegister)}
            className="text-blue-600 hover:text-blue-700 text-sm font-medium"
          >
            {isRegister 
              ? 'Already have an account? Sign in' 
              : "Don't have an account? Create one"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default LoginView;