import React from 'react';
import { X, Package } from 'lucide-react';
import { Card, CardContent } from '../../../components/ui/card';
import { Separator } from '../../../components/ui/separator';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';

const OrderItemsModal = ({ order, onClose }) => {
  if (!order) return null;

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package size={24} />
            Order #{order.order_id}
          </DialogTitle>
          <DialogDescription>
            {order.items?.length || 0} item(s) - Total: {parseFloat(order.total_amount || 0).toFixed(2)} €
          </DialogDescription>
        </DialogHeader>

        {/* Customer Info */}
        <div className="bg-muted/50 px-6 py-3 rounded-lg">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-muted-foreground">Customer:</span>
              <span className="ml-2 font-medium">
                {order.username && !/^[0-9a-fA-F-]{36}$/.test(order.username)
                  ? order.username
                  : order.first_name && order.last_name
                    ? `${order.first_name} ${order.last_name}`
                    : order.user_id}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground">Date:</span>
              <span className="ml-2 font-medium">
                {new Date(order.created_at).toLocaleDateString()}
              </span>
            </div>
          </div>
        </div>

        <Separator />

        {/* Items List */}
        <div className="overflow-y-auto flex-1 px-6">
          {!order.items || order.items.length === 0 ? (
            <div className="text-center py-8">
              <Package size={48} className="mx-auto mb-3 text-muted-foreground" />
              <p className="text-muted-foreground">No items found in this order</p>
            </div>
          ) : (
            <div className="space-y-4">
              {order.items.map((item, index) => (
                    <Card
                      key={index}
                      className="hover:bg-muted/50 transition-colors"
                    >
                      <CardContent className="flex items-center gap-4 p-4">
                        {/* Product Image */}
                        <div className="flex-shrink-0">
                          <img
                            src={(item.images && item.images[0]) || item.imageUrl || item.image_url || item.product_image || 'https://via.placeholder.com/80/e5e7eb/9ca3af?text=No+Image'}
                            alt={item.product_name || 'Product'}
                            className="w-20 h-20 object-cover rounded-lg border"
                            onError={(e) => {
                              e.target.onerror = null;
                              e.target.src = 'https://via.placeholder.com/80/e5e7eb/9ca3af?text=No+Image';
                            }}
                          />
                        </div>

                        {/* Product Info */}
                        <div className="flex-1 min-w-0">
                          <h4 className="font-semibold truncate">
                            {item.product_name || 'Unknown Product'}
                          </h4>
                          <div className="mt-1 flex items-center gap-4 text-sm text-muted-foreground">
                            <span>Quantity: <span className="font-medium text-foreground">{item.quantity}</span></span>
                            <span>Unit Price: <span className="font-medium text-foreground">{parseFloat(item.unit_price || 0).toFixed(2)} €</span></span>
                          </div>
                          {item.sku && (
                            <div className="mt-1 text-xs text-muted-foreground">
                              SKU: {item.sku}
                            </div>
                          )}
                        </div>

                        {/* Subtotal */}
                        <div className="flex-shrink-0 text-right">
                          <div className="text-sm text-muted-foreground">Subtotal</div>
                          <div className="text-lg font-bold text-green-600 dark:text-green-400">
                            {(parseFloat(item.unit_price || 0) * parseInt(item.quantity || 0)).toFixed(2)} €
                          </div>
                        </div>
                      </CardContent>
                    </Card>
              ))}
            </div>
          )}
        </div>

        <Separator />

        {/* Footer */}
        <div className="px-6 py-4 flex justify-between items-center">
          <div className="text-sm text-muted-foreground">
            {order.items?.length || 0} item(s)
          </div>
          <div className="flex items-center gap-2">
            <span className="font-medium">Total:</span>
            <span className="text-2xl font-bold text-green-600 dark:text-green-400">
              {parseFloat(order.total_amount || 0).toFixed(2)} €
            </span>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default OrderItemsModal;