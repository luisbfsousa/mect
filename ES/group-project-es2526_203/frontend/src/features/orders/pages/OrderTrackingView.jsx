import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { Package, Truck, CheckCircle, Clock, ExternalLink, ArrowLeft } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { API_BASE_URL } from '../../../services/api';

const OrderTrackingView = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (keycloak.authenticated && orderId) {
      fetchOrder();
    }
  }, [keycloak.authenticated, orderId]);

  const fetchOrder = async () => {
    try {
      setLoading(true);
      await keycloak.updateToken(5);

      const response = await fetch(`${API_BASE_URL}/orders/${orderId}`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to fetch order');
      }

      const data = await response.json();
      setOrder(data);
    } catch (err) {
      console.error('Error fetching order:', err);
      setError('Failed to load order details');
    } finally {
      setLoading(false);
    }
  };

  const getStatusSteps = () => {
    const steps = [
      { key: 'pending', label: 'Order Placed', icon: Clock },
      { key: 'processing', label: 'Processing', icon: Package },
      { key: 'shipped', label: 'Shipped', icon: Truck },
      { key: 'delivered', label: 'Delivered', icon: CheckCircle }
    ];

    const statusOrder = ['pending', 'processing', 'shipped', 'delivered'];
    const currentStatusIndex = statusOrder.indexOf(order?.order_status);

    return steps.map((step, index) => ({
      ...step,
      completed: index <= currentStatusIndex,
      active: index === currentStatusIndex
    }));
  };

  const handleTrackingClick = () => {
    if (order?.tracking_number) {
      const trackingUrl = `https://www.example-shipping.com/track/${order.tracking_number}`;
      window.open(trackingUrl, '_blank');
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const calculateDaysUntilDelivery = () => {
    if (!order?.estimated_delivery_date) return null;
    const today = new Date();
    const deliveryDate = new Date(order.estimated_delivery_date);
    const diffTime = deliveryDate - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto">
        <Card className="p-8">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
            <p className="ml-4 text-muted-foreground">Loading order details...</p>
          </div>
        </Card>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="max-w-4xl mx-auto">
        <Card className="p-8">
          <div className="text-center">
            <p className="text-destructive mb-4">{error || 'Order not found'}</p>
            <Button onClick={() => navigate('/orders')}>
              Back to Orders
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  const statusSteps = getStatusSteps();
  const daysUntilDelivery = calculateDaysUntilDelivery();

  return (
    <div className="max-w-4xl mx-auto">
      <Button
        variant="ghost"
        onClick={() => navigate('/orders')}
        className="mb-4 gap-2"
      >
        <ArrowLeft size={20} />
        Back to Orders
      </Button>

      <Card className="p-6 mb-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h1 className="text-2xl font-bold">Order #{order.order_id}</h1>
            <p className="text-muted-foreground">Placed on {formatDate(order.created_at)}</p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-bold">{parseFloat(order.total_amount).toFixed(2)} €</p>
            <Badge variant={
              order.order_status === 'delivered' ? 'default' :
              order.order_status === 'shipped' ? 'secondary' :
              order.order_status === 'processing' ? 'default' :
              'warning'
            }>
              {order.order_status.charAt(0).toUpperCase() + order.order_status.slice(1)}
            </Badge>
          </div>
        </div>

        {order.tracking_number && (
          <Card className="p-4 mb-4 bg-primary/5 border-primary/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">Tracking Number</p>
                <p className="text-lg font-semibold">{order.tracking_number}</p>
                <p className="text-sm text-muted-foreground">Carrier: {order.shipping_provider || 'Standard Shipping'}</p>
              </div>
              <Button onClick={handleTrackingClick} className="gap-2">
                Track Package
                <ExternalLink size={16} />
              </Button>
            </div>
          </Card>
        )}

        {order.estimated_delivery_date && order.order_status !== 'delivered' && (
          <Card className="p-4 bg-green-50 dark:bg-green-900/20 border-green-500/20">
            <p className="text-sm text-muted-foreground">Estimated Delivery</p>
            <p className="text-lg font-semibold">
              {new Date(order.estimated_delivery_date).toLocaleDateString('en-US', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
              })}
            </p>
            {daysUntilDelivery !== null && daysUntilDelivery >= 0 && (
              <p className="text-sm text-green-600 dark:text-green-200 font-medium">
                {daysUntilDelivery === 0 ? 'Arriving today!' : `Arriving in ${daysUntilDelivery} day${daysUntilDelivery > 1 ? 's' : ''}`}
              </p>
            )}
          </Card>
        )}
      </Card>

      <Card className="p-6 mb-6">
        <h2 className="text-xl font-bold mb-6">Order Progress</h2>
        
        <div className="relative">
          <div className="absolute top-5 left-0 right-0 h-1 bg-gray-200 dark:bg-gray-700">
            <div 
              className="h-full bg-blue-600 transition-all duration-500"
              style={{ 
                width: `${(statusSteps.filter(s => s.completed).length - 1) / (statusSteps.length - 1) * 100}%` 
              }}
            ></div>
          </div>

          <div className="relative flex justify-between">
            {statusSteps.map((step) => {
              const Icon = step.icon;
              return (
                <div key={step.key} className="flex flex-col items-center" style={{ zIndex: 1 }}>
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center mb-2 ${
                    step.completed ? 'bg-blue-600 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-400 dark:text-gray-300'
                  }`}>
                    <Icon size={20} />
                  </div>
                  <p className={`text-sm font-medium ${
                    step.active ? 'text-blue-600' : step.completed ? 'text-gray-800 dark:text-gray-100' : 'text-gray-400 dark:text-gray-400'
                  }`}>
                    {step.label}
                  </p>
                  {step.active && (
                    <p className="text-xs text-blue-600 mt-1">Current Status</p>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </Card>

      <Card className="p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Shipping Address</h2>
        {order.shipping_address && (
          <div className="text-gray-700 dark:text-gray-300">
            <p>{order.shipping_address.fullName || order.shipping_address.name}</p>
            <p>{order.shipping_address.address}</p>
            <p>{order.shipping_address.city}, {order.shipping_address.postalCode}</p>
            {order.shipping_address.phone && <p>Phone: {order.shipping_address.phone}</p>}
          </div>
        )}
      </Card>

      <Card className="p-6">
        <h2 className="text-xl font-bold mb-4">Order Items</h2>
        <div className="space-y-4">
          {order.items && order.items.map((item, index) => (
            <div key={index} className="flex items-center border-b border-gray-200 dark:border-gray-700 pb-4 last:border-0">
              {item.images && item.images[0] && (
                <img 
                  src={item.images[0]} 
                  alt={item.product_name}
                  className="w-20 h-20 object-cover rounded-lg mr-4"
                />
              )}
              <div className="flex-1">
                <h3 className="font-semibold text-gray-800 dark:text-gray-100">{item.product_name}</h3>
                <p className="text-sm text-gray-600 dark:text-gray-300">Quantity: {item.quantity}</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-gray-800 dark:text-gray-100">{parseFloat(item.subtotal).toFixed(2)} €</p>
                <p className="text-sm text-gray-600 dark:text-gray-300">{parseFloat(item.unit_price).toFixed(2)} € each</p>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t">
          <div className="flex justify-between mb-2">
            <span className="text-muted-foreground">Subtotal</span>
            <span>{parseFloat(order.total_amount - (order.shipping_cost || 0)).toFixed(2)} €</span>
          </div>
          <div className="flex justify-between mb-2">
            <span className="text-muted-foreground">Shipping</span>
            <span>{parseFloat(order.shipping_cost || 0).toFixed(2)} €</span>
          </div>
          <div className="flex justify-between text-lg font-bold">
            <span>Total</span>
            <span>{parseFloat(order.total_amount).toFixed(2)} €</span>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default OrderTrackingView;
