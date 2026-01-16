import React from 'react';
import { Package, Truck, CheckCircle, Clock, ExternalLink } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Separator } from '../../../components/ui/separator';

const OrderCard = ({ order, onTrack }) => {
  const getStatusIcon = (status) => {
    if (!status) return <Package className="text-gray-600" size={20} />;
    
    switch (status.toLowerCase()) {
      case 'pending':
        return <Clock className="text-yellow-600" size={20} />;
      case 'processing':
        return <Package className="text-blue-600" size={20} />;
      case 'shipped':
        return <Truck className="text-purple-600" size={20} />;
      case 'delivered':
        return <CheckCircle className="text-green-600" size={20} />;
      default:
        return <Package className="text-gray-600" size={20} />;
    }
  };

  const getStatusVariant = (status) => {
    if (!status) return 'secondary';

    const variants = {
      'pending': 'warning',
      'processing': 'default',
      'shipped': 'secondary',
      'delivered': 'success',
    };
    return variants[status.toLowerCase()] || 'secondary';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatStatus = (status) => {
    if (!status) return 'Unknown';
    return status.charAt(0).toUpperCase() + status.slice(1);
  };

  const getEstimatedDelivery = () => {
    if (!order.estimated_delivery_date) return null;
    const deliveryDate = new Date(order.estimated_delivery_date);
    const today = new Date();
    const diffTime = deliveryDate - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays < 0) return 'Expected ' + formatDate(order.estimated_delivery_date);
    if (diffDays === 0) return 'Arriving today';
    if (diffDays === 1) return 'Arriving tomorrow';
    return `Arriving in ${diffDays} days`;
  };

  return (
    <Card className="p-6 hover:shadow-lg transition-shadow">
      {/* Order Header */}
      <div className="flex justify-between items-start mb-4">
        <div>
          <div className="flex items-center space-x-2 mb-2">
            {getStatusIcon(order.order_status)}
            <h3 className="text-lg font-bold">Order #{order.order_id}</h3>
          </div>
          <p className="text-sm text-muted-foreground">Placed on {formatDate(order.created_at)}</p>
          {order.tracking_number && (
            <p className="text-sm text-muted-foreground">
              Tracking: <span className="font-mono font-medium">{order.tracking_number}</span>
            </p>
          )}
        </div>
        <div className="text-right">
          <Badge variant={getStatusVariant(order.order_status)}>
            {formatStatus(order.order_status)}
          </Badge>
          <p className="text-lg font-bold mt-2">
            {order.total_amount ? parseFloat(order.total_amount).toFixed(2) : '0.00'} €
          </p>
        </div>
      </div>

      {/* Estimated Delivery */}
      {order.order_status !== 'delivered' && order.estimated_delivery_date && (
        <Card className="bg-blue-50 dark:bg-blue-950 border-blue-200 dark:border-blue-800 p-3 mb-4">
          <p className="text-sm font-medium text-blue-800 dark:text-blue-200">
            📦 {getEstimatedDelivery()}
          </p>
        </Card>
      )}

      {/* Order Items Preview */}
      <div className="mb-4">
        <div className="space-y-2">
          {order.items && order.items.slice(0, 2).map((item, index) => (
            <div key={index} className="flex items-center">
              {item.images && item.images[0] && (
                <img 
                  src={item.images[0]} 
                  alt={item.product_name || 'Product'}
                  className="w-12 h-12 object-cover rounded mr-3"
                />
              )}
              <div className="flex-1">
                <p className="text-sm font-medium">{item.product_name || 'Unknown Product'}</p>
                <p className="text-xs text-muted-foreground">Qty: {item.quantity || 0}</p>
              </div>
              <p className="text-sm font-semibold">
                {item.subtotal ? parseFloat(item.subtotal).toFixed(2) : '0.00'} €
              </p>
            </div>
          ))}
          {order.items && order.items.length > 2 && (
            <p className="text-sm text-muted-foreground mt-2">
              + {order.items.length - 2} more item{order.items.length - 2 !== 1 ? 's' : ''}
            </p>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <Separator className="my-4" />
      <div className="flex space-x-3">
        <Button
          onClick={onTrack}
          className="flex-1 flex items-center justify-center"
        >
          <ExternalLink size={16} className="mr-2" />
          Track Order
        </Button>

        {order.order_status === 'delivered' && (
          <Button
            onClick={() => {/* Add review functionality */}}
            variant="outline"
            className="flex-1"
          >
            Leave Review
          </Button>
        )}
      </div>
    </Card>
  );
};

export default OrderCard;