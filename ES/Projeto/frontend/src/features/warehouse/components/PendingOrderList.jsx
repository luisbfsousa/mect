import React from 'react';
import { Clock, Package, Truck, CheckCircle } from 'lucide-react';
import { Badge } from '../../../components/ui/badge';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import { Card } from '../../../components/ui/card';

const PendingOrderList = ({
  orders,
  onRefresh 
}) => {
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
    const iconClass = "mr-1";
    switch(status?.toLowerCase()) {
      case 'pending':
        return <Clock size={16} className={iconClass} />;
      case 'processing':
        return <Package size={16} className={iconClass} />;
      case 'shipped':
        return <Truck size={16} className={iconClass} />;
      case 'delivered':
        return <CheckCircle size={16} className={iconClass} />;
      default:
        return null;
    }
  };

  if (!orders || orders.length === 0) {
    return (
      <div className="text-center py-12">
        <Package className="mx-auto h-16 w-16 text-muted-foreground mb-4" />
        <h3 className="text-xl font-semibold mb-2">No Orders Found</h3>
        <p className="text-muted-foreground">There are no orders to display at the moment.</p>
      </div>
    );
  }

  return (
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Order ID</TableHead>
            <TableHead>Customer</TableHead>
            <TableHead>Date</TableHead>
            <TableHead>Items</TableHead>
            <TableHead>Total</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Shipping</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {orders.map((order) => (
            <TableRow key={order.order_id || order.orderId}>
              <TableCell>
                <div className="font-medium">
                  #{order.order_id || order.orderId}
                </div>
              </TableCell>
              <TableCell>
                <div className="font-medium">
                  {order.first_name && order.last_name
                    ? `${order.first_name} ${order.last_name}`
                    : order.user_id || 'N/A'}
                </div>
                {order.email && (
                  <div className="text-sm text-muted-foreground">{order.email}</div>
                )}
              </TableCell>
              <TableCell>
                <div className="text-sm">
                  {new Date(order.created_at || order.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                  })}
                </div>
                <div className="text-xs text-muted-foreground">
                  {new Date(order.created_at || order.createdAt).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </div>
              </TableCell>
              <TableCell>
                {order.items && order.items.length > 0 ? (
                  <div className="space-y-1">
                    {order.items.map((item, index) => (
                      <div key={index} className="text-sm">
                        <span>
                          {item.product_name || `Product #${item.product_id}`}
                        </span>
                        <span className="text-muted-foreground"> × {item.quantity}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <span className="text-muted-foreground text-sm">No items</span>
                )}
              </TableCell>
              <TableCell>
                <span className="font-semibold text-green-600 dark:text-green-400">
                  {parseFloat(order.total_amount || order.totalAmount || order.total || 0).toFixed(2)} €
                </span>
              </TableCell>
              <TableCell>
                <Badge variant={order.order_status?.toLowerCase() || order.orderStatus?.toLowerCase()} className="inline-flex items-center gap-1">
                  {getStatusIcon(order.order_status || order.orderStatus)}
                  {order.order_status || order.orderStatus || 'Unknown'}
                </Badge>
              </TableCell>
              <TableCell>
                {order.tracking_number ? (
                  <div>
                    <div className="text-sm font-medium">
                      {order.tracking_number}
                    </div>
                    {order.shipping_provider && (
                      <div className="text-xs text-muted-foreground">
                        {order.shipping_provider}
                      </div>
                    )}
                  </div>
                ) : order.shipping_address ? (
                  <div className="text-sm text-muted-foreground">
                    {typeof order.shipping_address === 'string'
                      ? order.shipping_address
                      : `${order.shipping_address.city || ''}, ${order.shipping_address.state || ''}`}
                  </div>
                ) : (
                  <span className="text-muted-foreground text-sm">Not assigned</span>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  );
};

export default PendingOrderList;
