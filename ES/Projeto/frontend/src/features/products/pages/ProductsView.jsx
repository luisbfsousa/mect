import React, { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { Filter } from 'lucide-react';
import ProductCard from '../../../components/ProductCard';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import SearchBar from '../components/SearchBar';
import FilterPanel from '../components/FilterPanel';
import EmptyProductsState from '../components/EmptyProductsState';

const ProductsView = ({
  searchQuery,
  setSearchQuery,
  showFilters,
  setShowFilters,
  CATEGORIES,
  selectedCategory,
  setSelectedCategory,
  priceRange,
  setPriceRange,
  filteredProducts,
  setSelectedProduct,
  navigate,
  addToCart,
  loading,
  error
}) => {
  const location = useLocation();

  useEffect(() => {
    // When arriving with a ?category= query param, apply it to the selectedCategory filter
    const params = new URLSearchParams(location.search);
    const categoryParam = params.get('category');
    if (categoryParam && typeof setSelectedCategory === 'function') {
      setSelectedCategory(decodeURIComponent(categoryParam));
    }
    // only on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const handleViewDetails = (product) => {
    setSelectedProduct(product);
    navigate('/productDetail');
  };

  const handleAddToCart = (product) => {
    addToCart(product);
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setSelectedCategory('All');
    setPriceRange([0, 300]);
  };

  return (
    <div>
      <div className="mb-6">
        <div className="flex flex-col md:flex-row gap-4 items-start md:items-center justify-between">
          <div className="flex-1 w-full">
            <SearchBar value={searchQuery} onChange={setSearchQuery} />
          </div>
          
          <Button
            onClick={() => setShowFilters(!showFilters)}
            variant="outline"
            className="flex items-center gap-2"
          >
            <Filter size={20} />
            <span>Filters</span>
          </Button>
        </div>

        {showFilters && (
          <FilterPanel
            categories={CATEGORIES}
            selectedCategory={selectedCategory}
            onCategoryChange={setSelectedCategory}
            priceRange={priceRange}
            onPriceChange={setPriceRange}
          />
        )}
      </div>

      {loading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
          <p className="mt-4 text-muted-foreground">Loading products...</p>
        </div>
      ) : error ? (
        <Card className="border-destructive">
          <div className="bg-destructive/10 text-destructive px-4 py-3">
            {error}
          </div>
        </Card>
      ) : (
        <>
          <div className="mb-4 text-muted-foreground">
            Showing {filteredProducts.length} products
          </div>

          {filteredProducts.length === 0 ? (
            <EmptyProductsState onClearFilters={handleClearFilters} />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {filteredProducts.map(product => (
                <ProductCard
                  key={product.id}
                  product={product}
                  onViewDetails={handleViewDetails}
                  onAddToCart={handleAddToCart}
                />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default ProductsView;