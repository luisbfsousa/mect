import { useState, useEffect } from 'react';
import { productsAPI } from '../services/api';

export const useProducts = () => {
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [priceRange, setPriceRange] = useState([0, 1000]);
  const [showFilters, setShowFilters] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch products from API on mount
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await productsAPI.getAll();
        
        // Map backend product structure to frontend format
        const mappedProducts = data.map(product => ({
          id: product.product_id,
          product_id: product.product_id, // Keep for backwards compatibility
          name: product.name,
          description: product.description,
          price: parseFloat(product.price),
          category: product.category_name || 'Uncategorized',
          categoryId: product.category_id,
          category_id: product.category_id, // Keep for backwards compatibility
          image: product.images && product.images.length > 0 ? product.images[0] : null,
          images: product.images || [],
          rating: product.average_rating || 0, // Use actual average rating from reviews
          reviews: product.review_count || 0, // Use actual review count
          stock: product.stock_quantity,
          stock_quantity: product.stock_quantity, // Keep for backwards compatibility
          sku: product.sku,
          specifications: product.specifications || {}
        }));
        
        setProducts(mappedProducts);
      } catch (err) {
        console.error('Failed to fetch products:', err);
        setError('Failed to load products. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          p.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'All' || p.category === selectedCategory;
    const matchesPrice = p.price >= priceRange[0] && p.price <= priceRange[1];
    return matchesSearch && matchesCategory && matchesPrice;
  });

  // CRUD operations for admin
  const addProduct = (newProduct) => {
    setProducts(prev => [...prev, newProduct]);
  };

  const updateProduct = (id, updatedProduct) => {
    setProducts(prev => prev.map(p => p.id === id ? updatedProduct : p));
  };

  const deleteProduct = (id) => {
    setProducts(prev => prev.filter(p => p.id !== id));
  };

  // Refresh products from API
  const refreshProducts = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await productsAPI.getAll();
      const mappedProducts = data.map(product => ({
        id: product.product_id,
        product_id: product.product_id,
        name: product.name,
        description: product.description,
        price: parseFloat(product.price),
        category: product.category_name || 'Uncategorized',
        categoryId: product.category_id,
        category_id: product.category_id,
        image: product.images && product.images.length > 0 ? product.images[0] : null,
        images: product.images || [],
        rating: product.average_rating || 0, // Use actual average rating from reviews
        reviews: product.review_count || 0, // Use actual review count
        stock: product.stock_quantity,
        stock_quantity: product.stock_quantity,
        sku: product.sku,
        specifications: product.specifications || {}
      }));
      setProducts(mappedProducts);
    } catch (err) {
      console.error('Failed to refresh products:', err);
      setError('Failed to refresh products.');
    } finally {
      setLoading(false);
    }
  };

  return {
    products,
    setProducts,
    selectedProduct,
    setSelectedProduct,
    searchQuery,
    setSearchQuery,
    selectedCategory,
    setSelectedCategory,
    priceRange,
    setPriceRange,
    showFilters,
    setShowFilters,
    filteredProducts,
    loading,
    error,
    refreshProducts,
    // Admin CRUD operations
    addProduct,
    updateProduct,
    deleteProduct
  };
};