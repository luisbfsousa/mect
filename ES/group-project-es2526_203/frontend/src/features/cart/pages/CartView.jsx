import React from 'react';
import { ShoppingCart } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Separator } from '../../../components/ui/separator';
import EmptyState from '../components/EmptyState';
import CartItem from '../components/CartItem';

const CartView = ({
  cart,
  cartTotal,
  updateQuantity,
  removeFromCart,
  navigate
}) => (
  <Card>
    <CardHeader>
      <CardTitle>Shopping Cart</CardTitle>
    </CardHeader>
    <CardContent>
    
    {cart.length === 0 ? (
      <EmptyState
        icon={ShoppingCart}
        title="Your cart is empty"
        actionLabel="Continue Shopping"
        onAction={() => navigate('/products')}
      />
    ) : (
      <div className="space-y-4">
        {cart.map(item => (
          <CartItem
            key={item.cart_id}
            item={item}
            onUpdateQuantity={updateQuantity}
            onRemove={removeFromCart}
          />
        ))}

        <div className="pt-4">
          <Separator className="mb-4" />
          <div className="flex justify-between items-center text-xl font-bold mb-6">
            <span>Total:</span>
            <span className="text-primary">{cartTotal.toFixed(2)} €</span>
          </div>

          <div className="flex gap-4">
            <Button
              onClick={() => navigate('/products')}
              variant="outline"
              className="flex-1"
              size="lg"
            >
              Continue Shopping
            </Button>
            <Button
              onClick={() => navigate('/checkout')}
              className="flex-1"
              size="lg"
            >
              Proceed to Checkout
            </Button>
          </div>
        </div>
      </div>
    )}
    </CardContent>
  </Card>
);

export default CartView;