import { useState, useEffect, useCallback, useRef } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { ordersAPI, profileAPI } from '../services/api';

export const useAuth = (navigate) => {
  const { keycloak, initialized } = useKeycloak();
  const [orderHistory, setOrderHistory] = useState([]);
  const [loading] = useState(false); // FIXED: Removed setLoading since it's never used
  const [error, setError] = useState('');
  
  // Use ref to prevent unnecessary re-renders
  const tokenRefreshRef = useRef(null);

  // Maintain user in state so we can enrich it with backend profile
  const [user, setUser] = useState(null);

  // Seed user from token
  useEffect(() => {
    if (!keycloak.authenticated) {
      setUser(null);
      return;
    }
    const baseUser = {
      sub: keycloak.tokenParsed?.sub,
      user_id: keycloak.tokenParsed?.sub,
      id: keycloak.tokenParsed?.sub,
      name: keycloak.tokenParsed?.name || keycloak.tokenParsed?.preferred_username,
      email: keycloak.tokenParsed?.email,
      roles: (() => {
        const raw = keycloak.tokenParsed?.realm_access?.roles || [];
        const normalized = raw.map(r => r.replace(/_/g, '-'));
        return Array.from(new Set([...raw, ...normalized]));
      })(),
    };
    setUser(baseUser);
  }, [keycloak.authenticated, keycloak.tokenParsed]);

  // Enrich with backend profile so name reflects admin edits
  useEffect(() => {
    const loadProfile = async () => {
      if (!keycloak.authenticated) return;
      try {
        const profile = await profileAPI.getProfile();

        const deactivated = profile?.isDeactivated ?? profile?.deactivated ?? profile?.is_deactivated;
        if (deactivated) {
          alert('Your account is deactivated');
          try {
            await keycloak.logout({ redirectUri: window.location.origin });
          } catch (err) {
            console.error('Logout failed', err);
          }
          return;
        }
        const firstName = profile.firstName ?? profile.first_name;
        const lastName = profile.lastName ?? profile.last_name;
        const email = profile.email;
        setUser(prev => prev ? ({
          ...prev,
          name: [firstName, lastName].filter(Boolean).join(' ') || prev.name,
          email: email || prev.email,
        }) : prev);
      } catch (e) {
        // Non-fatal; keep token-derived name
        console.debug('Profile fetch failed; using token name', e);
      }
    };
    loadProfile();
  }, [keycloak.authenticated]);

  const token = keycloak.token;

  // OPTIMIZED: Token refresh - only when necessary, not constantly
  useEffect(() => {
    if (!keycloak.authenticated || !initialized) return;

    // Clear any existing interval
    if (tokenRefreshRef.current) {
      clearInterval(tokenRefreshRef.current);
    }

    // Function to refresh token only if it's expiring soon
    const refreshTokenIfNeeded = () => {
      // Update token if it expires in less than 5 minutes (300 seconds)
      keycloak.updateToken(300)
        .then(refreshed => {
          if (refreshed) {
            console.log('Token was refreshed');
          }
        })
        .catch(error => {
          console.error('Token refresh failed:', error);
          // Only logout if token is actually expired, not on network errors
          if (error.error === 'invalid_grant') {
            keycloak.logout();
          }
        });
    };

    // Initial refresh check
    refreshTokenIfNeeded();

    // Check every 60 seconds (not 4 minutes - this is just checking, not refreshing)
    tokenRefreshRef.current = setInterval(refreshTokenIfNeeded, 60000);

    return () => {
      if (tokenRefreshRef.current) {
        clearInterval(tokenRefreshRef.current);
      }
    };
  }, [keycloak, initialized, keycloak.authenticated]);

  // Fetch orders only once when authenticated - don't refetch on every token change
  const hasFetchedOrders = useRef(false);
  
  // FIXED: Moved fetchUserOrders function definition here
  const fetchUserOrders = useCallback(async () => {
    try {
      const orders = await ordersAPI.fetchOrders();
      setOrderHistory(orders);
    } catch (error) {
      console.error('Failed to fetch orders:', error);
      setOrderHistory([]);
    }
  }, []); // No dependencies needed since ordersAPI is stable
  
  useEffect(() => {
    if (keycloak.authenticated && token && !hasFetchedOrders.current) {
      fetchUserOrders();
      hasFetchedOrders.current = true;
    } else if (!keycloak.authenticated) {
      setOrderHistory([]);
      hasFetchedOrders.current = false;
    }
  }, [keycloak.authenticated, token, fetchUserOrders]); // FIXED: Added token and fetchUserOrders

  const handleRegister = useCallback(() => {
    keycloak.register({
      redirectUri: window.location.origin
    });
  }, [keycloak]);

  const handleLogin = useCallback(() => {
    keycloak.login({
      redirectUri: window.location.origin
    });
  }, [keycloak]);

  const handleLogout = useCallback(() => {
    setOrderHistory([]);
    hasFetchedOrders.current = false;
    keycloak.logout({
      redirectUri: window.location.origin
    });
  }, [keycloak]);

  const hasRole = useCallback((role) => {
    if (!keycloak.authenticated) return false;
    const userRoles = keycloak.tokenParsed?.realm_access?.roles || [];
    return userRoles.includes(role);
  }, [keycloak.authenticated, keycloak.tokenParsed]);

  const hasAnyRole = useCallback((roles) => {
    if (!keycloak.authenticated) return false;
    const userRoles = keycloak.tokenParsed?.realm_access?.roles || [];
    return roles.some(role => userRoles.includes(role));
  }, [keycloak.authenticated, keycloak.tokenParsed]);

  return {
    user,
    token,
    orderHistory,
    setOrderHistory,
    loading,
    error,
    setError,
    handleRegister,
    handleLogin,
    handleLogout,
    isAuthenticated: keycloak.authenticated,
    initialized,
    hasRole,
    hasAnyRole,
    // Expose method to manually refresh orders if needed
    refreshOrders: fetchUserOrders
  };
};