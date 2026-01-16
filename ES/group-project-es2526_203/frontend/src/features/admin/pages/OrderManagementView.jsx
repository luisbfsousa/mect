import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { Search, Package, TrendingUp, Clock, Truck, CheckCircle, CreditCard, X } from 'lucide-react';
import { ordersAPI, API_BASE_URL } from '../../../services/api';
import AdminNavTabs from '../components/AdminNavTabs';
import { Card } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Button } from '../../../components/ui/button';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';

const OrderManagementView = () => {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const [orders, setOrders] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showShipModal, setShowShipModal] = useState(false);
  const [shipmentDetails, setShipmentDetails] = useState({
    trackingNumber: '',
    shippingProvider: 'Standard Shipping'
  });

  useEffect(() => {
    if (keycloak.authenticated) {
      fetchOrders();
      fetchStats();
    }
  }, [keycloak.authenticated, filterStatus, searchTerm]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      await keycloak.updateToken(5);
      
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (searchTerm) params.append('search', searchTerm);
      
      const response = await fetch(`${API_BASE_URL}/admin/orders?${params}`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch orders: ${response.status}`);
      }
      
      const data = await response.json();
      console.log('Fetched orders:', data);
      setOrders(data);
    } catch (error) {
      console.error('Failed to fetch orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      await keycloak.updateToken(5);
      
      const response = await fetch(`${API_BASE_URL}/admin/orders/stats`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        console.warn('Stats endpoint not fully implemented, skipping...');
        return;
      }
      
      const text = await response.text();
      if (text) {
        const data = JSON.parse(text);
        setStats(data);
      }
    } catch (error) {
      console.error('Failed to fetch stats:', error);
      // Stats are optional, don't fail the page
    }
  };

  const confirmPayment = async (orderId) => {
    if (!window.confirm('Confirm payment for this order? This will move it to processing status.')) {
      return;
    }

    try {
      await keycloak.updateToken(5);
      
      const response = await fetch(`${API_BASE_URL}/admin/orders/${orderId}/confirm-payment`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to confirm payment');
      }

      alert('✅ Payment confirmed! Customer has been notified.');
      fetchOrders();
      fetchStats();
    } catch (error) {
      console.error('Failed to confirm payment:', error);
      alert('❌ Failed to confirm payment: ' + error.message);
    }
  };

  const openShipModal = (order) => {
    setSelectedOrder(order);
    setShipmentDetails({
      trackingNumber: order.tracking_number || '',
      shippingProvider: order.shipping_provider || 'Standard Shipping'
    });
    setShowShipModal(true);
  };

  const closeShipModal = () => {
    setShowShipModal(false);
    setSelectedOrder(null);
    setShipmentDetails({
      trackingNumber: '',
      shippingProvider: 'Standard Shipping'
    });
  };

  const markAsShipped = async () => {
    if (!selectedOrder) return;

    try {
      await keycloak.updateToken(5);
      
      const params = new URLSearchParams();
      if (shipmentDetails.trackingNumber) {
        params.append('trackingNumber', shipmentDetails.trackingNumber);
      }
      if (shipmentDetails.shippingProvider) {
        params.append('shippingProvider', shipmentDetails.shippingProvider);
      }

      const response = await fetch(`${API_BASE_URL}/admin/orders/${selectedOrder.order_id}/ship?${params}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('Failed to mark order as shipped');
      }

      alert('📦 Order marked as shipped! Customer has been notified with tracking details.');
      closeShipModal();
      fetchOrders();
      fetchStats();
    } catch (error) {
      console.error('Failed to mark as shipped:', error);
      alert('❌ Failed to mark order as shipped: ' + error.message);
    }
  };

  const updateOrderStatus = async (orderId, newStatus) => {
    try {
      await ordersAPI.updateStatus(orderId, newStatus);
      fetchOrders();
      fetchStats();
      alert('✅ Order status updated successfully! Customer has been notified.');
    } catch (error) {
      console.error('Failed to update order:', error);
      alert('❌ Failed to update order status');
    }
  };

  const getStatusVariant = (status) => {
    const variants = {
      'pending': 'warning',
      'processing': 'default',
      'shipped': 'secondary',
      'delivered': 'default',
      'cancelled': 'destructive'
    };
    return variants[status] || 'secondary';
  };

  const statusOptions = [
    { value: 'pending', label: 'Pending', icon: Clock },
    { value: 'processing', label: 'Processing', icon: Package },
    { value: 'shipped', label: 'Shipped', icon: Truck },
    { value: 'delivered', label: 'Delivered', icon: CheckCircle }
  ];

  return (
    <div className="max-w-7xl mx-auto">
      <AdminNavTabs />

      <Card>
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Order Management</h2>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-300">View and manage customer orders</p>
        </div>

        {/* Filters */}
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" size={20} />
              <input
                type="text"
                placeholder="Search by tracking number..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-gray-100 dark:bg-gray-800 placeholder:text-gray-500 dark:placeholder:text-gray-400 border-gray-200 dark:border-gray-700 border rounded-lg focus:ring-2 focus:ring-ring"
              />
            </div>

            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-4 py-2 bg-gray-100 dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-900 dark:text-gray-100 border rounded-lg focus:ring-2 focus:ring-ring"
            >
              <option value="">All Statuses</option>
              <option value="pending">Pending</option>
              <option value="processing">Processing</option>
              <option value="shipped">Shipped</option>
              <option value="delivered">Delivered</option>
            </select>
          </div>
        </div>

        {/* Orders List */}

        {loading ? (
          <div className="p-8 text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-300">Loading orders...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="p-8 text-center text-gray-600 dark:text-gray-300">
            No orders found
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Order ID</TableHead>
                <TableHead>Items</TableHead>
                <TableHead>Customer</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Total</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.map((order) => (
                <TableRow key={order.order_id}>
                  <TableCell>
                    <div className="font-medium">#{order.order_id}</div>
                    <div className="text-xs text-muted-foreground">{order.tracking_number}</div>
                  </TableCell>
                  <TableCell>
                    {order.items && order.items.length > 0 ? (
                      <div className="space-y-1">
                        {order.items.slice(0, 3).map((item, idx) => (
                          <div key={idx} className="text-xs">
                            <span className="font-medium">{item.product_name || `Product #${item.product_id}`}</span>
                            <span className="text-muted-foreground"> × {item.quantity}</span>
                          </div>
                        ))}
                        {order.items.length > 3 && (
                          <div className="text-xs text-muted-foreground">
                            +{order.items.length - 3} more
                          </div>
                        )}
                      </div>
                    ) : (
                      <span className="text-xs text-muted-foreground">No items</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{order.first_name} {order.last_name}</div>
                    <div className="text-xs text-muted-foreground">{order.email}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm text-muted-foreground">
                      {new Date(order.created_at).toLocaleDateString()}
                    </div>
                  </TableCell>
                  <TableCell>
                    <span className="font-semibold text-green-600 dark:text-green-400">
                      {parseFloat(order.total_amount).toFixed(2)} €
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant={getStatusVariant(order.order_status)}>
                      {order.order_status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                      <div className="flex gap-2">
                        {/* Confirm Payment Button (only for pending orders) */}
                        {order.order_status === 'pending' && (
                          <Button
                            onClick={() => confirmPayment(order.order_id)}
                            size="sm"
                            variant="default"
                            className="gap-1"
                          >
                            <CreditCard size={14} />
                            Confirm Payment
                          </Button>
                        )}

                        {/* Mark as Shipped Button (only for processing orders) */}
                        {order.order_status === 'processing' && (
                          <Button
                            onClick={() => openShipModal(order)}
                            size="sm"
                            className="gap-1"
                          >
                            <Truck size={14} />
                            Ship Order
                          </Button>
                        )}
                        
                        {/* Status Dropdown for other statuses */}
                        <select
                          value={order.order_status}
                          onChange={(e) => updateOrderStatus(order.order_id, e.target.value)}
                          className="border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded px-2 py-1 text-xs focus:ring-2 focus:ring-blue-500"
                        >
                          {statusOptions.map(status => (
                            <option key={status.value} value={status.value}>
                              {status.label}
                            </option>
                          ))}
                        </select>
                      </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>

      {/* Ship Order Modal */}
      <Dialog open={showShipModal} onOpenChange={closeShipModal}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Truck className="text-primary" size={24} />
              Ship Order #{selectedOrder?.order_id}
            </DialogTitle>
            <DialogDescription>
              Customer: {selectedOrder?.first_name} {selectedOrder?.last_name} ({selectedOrder?.email})
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Tracking Number</label>
              <input
                type="text"
                value={shipmentDetails.trackingNumber}
                onChange={(e) => setShipmentDetails({...shipmentDetails, trackingNumber: e.target.value})}
                placeholder="Enter tracking number"
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-ring"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Shipping Provider</label>
              <select
                value={shipmentDetails.shippingProvider}
                onChange={(e) => setShipmentDetails({...shipmentDetails, shippingProvider: e.target.value})}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-ring"
              >
                <option value="Standard Shipping">Standard Shipping</option>
                <option value="Express Shipping">Express Shipping</option>
              </select>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={closeShipModal}>
              Cancel
            </Button>
            <Button onClick={markAsShipped}>
              Mark as Shipped
            </Button>
          </DialogFooter>

          <p className="text-xs text-muted-foreground text-center">
            Customer will be notified via email with tracking details
          </p>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default OrderManagementView;
