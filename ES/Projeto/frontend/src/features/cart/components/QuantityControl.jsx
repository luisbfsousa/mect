import React from 'react';
import { Button } from '../../../components/ui/button';

const QuantityControl = ({ quantity, onDecrease, onIncrease, min = 1 }) => (
  <div className="flex items-center gap-2">
    <Button
      onClick={onDecrease}
      disabled={quantity <= min}
      variant="outline"
      size="sm"
      className="w-8 h-8 p-0"
    >
      -
    </Button>
    <span className="w-12 text-center font-semibold">{quantity}</span>
    <Button
      onClick={onIncrease}
      variant="outline"
      size="sm"
      className="w-8 h-8 p-0"
    >
      +
    </Button>
  </div>
);

export default QuantityControl;