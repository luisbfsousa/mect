import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import ProductCard from './ProductCard';

const mockProduct = {
  name: 'Test Product',
  description: 'A great product for testing',
  price: '99.99',
  stock: 15,
  rating: 4.5,
  reviews: 100,
  image: 'https://example.com/image.jpg'
};

describe('ProductCard Component', () => {
  test('renders product information correctly', () => {
    render(
      <ProductCard
        product={mockProduct}
        onViewDetails={() => {}}
        onAddToCart={() => {}}
      />
    );

    expect(screen.getByText(/test product/i)).toBeInTheDocument();
    expect(screen.getByText(/a great product for testing/i)).toBeInTheDocument();
    expect(screen.getByText(/99.99 €/)).toBeInTheDocument();
  });

  test('displays stock status for in-stock product', () => {
    render(
      <ProductCard
        product={mockProduct}
        onViewDetails={() => {}}
        onAddToCart={() => {}}
      />
    );

    expect(screen.getByText(/15 in stock/i)).toBeInTheDocument();
  });

  test('displays out of stock status and disables add to cart', () => {
    const outOfStockProduct = { ...mockProduct, stock: 0 };

    render(
      <ProductCard
        product={outOfStockProduct}
        onViewDetails={() => {}}
        onAddToCart={() => {}}
      />
    );

    expect(screen.getByText(/out of stock/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add to cart/i })).toBeDisabled();
  });

  test('handles view details button click', () => {
    const handleViewDetails = jest.fn();

    render(
      <ProductCard
        product={mockProduct}
        onViewDetails={handleViewDetails}
        onAddToCart={() => {}}
      />
    );

    userEvent.click(screen.getByRole('button', { name: /view details/i }));
    expect(handleViewDetails).toHaveBeenCalledWith(mockProduct);
  });

  test('handles add to cart button click', () => {
    const handleAddToCart = jest.fn();

    render(
      <ProductCard
        product={mockProduct}
        onViewDetails={() => {}}
        onAddToCart={handleAddToCart}
      />
    );

    userEvent.click(screen.getByRole('button', { name: /add to cart/i }));
    expect(handleAddToCart).toHaveBeenCalledWith(mockProduct);
  });

  test('shows adding state when isAdding is true', () => {
    render(
      <ProductCard
        product={mockProduct}
        onViewDetails={() => {}}
        onAddToCart={() => {}}
        isAdding={true}
      />
    );

    expect(screen.getByText(/adding.../i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /adding.../i })).toBeDisabled();
  });

  test('displays low stock warning for products with 10 or fewer items', () => {
    const lowStockProduct = { ...mockProduct, stock: 5 };

    render(
      <ProductCard
        product={lowStockProduct}
        onViewDetails={() => {}}
        onAddToCart={() => {}}
      />
    );

    expect(screen.getByText(/only 5 left in stock/i)).toBeInTheDocument();
  });
});
