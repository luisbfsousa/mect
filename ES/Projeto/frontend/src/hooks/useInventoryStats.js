import { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { API_BASE_URL } from '../services/api';

export const useInventoryStats = ({ enabled = true } = {}) => {
  const { keycloak } = useKeycloak();
  const [lowStockCount, setLowStockCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const isAdmin = keycloak.authenticated && 
    keycloak.tokenParsed?.realm_access?.roles?.includes('administrator');

  const fetchInventoryStats = async () => {
    if (!enabled || !isAdmin || !keycloak.token) {
      setLowStockCount(0);
      return;
    }

    try {
      setLoading(true);
      await keycloak.updateToken(5);

      const response = await fetch(`${API_BASE_URL}/admin/products`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch products');
      }

      const products = await response.json();
      
      // Calculate low-stock count
      const lowStock = products.filter(p => {
        const stock = p.stock_quantity || p.stockQuantity || 0;
        const threshold = p.low_stock_threshold || p.lowStockThreshold || 10;
        return stock > 0 && stock <= threshold;
      }).length;

      setLowStockCount(lowStock);
    } catch (error) {
      console.error('Failed to fetch inventory stats:', error);
      setLowStockCount(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (enabled && isAdmin) {
      fetchInventoryStats();
      // Refresh every 5 minutes
      const interval = setInterval(fetchInventoryStats, 5 * 60 * 1000);
      return () => clearInterval(interval);
    }
  }, [enabled, isAdmin, keycloak.token]);

  return {
    lowStockCount: enabled ? lowStockCount : 0,
    loading,
    refresh: fetchInventoryStats
  };
};
