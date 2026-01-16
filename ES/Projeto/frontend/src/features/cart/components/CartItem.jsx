import React from 'react';
import { Minus, Plus, X, Package } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';

const CartItem = ({ item, onUpdateQuantity, onRemove }) => {
  // Converter unit_price para número se vier como string
  const unitPrice = typeof item.unit_price === 'string'
    ? parseFloat(item.unit_price)
    : item.unit_price;

  const subtotal = unitPrice * item.quantity;

  // Parse da imagem se vier como JSON string
  let imageUrl = null; // No default - will show icon if no image
  if (item.images) {
    try {
      const images = typeof item.images === 'string'
        ? JSON.parse(item.images)
        : item.images;
      imageUrl = Array.isArray(images) ? images[0] : images;
    } catch (e) {
      console.error('Failed to parse images:', e);
    }
  }

  return (
    <Card className="flex gap-4 p-4">
      {/* Product Image */}
      <div className="bg-muted rounded-lg w-24 h-24 flex items-center justify-center flex-shrink-0">
        {typeof imageUrl === 'string' && imageUrl.startsWith('http') ? (
          <img src={imageUrl} alt={item.product_name} className="w-full h-full object-cover rounded-lg" />
        ) : (
          <Package className="h-10 w-10 text-muted-foreground" />
        )}
      </div>

      {/* Product Info */}
      <div className="flex-1">
        <h3 className="font-semibold text-lg mb-1">{item.product_name || 'Product'}</h3>
        <p className="text-muted-foreground mb-2">{unitPrice.toFixed(2)} € each</p>

        {/* Quantity Controls */}
        <div className="flex items-center gap-2">
          <Button
            onClick={() => onUpdateQuantity(item.cart_id, item.quantity - 1)}
            variant="outline"
            size="sm"
            disabled={item.quantity <= 1}
          >
            <Minus size={16} />
          </Button>

          <span className="w-12 text-center font-medium">{item.quantity}</span>

          <Button
            onClick={() => onUpdateQuantity(item.cart_id, item.quantity + 1)}
            variant="outline"
            size="sm"
          >
            <Plus size={16} />
          </Button>
        </div>
      </div>

      {/* Price and Remove */}
      <div className="flex flex-col items-end justify-between">
        <Button
          onClick={() => onRemove(item.cart_id)}
          variant="ghost"
          size="sm"
        >
          <X size={16} />
        </Button>

        <div className="text-right">
          <p className="text-sm text-muted-foreground">Subtotal</p>
          <p className="text-xl font-bold text-primary">{subtotal.toFixed(2)} €</p>
        </div>
      </div>
    </Card>
  );
};

export default CartItem;