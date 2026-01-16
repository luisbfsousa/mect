import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { Search, Package, Clock, Truck, Filter, CheckCircle, FileText } from 'lucide-react';
import { API_BASE_URL } from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Input } from '../../../components/ui/input';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import ErrorAlert from '../../../components/ErrorAlert';
import OrderItemsModal from '../components/OrderItemsModal';
import ShipOrderModal from '../components/ShipOrderModal';
import PackingSlipModal from '../components/PackingSlipModal';

const WarehouseOrdersView = () => {
  const { keycloak } = useKeycloak();
  const [orders, setOrders] = useState([]);
  const canShipOrders = true;
  const canDeliverOrders = true;
  const canViewOrderItems = true;
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterCustomer, setFilterCustomer] = useState('');
  const [itemsModalOrder, setItemsModalOrder] = useState(null);
  const [shipModalOpen, setShipModalOpen] = useState(false);
  const [shipOrder, setShipOrder] = useState(null);
  const [packingSlipModalOrder, setPackingSlipModalOrder] = useState(null);
  const [shipForm, setShipForm] = useState({
    trackingNumber: '',
    shippingProvider: 'Standard Shipping'
  });
  const [actionLoading, setActionLoading] = useState(false);
  const showActionsColumn = true;

  useEffect(() => {
    if (keycloak.authenticated) {
      fetchOrders();
    }
    // eslint-disable-next-line
  }, [keycloak.authenticated, filterStatus, filterCustomer]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);

      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (filterCustomer) params.append('customer', filterCustomer);

      const response = await fetch(`${API_BASE_URL}/warehouse/orders?${params}`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to fetch orders: ${response.status} - ${errorText}`);
      }

      const data = await response.json();
      setOrders(Array.isArray(data) ? data : []);
    } catch (err) {
      setError('Failed to load orders. Please check your permissions.');
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const handleClearFilters = () => {
    setFilterStatus('');
    setFilterCustomer('');
    setError('');
    setSuccess('');
  };

  const handleViewItems = (order) => {
    setItemsModalOrder(order);
  };

  const handleCloseModal = () => {
    setItemsModalOrder(null);
  };

  const handleOpenPackingSlip = (order) => {
    setPackingSlipModalOrder(order);
  };

  const handleClosePackingSlip = () => {
    setPackingSlipModalOrder(null);
  };

  const openShipModal = (order) => {
    setError('');
    setSuccess('');
    setShipOrder(order);
    setShipForm({
      trackingNumber: order?.tracking_number || '',
      shippingProvider: order?.shipping_provider || 'Standard Shipping'
    });
    setShipModalOpen(true);
  };

  const closeShipModal = () => {
    setShipModalOpen(false);
    setShipOrder(null);
    setShipForm({
      trackingNumber: '',
      shippingProvider: 'Standard Shipping'
    });
  };

  const handleShipFormChange = (field, value) => {
    setShipForm((prev) => ({
      ...prev,
      [field]: value
    }));
  };

  const handleMarkAsShipped = async () => {
    if (!shipOrder) {
      return;
    }

    try {
      setActionLoading(true);
      setError('');
      setSuccess('');

      await keycloak.updateToken(5);

      const response = await fetch(`${API_BASE_URL}/warehouse/orders/${shipOrder.order_id}/ship`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          tracking_number: shipForm.trackingNumber || undefined,
          shipping_provider: shipForm.shippingProvider || undefined
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to mark order as shipped.');
      }

      setSuccess('📦 Order marked as shipped. Customer has been notified.');
      closeShipModal();
      await fetchOrders();
    } catch (err) {
      setError(err.message || 'Failed to mark order as shipped.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleMarkAsDelivered = async (order) => {
    if (!order) {
      return;
    }

    const confirmed = window.confirm(
      'Confirm that this order has been delivered to the customer? Notifications will be sent after confirmation.'
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError('');
      setSuccess('');

      await keycloak.updateToken(5);

      const response = await fetch(`${API_BASE_URL}/warehouse/orders/${order.order_id}/deliver`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ confirm: true })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to mark order as delivered.');
      }

      setSuccess('✅ Order marked as delivered. Notifications sent to customer and staff.');
      await fetchOrders();
    } catch (err) {
      setError(err.message || 'Failed to mark order as delivered.');
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'pending': 'bg-yellow-100 text-yellow-800 border-yellow-300',
      'processing': 'bg-blue-100 text-blue-800 border-blue-300',
      'shipped': 'bg-purple-100 text-purple-800 border-purple-300',
      'delivered': 'bg-green-100 text-green-800 border-green-300',
      'cancelled': 'bg-red-100 text-red-800 border-red-300'
    };
    return colors[status?.toLowerCase()] || 'bg-gray-100 text-gray-800 border-gray-300';
  };

  const getStatusIcon = (status) => {
    switch(status?.toLowerCase()) {
      case 'pending':
        return <Clock size={16} className="inline mr-1" />;
      case 'processing':
        return <Package size={16} className="inline mr-1" />;
      case 'shipped':
        return <Truck size={16} className="inline mr-1" />;
      default:
        return null;
    }
  };

  if (!keycloak.authenticated) {
    return (
      <Card className="max-w-7xl mx-auto">
        <CardContent className="p-6">
          <p className="text-center text-muted-foreground">Please log in to access warehouse.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Warehouse</CardTitle>
          <CardDescription>Manage and track client orders</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">

          <ErrorAlert message={error} />

          {success && (
            <div className="p-4 bg-green-100 dark:bg-green-900 border border-green-400 dark:border-green-700 text-green-700 dark:text-green-100 rounded-lg">
              {success}
            </div>
          )}

          {/* Filters */}
          <Card className="bg-muted/50">
            <CardContent className="p-4">
              <div className="flex items-center mb-3">
                <Filter size={20} className="text-muted-foreground mr-2" />
                <h2 className="text-lg font-semibold">Filters</h2>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="px-4 py-2 border border-input rounded-lg focus:ring-2 focus:ring-ring focus:border-transparent bg-background"
                >
                  <option value="">All Statuses</option>
                  <option value="pending">Pending</option>
                  <option value="processing">Processing</option>
                  <option value="shipped">Shipped</option>
                  <option value="delivered">Delivered</option>
                  <option value="cancelled">Cancelled</option>
                </select>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" size={20} />
                  <Input
                    type="text"
                    placeholder="Search by customer..."
                    value={filterCustomer}
                    onChange={(e) => setFilterCustomer(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button
                  onClick={handleClearFilters}
                  variant="secondary"
                >
                  Clear Filters
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Orders List */}
          {loading && orders.length === 0 ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
              <p className="ml-4 text-muted-foreground">Loading orders...</p>
            </div>
          ) : orders.length === 0 ? (
            <div className="text-center py-12">
              <Package className="mx-auto h-16 w-16 text-muted-foreground mb-4" />
              <h3 className="text-xl font-semibold mb-2">No Orders Found</h3>
              <p className="text-muted-foreground">
                {(filterStatus || filterCustomer)
                  ? 'Try adjusting your filters to see more orders.'
                  : 'There are no orders at the moment.'}
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order ID</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Items</TableHead>
                  <TableHead>Total</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Tracking</TableHead>
                  {showActionsColumn && (
                    <TableHead>Actions</TableHead>
                  )}
                </TableRow>
              </TableHeader>
              <TableBody>
                  {orders.map((order) => (
                    <TableRow key={order.order_id}>
                      <TableCell>
                        <div className="font-medium">
                          #{order.order_id}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          {order.username && !/^[0-9a-fA-F-]{36}$/.test(order.username)
                            ? order.username
                            : order.first_name && order.last_name
                              ? `${order.first_name} ${order.last_name}`
                              : order.user_id}
                        </div>
                        {order.email && (
                          <div className="text-xs text-muted-foreground">{order.email}</div>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="text-sm text-muted-foreground">
                          {new Date(order.created_at).toLocaleDateString()}
                        </div>
                      </TableCell>
                      <TableCell>
                        {canViewOrderItems ? (
                          <Button
                            onClick={() => handleViewItems(order)}
                            variant="link"
                            className="h-auto p-0 flex items-center gap-1"
                          >
                            <Package size={16} />
                            {order.items?.length || 0} item(s)
                          </Button>
                        ) : (
                          <span className="text-muted-foreground flex items-center gap-1 text-sm">
                            <Package size={16} />
                            Items hidden
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        <span className="font-semibold text-green-600 dark:text-green-400">
                          {parseFloat(order.total_amount || 0).toFixed(2)} €
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={order.order_status?.toLowerCase()} className="inline-flex items-center gap-1">
                          {getStatusIcon(order.order_status)}
                          {order.order_status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {order.tracking_number ? (
                          <div>
                            <div className="text-sm font-medium">{order.tracking_number}</div>
                            {order.shipping_provider && (
                              <div className="text-xs text-muted-foreground">{order.shipping_provider}</div>
                            )}
                          </div>
                        ) : (
                          <span className="text-muted-foreground">-</span>
                        )}
                      </TableCell>
                      {showActionsColumn && (
                        <TableCell>
                          <div className="flex gap-2 flex-wrap">
                            <Button
                              type="button"
                              onClick={() => handleOpenPackingSlip(order)}
                              variant="outline"
                              size="sm"
                              className="flex items-center gap-2"
                              title="Generate packing slip"
                            >
                              <FileText size={16} />
                              Packing Slip
                            </Button>
                            {(() => {
                              const status = (order.order_status || '').toLowerCase();

                              if (status === 'processing' && canShipOrders) {
                                return (
                                  <Button
                                    type="button"
                                    onClick={() => openShipModal(order)}
                                    variant="outline"
                                    size="sm"
                                    className="flex items-center gap-2"
                                    disabled={actionLoading}
                                  >
                                    <Truck size={16} />
                                    Mark as Shipped
                                  </Button>
                                );
                              }

                              if (status === 'shipped' && canDeliverOrders) {
                                return (
                                  <Button
                                    type="button"
                                    onClick={() => handleMarkAsDelivered(order)}
                                    variant="outline"
                                    size="sm"
                                    className="flex items-center gap-2 text-green-600 hover:text-green-700"
                                    disabled={actionLoading}
                                  >
                                    <CheckCircle size={16} />
                                    Mark as Delivered
                                  </Button>
                                );
                              }

                              return null;
                            })()}
                          </div>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
          )}
        </CardContent>
      </Card>

      {/* Modal to inspect order items */}
      {itemsModalOrder && canViewOrderItems && (
        <OrderItemsModal 
          order={itemsModalOrder} 
          onClose={handleCloseModal}
        />
      )}

      {shipModalOpen && canShipOrders && (
        <ShipOrderModal
          order={shipOrder}
          formData={shipForm}
          onChange={handleShipFormChange}
          onSubmit={handleMarkAsShipped}
          onClose={closeShipModal}
          loading={actionLoading}
        />
      )}

      {packingSlipModalOrder && (
        <PackingSlipModal
          order={packingSlipModalOrder}
          onClose={handleClosePackingSlip}
          keycloak={keycloak}
        />
      )}
    </div>
  );
};

export default WarehouseOrdersView;
