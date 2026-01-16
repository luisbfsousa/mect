import React from 'react';
import { Badge } from '../../../components/ui/badge';

const getStockStatus = (stock) => {
  if (stock === 0) {
    return {
      text: 'Out of stock',
      variant: 'danger'
    };
  }
  if (stock <= 10) {
    return {
      text: 'Low stock',
      variant: 'warning'
    };
  }
  return {
    text: 'In stock',
    variant: 'success'
  };
};

const ProductPriceCard = ({ price, stock }) => {
  const stockStatus = getStockStatus(stock);

  return (
    <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
      <div className="flex justify-between items-center mb-2">
        <span className="text-gray-600 dark:text-gray-300">Price:</span>
        <span className="text-3xl font-bold text-blue-600 dark:text-blue-400">${price}</span>
      </div>
      <div className="flex justify-between items-center">
        <span className="text-gray-600 dark:text-gray-300">Availability:</span>
        <Badge variant={stockStatus.variant}>
          {stockStatus.text}
        </Badge>
      </div>
    </div>
  );
};

export default ProductPriceCard;