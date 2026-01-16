import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Button } from './button';

describe('Button Component', () => {
  test('renders button with text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button', { name: /click me/i })).toBeInTheDocument();
  });

  test('handles click events', () => {
    const handleClick = jest.fn();

    render(<Button onClick={handleClick}>Click me</Button>);

    userEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  test('renders with different variants', () => {
    const { rerender } = render(<Button variant="destructive">Delete</Button>);
    expect(screen.getByRole('button')).toBeInTheDocument();

    rerender(<Button variant="outline">Cancel</Button>);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  test('disables button when disabled prop is true', () => {
    render(<Button disabled>Disabled Button</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  test('applies custom className', () => {
    render(<Button className="custom-class">Styled Button</Button>);
    expect(screen.getByRole('button')).toHaveClass('custom-class');
  });
});
