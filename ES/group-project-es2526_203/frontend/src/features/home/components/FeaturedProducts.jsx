import React from 'react';
import ProductCard from '../../../components/ProductCard';

const FeaturedProducts = ({ products, onViewDetails, onAddToCart }) => {
  return (
    <section>
      <h3 className="text-2xl font-bold mb-6 dark:text-gray-100">Featured Products</h3>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {products.slice(0, 3).map(product => (
          <ProductCard
            key={product.id}
            product={product}
            onViewDetails={onViewDetails}
            onAddToCart={onAddToCart}
          />
        ))}
      </div>
    </section>
  );
};

export default FeaturedProducts;