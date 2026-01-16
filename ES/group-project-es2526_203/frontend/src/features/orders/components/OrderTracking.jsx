import React from 'react';
import { Package, Truck, CheckCircle, Clock, ExternalLink, MapPin, Calendar } from 'lucide-react';
import { Card } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Button } from '../../../components/ui/button';

const OrderTracking = ({ order }) => {
  const getStatusInfo = (status) => {
    const statusMap = {
      'pending': {
        label: 'Order Placed',
        variant: 'warning',
        icon: Clock,
        progress: 25,
        description: 'Your order has been received and is being prepared.'
      },
      'processing': {
        label: 'Processing',
        variant: 'default',
        icon: Package,
        progress: 50,
        description: 'Your order is being processed and packed.'
      },
      'shipped': {
        label: 'Shipped',
        variant: 'secondary',
        icon: Truck,
        progress: 75,
        description: 'Your order has been shipped and is on the way.'
      },
      'delivered': {
        label: 'Delivered',
        variant: 'default',
        icon: CheckCircle,
        progress: 100,
        description: 'Your order has been delivered successfully.'
      }
    };
    return statusMap[status] || statusMap['pending'];
  };

  const statusInfo = getStatusInfo(order.order_status);
  const StatusIcon = statusInfo.icon;
  const items = Array.isArray(order.items) ? order.items : [];

  const stages = [
    { key: 'pending', label: 'Order Placed', icon: Clock },
    { key: 'processing', label: 'Processing', icon: Package },
    { key: 'shipped', label: 'Shipped', icon: Truck },
    { key: 'delivered', label: 'Delivered', icon: CheckCircle }
  ];

  const currentStageIndex = stages.findIndex(s => s.key === order.order_status);

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  return (
    <div className="space-y-6">
      {/* Order Header */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-3xl font-bold">Order #{order.order_id}</h2>
            <p className="text-muted-foreground mt-1 flex items-center gap-2">
              <Calendar size={16} />
              Placed on {formatDate(order.created_at)}
            </p>
          </div>
          <Badge variant={statusInfo.variant} className="px-6 py-3 text-lg gap-2">
            <StatusIcon size={24} />
            {statusInfo.label}
          </Badge>
        </div>

        <p className="text-muted-foreground mt-2">{statusInfo.description}</p>
      </Card>

      {/* Progress Tracker */}
      <Card className="p-6">
        <h3 className="text-xl font-bold mb-6">Delivery Progress</h3>
        
        <div className="relative mb-12">
          {/* Background line */}
          <div className="absolute top-6 left-0 right-0 h-2 bg-gray-200 dark:bg-gray-700 rounded-full" />
          
          {/* Progress line */}
          <div 
            className="absolute top-6 left-0 h-2 bg-blue-600 rounded-full transition-all duration-500"
            style={{ width: `${statusInfo.progress}%` }}
          />

          {/* Stages */}
          <div className="relative flex justify-between">
            {stages.map((stage, index) => {
              const isCompleted = index <= currentStageIndex;
              const isCurrent = index === currentStageIndex;
              const StageIcon = stage.icon;
              
              return (
                <div key={stage.key} className="flex flex-col items-center" style={{ width: '25%' }}>
                  <div 
                    className={`w-12 h-12 rounded-full flex items-center justify-center border-4 transition-all z-10 ${
                      isCompleted 
                        ? 'bg-blue-600 border-blue-600' 
                        : 'bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600'
                    } ${isCurrent ? 'ring-4 ring-blue-200 dark:ring-blue-800 scale-110' : ''}`}
                  >
                    <StageIcon 
                      className={isCompleted ? 'text-white' : 'text-gray-400 dark:text-gray-300'} 
                      size={24} 
                    />
                  </div>
                  <span className={`text-sm mt-3 font-semibold text-center ${
                    isCompleted ? 'text-gray-900 dark:text-gray-100' : 'text-gray-400 dark:text-gray-400'
                  }`}>
                    {stage.label}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </Card>

      {/* Shipping Details */}
      <Card className="p-6">
        <h3 className="text-xl font-bold mb-4">Shipping Information</h3>
        
        <div className="grid md:grid-cols-2 gap-6">
          <div className="space-y-3">
            <div className="flex items-start gap-3">
              <Truck className="text-blue-600 mt-1" size={20} />
              <div>
                <p className="font-semibold text-gray-700 dark:text-gray-100">Carrier</p>
                <p className="text-gray-600 dark:text-gray-300">{order.shipping_provider || 'Standard Shipping'}</p>
              </div>
            </div>
            
            {order.tracking_number && (
            <div className="flex items-start gap-3">
              <Package className="text-blue-600 mt-1" size={20} />
              <div>
                <p className="font-semibold text-gray-700 dark:text-gray-100">Tracking Number</p>
                <p className="text-blue-600 font-mono text-sm dark:text-blue-200">{order.tracking_number}</p>
              </div>
            </div>
            )}
            
            {order.estimated_delivery_date && (
              <div className="flex items-start gap-3">
                <Calendar className="text-blue-600 mt-1" size={20} />
                <div>
                  <p className="font-semibold text-gray-700 dark:text-gray-100">Estimated Delivery</p>
                  <p className="text-gray-600 dark:text-gray-300">{formatDate(order.estimated_delivery_date)}</p>
                </div>
              </div>
            )}
          </div>

          <div>
            <div className="flex items-start gap-3">
              <MapPin className="text-blue-600 mt-1" size={20} />
              <div>
                <p className="font-semibold text-gray-700 dark:text-gray-100 mb-2">Shipping Address</p>
                {order.shipping_address && (
                  <div className="text-gray-600 dark:text-gray-300 text-sm space-y-1">
                    <p>{order.shipping_address.fullName || order.shipping_address.name}</p>
                    <p>{order.shipping_address.address}</p>
                    <p>
                      {order.shipping_address.city}, {order.shipping_address.state} {order.shipping_address.zipCode}
                    </p>
                    {order.shipping_address.country && <p>{order.shipping_address.country}</p>}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* External Tracking Link */}
        {order.tracking_number && (
          <div className="mt-6 pt-6 border-t">
            <Button
              onClick={() => window.open(`https://www.google.com/search?q=track+package+${order.tracking_number}`, '_blank')}
              className="w-full md:w-auto gap-2"
            >
              <ExternalLink size={20} />
              Track with Shipping Provider
            </Button>
          </div>
        )}
      </Card>

      {/* Order Items */}
      <Card className="p-6">
        <h3 className="text-xl font-bold mb-4">Order Items</h3>
        
        <div className="space-y-4">
          {items.map((item, index) => (
            <div key={item.order_item_id || index} className="flex items-center gap-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
              {item.images && item.images[0] && (
                <img 
                  src={item.images[0]} 
                  alt={item.product_name}
                  className="w-20 h-20 object-cover rounded-lg"
                />
              )}
              <div className="flex-1">
                <p className="font-semibold text-gray-800 dark:text-gray-100 text-lg">
                  {item.product_name || 'Product'}
                </p>
                <p className="text-gray-600 dark:text-gray-300">
                  Quantity: {item.quantity || 0} × {parseFloat(item.unit_price || 0).toFixed(2)} €
                </p>
              </div>
              <p className="text-xl font-bold text-gray-800 dark:text-gray-100">
                {parseFloat(item.subtotal || 0).toFixed(2)} €
              </p>
            </div>
          ))}
        </div>

        {/* Order Summary */}
        <div className="mt-6 pt-6 border-t space-y-2">
          <div className="flex justify-between text-muted-foreground">
            <span>Subtotal</span>
            <span>{parseFloat(order.total_amount || 0).toFixed(2)} €</span>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <span>Shipping</span>
            <span>{parseFloat(order.shipping_cost || 0).toFixed(2)} €</span>
          </div>
          <div className="flex justify-between text-xl font-bold pt-2 border-t">
            <span>Total</span>
            <span>{parseFloat(order.total_amount || 0).toFixed(2)} €</span>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default OrderTracking;