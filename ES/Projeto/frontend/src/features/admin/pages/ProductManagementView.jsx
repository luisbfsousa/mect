import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { useNavigate } from 'react-router-dom';
import { Package } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../../../components/ui/dialog';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import ErrorAlert from '../../../components/ErrorAlert';
import { API_BASE_URL } from '../../../services/api';
import AdminNavTabs from '../components/AdminNavTabs';

const ProductManagementView = () => {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [search, setSearch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [currentProduct, setCurrentProduct] = useState({
    name: '',
    description: '',
    price: 0,
    category_id: 1,
    stock_quantity: 0,
    sku: '',
    images: [],
    specifications: {}
  });
  const [imageInput, setImageInput] = useState('');

  useEffect(() => {
    if (keycloak.authenticated) {
      fetchProducts();
    }
  }, [keycloak.authenticated]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);
      
      const response = await fetch(`${API_BASE_URL}/admin/products`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('Error response:', errorText);
        throw new Error(`Failed to fetch products: ${response.status}`);
      }
      
      const data = await response.json();
      console.log('Fetched products:', data);
      setProducts(data);
    } catch (err) {
      console.error('Error fetching products:', err);
      setError('Failed to load products. Please check your permissions.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddProduct = () => {
    setEditingProduct(null);
    setCurrentProduct({
      name: '',
      description: '',
      price: 0,
      category_id: 1,
      stock_quantity: 0,
      sku: '',
      images: [],
      specifications: {}
    });
    setImageInput('');
    setIsModalOpen(true);
    setError('');
    setSuccess('');
  };

  const handleEditProduct = (product) => {
    setEditingProduct(product);
    setCurrentProduct({
      name: product.name,
      description: product.description,
      price: product.price,
      category_id: product.category_id || product.categoryId,
      stock_quantity: product.stock_quantity || product.stockQuantity,
      sku: product.sku,
      images: product.images || [],
      specifications: product.specifications || {}
    });
    setImageInput('');
    setIsModalOpen(true);
    setError('');
    setSuccess('');
  };

  const handleDeleteProduct = async (id) => {
    if (!window.confirm('Are you sure you want to delete this product?')) {
      return;
    }

    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);
      
      const response = await fetch(`${API_BASE_URL}/admin/products/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('Delete error:', errorText);
        throw new Error(`Failed to delete product: ${response.status}`);
      }
      
      setSuccess('Product deleted successfully!');
      setTimeout(() => setSuccess(''), 3000);
      fetchProducts();
    } catch (err) {
      console.error('Error deleting product:', err);
      setError('Failed to delete product. ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProduct = async (e) => {
    e.preventDefault();
    
    try {
      setLoading(true);
      setError('');
      setSuccess('');
      
      await keycloak.updateToken(5);

      const url = editingProduct 
        ? `${API_BASE_URL}/admin/products/${editingProduct.product_id || editingProduct.productId}`
        : `${API_BASE_URL}/admin/products`;
      
      const method = editingProduct ? 'PUT' : 'POST';

      const payload = {
        ...currentProduct,
        price: currentProduct.price === '' ? null : parseFloat(currentProduct.price),
        stock_quantity: currentProduct.stock_quantity === '' ? null : parseInt(currentProduct.stock_quantity, 10),
        category_id: currentProduct.category_id === '' ? null : parseInt(currentProduct.category_id, 10),
      };

      console.log('Saving product with images:', currentProduct.images);

      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(currentProduct)
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Save error:', errorText);
        throw new Error(`Failed to save product: ${response.status}`);
      }

      setSuccess(editingProduct ? 'Product updated successfully!' : 'Product added successfully!');
      setTimeout(() => setSuccess(''), 3000);
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      console.error('Error saving product:', err);
      setError((editingProduct ? 'Failed to update product. ' : 'Failed to add product. ') + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setCurrentProduct(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleAddImage = () => {
    const trimmedInput = imageInput.trim();
    
    if (!trimmedInput) {
      setError('Please enter an image URL');
      setTimeout(() => setError(''), 3000);
      return;
    }
    
    // Validate URL format
    try {
      const url = new URL(trimmedInput);
      if (!url.protocol.startsWith('http')) {
        throw new Error('URL must start with http:// or https://');
      }
      
      setCurrentProduct(prev => {
        const newImages = [...(prev.images || []), trimmedInput];
        console.log('✅ Image added to array:', newImages);
        return {
          ...prev,
          images: newImages
        };
      });
      
      setImageInput('');
      console.log('✅ Image input cleared');
      
    } catch (err) {
      console.error('❌ Invalid URL:', err);
      setError('Please enter a valid URL (must start with http:// or https://)');
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleRemoveImage = (index) => {
    setCurrentProduct(prev => ({
      ...prev,
      images: prev.images.filter((_, i) => i !== index)
    }));
  };

  const handleImageInputKeyPress = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddImage();
    }
  };

  if (!keycloak.authenticated) {
    return (
      <Card className="max-w-7xl mx-auto">
        <CardContent className="p-6">
          <p className="text-center text-muted-foreground">Please log in to access product management.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <AdminNavTabs />

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="mb-4">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Product Management</h2>
              <p className="mt-1 text-sm text-gray-600 dark:text-gray-300">Manage your product catalog</p>
            </div>
            <Button
              onClick={handleAddProduct}
            >
              Add Product
            </Button>
          </div>

          <div className="flex items-center gap-3">
            <Input
              placeholder="Search by name, description or SKU"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-gray-100 dark:bg-gray-800 placeholder:text-gray-500 dark:placeholder:text-gray-400 border-gray-200 dark:border-gray-700"
            />
          </div>

        </CardHeader>

        <CardContent>
          {success && (
            <Card className="mb-4 border-green-500">
              <div className="p-4 bg-green-50 dark:bg-green-950 text-green-700 dark:text-green-300">
                {success}
              </div>
            </Card>
          )}

          {loading && products.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
              <p className="ml-4 text-muted-foreground">Loading products...</p>
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-12">
              <Package className="mx-auto h-16 w-16 text-muted-foreground mb-4" />
              <h3 className="text-xl font-semibold mb-2">No Products Found</h3>
              <p className="text-muted-foreground">Start by adding your first product to the catalog.</p>
            </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Image</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Category</TableHead>
                <TableHead>Price</TableHead>
                <TableHead>Stock</TableHead>
                <TableHead>SKU</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {products
                .filter(p => {
                    const q = search.trim().toLowerCase();
                    if (!q) return true;
                    return (
                      (p.name || '').toLowerCase().includes(q) ||
                      (p.description || '').toLowerCase().includes(q) ||
                      (p.sku || '').toLowerCase().includes(q)
                    );
                  })
                .map((product) => (
                  <TableRow key={product.product_id || product.productId}>
                    <TableCell>
                      <div className="font-bold">{product.product_id || product.productId}</div>
                    </TableCell>
                    <TableCell>
                        {product.images && product.images.length > 0 ? (
                          <img 
                            src={product.images[0]} 
                            alt={product.name}
                            className="w-16 h-16 object-cover rounded"
                            onError={(e) => {
                              e.target.src = 'https://via.placeholder.com/100?text=No+Image';
                            }}
                          />
                        ) : (
                          <div className="w-16 h-16 bg-gray-200 rounded flex items-center justify-center text-gray-400">
                            No Image
                          </div>
                        )}
                    </TableCell>
                    <TableCell>
                      <div className="font-medium">{product.name}</div>
                      <div className="text-sm text-muted-foreground truncate max-w-[200px]">
                        {product.description}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">
                        {product.category_name || product.categoryName || `ID: ${product.category_id || product.categoryId || 'N/A'}`}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <span className="font-semibold text-green-600 dark:text-green-400">
                        {parseFloat(product.price).toFixed(2)} €
                      </span>
                    </TableCell>
                    <TableCell>
                      <Badge variant={
                        (product.stock_quantity ?? product.stockQuantity ?? 0) > 10 ? 'success' :
                        (product.stock_quantity ?? product.stockQuantity ?? 0) > 0 ? 'warning' :
                        'danger'
                      }>
                        {product.stock_quantity ?? product.stockQuantity ?? 0} units
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <span className="text-sm text-muted-foreground">
                        {product.sku || 'N/A'}
                      </span>
                    </TableCell>
                    <TableCell>
                        <div className="flex space-x-2">
                          <Button
                            onClick={() => handleEditProduct(product)}
                            size="sm"
                          >
                            Edit
                          </Button>
                          <Button
                            onClick={() => handleDeleteProduct(product.product_id || product.productId)}
                            variant="ghost"
                            size="sm"
                            className="text-destructive hover:text-destructive"
                          >
                            Delete
                          </Button>
                        </div>
                    </TableCell>
                  </TableRow>
                ))}
            </TableBody>
          </Table>
          )}
        </CardContent>
      </Card>

      {/* Modal */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingProduct ? 'Edit Product' : 'Add New Product'}
            </DialogTitle>
          </DialogHeader>

          {/* Inline error banner inside modal */}
          {error && (
            <div className="mb-4 p-3 bg-destructive/10 border border-destructive/20 text-destructive rounded">
              {error}
            </div>
          )}

          <form onSubmit={handleSaveProduct}>
            <div className="space-y-4">
              <div>
                <Label htmlFor="name">
                  Product Name *
                </Label>
                <Input
                  id="name"
                  type="text"
                  name="name"
                  value={currentProduct.name}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div>
                <Label htmlFor="description">
                  Description
                </Label>
                <textarea
                  id="description"
                  name="description"
                  value={currentProduct.description}
                  onChange={handleInputChange}
                  rows="3"
                  className="w-full px-3 py-2 border border-input rounded-md bg-background focus:ring-2 focus:ring-ring"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="price">
                    Price *
                  </Label>
                  <Input
                    id="price"
                    type="number"
                    name="price"
                    value={currentProduct.price}
                    onChange={handleInputChange}
                    step="0.01"
                    min="0"
                    required
                  />
                </div>

                <div>
                  <Label htmlFor="stock_quantity">
                    Stock Quantity *
                  </Label>
                  <Input
                    id="stock_quantity"
                    type="number"
                    name="stock_quantity"
                    value={currentProduct.stock_quantity}
                    onChange={handleInputChange}
                    min="0"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="sku">
                    SKU
                  </Label>
                  <Input
                    id="sku"
                    type="text"
                    name="sku"
                    value={currentProduct.sku}
                    onChange={handleInputChange}
                  />
                </div>

                <div>
                  <Label htmlFor="category_id">
                    Category ID *
                  </Label>
                  <Input
                    id="category_id"
                    type="number"
                    name="category_id"
                    value={currentProduct.category_id}
                    onChange={handleInputChange}
                    min="1"
                    required
                  />
                </div>
              </div>

              {/* Image Management Section */}
              <div className="border-t pt-4">
                <Label className="mb-2">
                  Product Images (Current: {currentProduct.images?.length || 0})
                </Label>

                {/* Add Image Input */}
                <div className="flex gap-2 mb-3">
                  <Input
                    type="url"
                    value={imageInput}
                    onChange={(e) => setImageInput(e.target.value)}
                    onKeyPress={handleImageInputKeyPress}
                    placeholder="https://images.unsplash.com/photo-..."
                    className="flex-1"
                  />
                  <Button
                    type="button"
                    onClick={handleAddImage}
                    disabled={!imageInput.trim()}
                    variant="default"
                    className="bg-green-600 hover:bg-green-700"
                  >
                    Add
                  </Button>
                </div>

                {/* Image Preview Grid */}
                {currentProduct.images && currentProduct.images.length > 0 ? (
                  <div className="grid grid-cols-3 gap-3">
                    {currentProduct.images.map((imageUrl, index) => (
                      <div key={index} className="relative group">
                        <img
                          src={imageUrl}
                          alt={`Product ${index + 1}`}
                          className="w-full h-24 object-cover rounded-lg border-2 border-border"
                          onLoad={() => console.log('✅ Image loaded:', imageUrl)}
                          onError={(e) => {
                            console.error('❌ Image failed to load:', imageUrl);
                            e.target.src = 'https://via.placeholder.com/150?text=Invalid+URL';
                          }}
                        />
                        <Button
                          type="button"
                          onClick={() => handleRemoveImage(index)}
                          variant="destructive"
                          size="icon"
                          className="absolute top-1 right-1 h-6 w-6 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
                          title="Remove image"
                        >
                          ×
                        </Button>
                        <Badge className="absolute bottom-1 left-1" variant="secondary">
                          #{index + 1}
                        </Badge>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8 bg-muted rounded-lg border-2 border-dashed border-border">
                    <div className="text-4xl mb-2"></div>
                    <p className="text-sm font-medium">No images added yet</p>
                    <p className="text-muted-foreground text-xs mt-1">Add image URLs above to see previews</p>
                  </div>
                )}
              </div>
            </div>

            <DialogFooter className="mt-6">
              <Button
                type="button"
                onClick={() => setIsModalOpen(false)}
                variant="outline"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={loading}
              >
                {loading ? 'Saving...' : editingProduct ? 'Update Product' : 'Add Product'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default ProductManagementView;
