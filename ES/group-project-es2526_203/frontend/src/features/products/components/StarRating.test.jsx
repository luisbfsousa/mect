import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StarRating from './StarRating';

describe('StarRating Component', () => {
  test('renders star rating with correct rating and review count', () => {
    render(<StarRating rating={4.5} reviews={120} />);

    expect(screen.getByText(/4.5/)).toBeInTheDocument();
    expect(screen.getByText(/120 reviews/)).toBeInTheDocument();
  });

  test('displays zero rating correctly', () => {
    render(<StarRating rating={0} reviews={0} />);

    expect(screen.getByText(/0.0/)).toBeInTheDocument();
    expect(screen.getByText(/0 reviews/)).toBeInTheDocument();
  });

  test('displays perfect rating', () => {
    render(<StarRating rating={5} reviews={50} />);

    expect(screen.getByText(/5.0/)).toBeInTheDocument();
    expect(screen.getByText(/50 reviews/)).toBeInTheDocument();
  });

  test('formats decimal ratings correctly', () => {
    render(<StarRating rating={3.7} reviews={89} />);

    expect(screen.getByText(/3.7/)).toBeInTheDocument();
    expect(screen.getByText(/89 reviews/)).toBeInTheDocument();
  });

  test('renders 5 star elements', () => {
    const { container } = render(<StarRating rating={3} reviews={10} />);

    const stars = container.querySelectorAll('svg');
    expect(stars).toHaveLength(5);
  });
});
