import React from 'react';
import { Package } from 'lucide-react';
import StarRating from '../features/products/components/StarRating';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Card, CardContent, CardFooter } from './ui/card';

const getStockStatus = (stock) => {
  if (stock === 0) {
    return {
      text: 'Out of stock',
      variant: 'danger'
    };
  }
  if (stock <= 10) {
    return {
      text: `Only ${stock} left in stock`,
      variant: 'warning'
    };
  }
  return {
    text: `${stock} in stock`,
    variant: 'success'
  };
};

const ProductCard = ({ product, onViewDetails, onAddToCart, isAdding }) => {
  const stockStatus = getStockStatus(product.stock);

  return (
    <Card className="overflow-hidden hover:shadow-lg transition-shadow">
      <div className="aspect-video bg-muted flex items-center justify-center overflow-hidden">
        {product.image ? (
          <img
            src={product.image}
            alt={product.name}
            className="w-full h-full object-cover"
            onError={(e) => {
              e.target.onerror = null;
              e.target.src = 'https://via.placeholder.com/400x300?text=No+Image';
            }}
          />
        ) : (
          <Package className="h-16 w-16 text-muted-foreground" />
        )}
      </div>

      <CardContent className="p-4">
        <h3 className="font-semibold text-lg mb-2 line-clamp-1">{product.name}</h3>

        <StarRating rating={product.rating} reviews={product.reviews} />

        <p className="text-muted-foreground text-sm mt-2 mb-3 line-clamp-2">{product.description}</p>

        <div className="flex justify-between items-center">
          <span className="text-2xl font-bold">{product.price} €</span>
          <Badge variant={stockStatus.variant}>
            {stockStatus.text}
          </Badge>
        </div>
      </CardContent>

      <CardFooter className="p-4 pt-0 flex gap-2">
        <Button
          onClick={() => onViewDetails(product)}
          variant="outline"
          className="flex-1"
        >
          View Details
        </Button>
        <Button
          onClick={() => onAddToCart(product)}
          disabled={product.stock === 0 || isAdding}
          className="flex-1"
        >
          {isAdding ? 'Adding...' : 'Add to Cart'}
        </Button>
      </CardFooter>
    </Card>
  );
};

export default ProductCard;