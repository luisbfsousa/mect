import React from 'react';
import { CheckCircle } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';

const OrderConfirmationView = ({ navigate }) => (
  <Card className="max-w-2xl mx-auto p-8 text-center">
    <CheckCircle className="mx-auto h-16 w-16 text-green-600 mb-4" />
    <h2 className="text-3xl font-bold mb-4">Order Confirmed!</h2>
    <p className="text-muted-foreground mb-6">
      Thank you for your purchase. Your order has been placed successfully.
    </p>
    <div className="flex gap-4 justify-center">
      <Button
        onClick={() => navigate('/orders')}
        size="lg"
      >
        View Orders
      </Button>
      <Button
        onClick={() => navigate('/')}
        variant="outline"
        size="lg"
      >
        Continue Shopping
      </Button>
    </div>
  </Card>
);

export default OrderConfirmationView;