import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import EmptyState from './EmptyState';
import { ShoppingCart } from 'lucide-react';

describe('EmptyState Component', () => {
  test('renders title and message', () => {
    render(
      <EmptyState
        title="Your cart is empty"
        message="Add items to get started"
      />
    );

    expect(screen.getByText(/your cart is empty/i)).toBeInTheDocument();
    expect(screen.getByText(/add items to get started/i)).toBeInTheDocument();
  });

  test('renders without icon when not provided', () => {
    render(
      <EmptyState
        title="No items found"
      />
    );

    expect(screen.getByText(/no items found/i)).toBeInTheDocument();
  });

  test('renders action button when provided', () => {
    const handleAction = jest.fn();

    render(
      <EmptyState
        title="Empty"
        actionLabel="Start Shopping"
        onAction={handleAction}
      />
    );

    expect(screen.getByRole('button', { name: /start shopping/i })).toBeInTheDocument();
  });

  test('handles action button click', () => {
    const handleAction = jest.fn();

    render(
      <EmptyState
        title="Empty"
        actionLabel="Browse Products"
        onAction={handleAction}
      />
    );

    userEvent.click(screen.getByRole('button'));
    expect(handleAction).toHaveBeenCalledTimes(1);
  });

  test('does not render button when action props are missing', () => {
    render(
      <EmptyState
        title="Empty"
        message="No items"
      />
    );

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
