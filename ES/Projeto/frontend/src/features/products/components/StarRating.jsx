import React from 'react';
import { Star } from 'lucide-react';

const StarRating = ({ rating, reviews }) => {
  return (
    <div className="flex items-center gap-4">
      <div className="flex items-center text-yellow-500">
        {[...Array(5)].map((_, i) => (
          <Star
            key={i}
            size={20}
            fill={i < Math.floor(rating) ? 'currentColor' : 'none'}
          />
        ))}
      </div>
      <span className="text-muted-foreground">
        {Number(rating).toFixed(1)} ({reviews} reviews)
      </span>
    </div>
  );
};

export default StarRating;