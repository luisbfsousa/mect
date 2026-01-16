import React from 'react';
import { Card } from '../../../components/ui/card';

const OrderSummary = ({ cart, cartTotal }) => {
  return (
    <Card className="p-4 bg-primary/5">
      <h3 className="font-semibold mb-2">Order Summary</h3>
      <div className="space-y-1 text-sm">
        {cart.map(item => (
          <div key={item.cart_id} className="flex justify-between">
            <span>{item.product_name} x{item.quantity}</span>
            <span>{(item.unit_price * item.quantity).toFixed(2)} €</span>
          </div>
        ))}
        <div className="border-t pt-2 mt-2 flex justify-between font-bold">
          <span>Total:</span>
          <span className="text-primary">{cartTotal.toFixed(2)} €</span>
        </div>
      </div>
    </Card>
  );
};

export default OrderSummary;