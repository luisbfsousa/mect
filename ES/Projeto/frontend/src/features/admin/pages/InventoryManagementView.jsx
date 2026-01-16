import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { useNavigate } from 'react-router-dom';
import { Package, AlertTriangle, TrendingUp, Search, Save, X } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import ErrorAlert from '../../../components/ErrorAlert';
import { API_BASE_URL } from '../../../services/api';
import AdminNavTabs from '../components/AdminNavTabs';
import { useInventoryStats } from '../../../hooks/useInventoryStats';

const InventoryManagementView = () => {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const { refresh: refreshInventoryStats } = useInventoryStats();
  const [products, setProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingStocks, setEditingStocks] = useState({});
  const [savingProductIds, setSavingProductIds] = useState(new Set());

  useEffect(() => {
    fetchProducts();
  }, [keycloak.token]);

  useEffect(() => {
    filterProducts();
  }, [searchTerm, products]);

  const fetchProducts = async () => {
    if (!keycloak.token) return;

    try {
      setLoading(true);
      setError('');
      const response = await fetch(`${API_BASE_URL}/admin/products`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch products');
      }

      const data = await response.json();
      setProducts(data);
      setFilteredProducts(data);
    } catch (err) {
      console.error('Error fetching products:', err);
      setError('Failed to load products: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const filterProducts = () => {
    if (!searchTerm.trim()) {
      setFilteredProducts(products);
      return;
    }

    const term = searchTerm.toLowerCase();
    const filtered = products.filter(product =>
      product.name.toLowerCase().includes(term) ||
      product.sku?.toLowerCase().includes(term) ||
      product.product_id?.toString().includes(term)
    );
    setFilteredProducts(filtered);
  };

  const handleStockChange = (productId, newValue) => {
    const value = newValue === '' ? '' : Number.isNaN(parseInt(newValue, 10)) ? '' : parseInt(newValue, 10);
    setEditingStocks(prev => ({
      ...prev,
      [productId]: value
    }));
  };

  const handleSaveStock = async (product) => {
    const productId = product.product_id || product.productId;
    const newStock = editingStocks[productId];

    if (newStock === undefined || newStock === product.stock_quantity || newStock === product.stockQuantity) {
      // No change, just cancel editing
      setEditingStocks(prev => {
        const updated = { ...prev };
        delete updated[productId];
        return updated;
      });
      return;
    }

    try {
      setSavingProductIds(prev => new Set(prev).add(productId));
      setError('');
      setSuccess('');

      const response = await fetch(`${API_BASE_URL}/admin/products/${productId}/inventory`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          stockQuantity: newStock
        })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to update inventory');
      }

      const updatedProduct = await response.json();
      
      // Update the products list
      setProducts(prev => prev.map(p => 
        (p.product_id || p.productId) === productId ? updatedProduct : p
      ));

      // Clear editing state for this product
      setEditingStocks(prev => {
        const updated = { ...prev };
        delete updated[productId];
        return updated;
      });

      setSuccess(`Stock updated successfully for ${product.name}`);
      setTimeout(() => setSuccess(''), 3000);
      
      // Refresh inventory stats after updating stock
      refreshInventoryStats();
    } catch (err) {
      console.error('Error updating inventory:', err);
      setError('Failed to update inventory: ' + err.message);
    } finally {
      setSavingProductIds(prev => {
        const updated = new Set(prev);
        updated.delete(productId);
        return updated;
      });
    }
  };

  const handleCancelEdit = (productId) => {
    setEditingStocks(prev => {
      const updated = { ...prev };
      delete updated[productId];
      return updated;
    });
  };

  const getStockStatusVariant = (stock) => {
    if (stock === 0) return 'destructive';
    if (stock < 10) return 'warning';
    return 'default';
  };

  const getStockStatusIcon = (stock) => {
    if (stock === 0) return <AlertTriangle size={16} className="inline mr-1" />;
    if (stock < 10) return <AlertTriangle size={16} className="inline mr-1" />;
    return <TrendingUp size={16} className="inline mr-1" />;
  };

  const lowStockCount = products.filter(p => {
    const stock = p.stock_quantity || p.stockQuantity || 0;
    return stock > 0 && stock < 10;
  }).length;
  const outOfStockCount = products.filter(p => (p.stock_quantity || p.stockQuantity || 0) === 0).length;

  if (!keycloak.authenticated) {
    return (
      <Card className="max-w-7xl mx-auto p-6">
        <p className="text-center text-muted-foreground">Please log in to access inventory management.</p>
      </Card>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <AdminNavTabs />

      <Card className="p-6">

        {/* Low-stock alert banner for administrators */}
          {filteredProducts.some(p => (p.stock_quantity || p.stockQuantity || 0) < (p.low_stock_threshold || p.lowStockThreshold || 10)) && (
            <div className="mb-4 p-4 bg-yellow-50 dark:bg-yellow-900 border border-yellow-300 dark:border-yellow-700 text-yellow-800 dark:text-yellow-100 rounded-lg">
              <div className="font-semibold mb-2">❗Low Stock Alerts❗</div>
              <ul className="list-disc ml-5 text-sm">
                {filteredProducts
                  .filter(p => {
                    const stock = p.stock_quantity || p.stockQuantity || 0;
                    const threshold = p.low_stock_threshold || p.lowStockThreshold || 10;
                    return stock <= threshold;
                  })
                  .slice(0, 5)
                  .map(p => (
                    <li key={p.product_id || p.productId}>
                      {p.name} - {(p.stock_quantity ?? p.stockQuantity ?? 0)} Units.
                    </li>
                  ))
                }
              </ul>
            </div>
          )}

        {/* Header */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-2xl font-bold text-gray-800 dark:text-gray-100">Inventory Management</h2>
              <p className="text-gray-600 dark:text-gray-400 mt-1">Update and monitor stock levels</p>
            </div>
          </div>

        {/* Statistics Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <Card className="p-4 border-primary/20 bg-primary/5">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-primary font-medium">Total Products</p>
                  <p className="text-2xl font-bold">{products.length}</p>
                </div>
                <Package size={32} className="text-primary" />
              </div>
            </Card>
            <Card className="p-4 border-yellow-500/20 bg-yellow-50 dark:bg-yellow-900/20">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-yellow-600 dark:text-yellow-400 font-medium">Low Stock</p>
                  <p className="text-2xl font-bold text-yellow-800 dark:text-yellow-100">{lowStockCount}</p>
                </div>
                <AlertTriangle size={32} className="text-yellow-600 dark:text-yellow-400" />
              </div>
            </Card>
            <Card className="p-4 border-destructive/20 bg-destructive/5">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-destructive font-medium">Out of Stock</p>
                  <p className="text-2xl font-bold">{outOfStockCount}</p>
                </div>
                <AlertTriangle size={32} className="text-destructive" />
              </div>
            </Card>
          </div>

        {/* Search Bar */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 dark:text-gray-500" size={20} />
            <input
              type="text"
              placeholder="Search by product name, SKU, or ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            />
          </div>
        </div>

        <ErrorAlert message={error} />
        
        {success && (
          <div className="mb-4 p-4 bg-green-100 dark:bg-green-900 border border-green-400 dark:border-green-700 text-green-700 dark:text-green-100 rounded-lg flex items-center">
            <Save size={20} className="mr-2" />
            {success}
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            <p className="ml-4 text-gray-600 dark:text-gray-400">Loading inventory...</p>
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="text-center py-12">
            <Package size={64} className="mx-auto text-gray-400 dark:text-gray-500 mb-4" />
            <p className="text-gray-600 dark:text-gray-400 text-lg">
              {searchTerm ? 'No products found matching your search' : 'No products available'}
            </p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Product</TableHead>
                <TableHead>SKU</TableHead>
                <TableHead>Current Stock</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
                {filteredProducts.map((product) => {
                  const productId = product.product_id || product.productId;
                  const currentStock = product.stock_quantity || product.stockQuantity || 0;
                  const isEditing = editingStocks.hasOwnProperty(productId);
                  const isSaving = savingProductIds.has(productId);
                  const editValue = editingStocks[productId] ?? currentStock;

                  return (
                    <TableRow key={productId}>
                      <TableCell>
                        <div className="font-medium">#{productId}</div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          {product.images && product.images.length > 0 ? (
                            <img 
                              src={product.images[0]} 
                              alt={product.name}
                              className="w-12 h-12 object-cover rounded border border-gray-300 dark:border-gray-600"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.src = 'https://via.placeholder.com/48/e5e7eb/9ca3af?text=No+Image';
                              }}
                            />
                          ) : (
                            <div className="w-12 h-12 bg-gray-200 dark:bg-gray-600 rounded border border-gray-300 dark:border-gray-600 flex items-center justify-center text-gray-400 dark:text-gray-500 text-xs">
                              No Image
                            </div>
                          )}
                          <div>
                            <div className="font-medium text-gray-900 dark:text-gray-100">{product.name}</div>
                            <div className="text-sm text-gray-500 dark:text-gray-400 truncate max-w-xs">
                              {product.description}
                            </div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm text-muted-foreground">
                          {product.sku || 'N/A'}
                        </span>
                      </TableCell>
                      <TableCell>
                        {isEditing ? (
                          <input
                            type="number"
                            min="0"
                            value={editValue === '' ? '' : editValue}
                            onChange={(e) => handleStockChange(productId, e.target.value)}
                            className="w-24 px-2 py-1 border border-blue-500 rounded focus:ring-2 focus:ring-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                            disabled={isSaving}
                          />
                        ) : (
                          <span className="font-semibold">{currentStock} units</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge variant={getStockStatusVariant(currentStock)} className="gap-1">
                          {getStockStatusIcon(currentStock)}
                          {currentStock === 0 ? 'Out of Stock' : currentStock < 10 ? 'Low Stock' : 'In Stock'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-2">
                          {isEditing ? (
                            <>
                              <Button
                                onClick={() => handleSaveStock(product)}
                                disabled={isSaving}
                                size="sm"
                                className="gap-1"
                              >
                                {isSaving ? (
                                  <>
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                                    Saving...
                                  </>
                                ) : (
                                  <>
                                    <Save size={14} />
                                    Save
                                  </>
                                )}
                              </Button>
                              <Button
                                onClick={() => handleCancelEdit(productId)}
                                disabled={isSaving}
                                variant="outline"
                                size="sm"
                                className="gap-1"
                              >
                                <X size={14} />
                                Cancel
                              </Button>
                            </>
                          ) : (
                            <Button
                              onClick={() => setEditingStocks(prev => ({ ...prev, [productId]: currentStock }))}
                              size="sm"
                            >
                              Update Stock
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
            </TableBody>
          </Table>
        )}
      </Card>
    </div>
  );
};

export default InventoryManagementView;
