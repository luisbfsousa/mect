import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import OrderCard from '../components/OrderCard';
import EmptyOrders from '../components/EmptyOrders';
import { Package, Search, Filter } from 'lucide-react';

const OrdersView = ({ orderHistory = [] }) => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });

  // Debug log - INSIDE the component
  console.log('OrdersView received orderHistory:', orderHistory);

  // Filter orders based on search, status, and date range
  const filteredOrders = orderHistory.filter(order => {
    // Search filter
    const matchesSearch = searchTerm === '' || 
      order.order_id?.toString().includes(searchTerm) ||
      order.tracking_number?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.items?.some(item => 
        item.product_name?.toLowerCase().includes(searchTerm.toLowerCase())
      );

    // Status filter
    const matchesStatus = filterStatus === 'all' || order.order_status === filterStatus;

    // Date range filter
    const orderDate = new Date(order.created_at);
    const matchesDateRange = 
      (!dateRange.start || orderDate >= new Date(dateRange.start)) &&
      (!dateRange.end || orderDate <= new Date(dateRange.end));

    return matchesSearch && matchesStatus && matchesDateRange;
  });

  const handleTrackOrder = (orderId) => {
    navigate(`/orders/${orderId}/track`);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setFilterStatus('all');
    setDateRange({ start: '', end: '' });
  };

  if (!orderHistory || orderHistory.length === 0) {
    return <EmptyOrders />;
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Order History</h1>
      </div>

      {/* Filters Section */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Search */}
            <div>
              <Label htmlFor="search" className="mb-2">
                Search Orders
              </Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" size={20} />
                <Input
                  id="search"
                  type="text"
                  placeholder="Order ID, tracking number, or product..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>

            {/* Status Filter */}
            <div>
              <Label htmlFor="status" className="mb-2">
                Order Status
              </Label>
              <select
                id="status"
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                className="w-full px-4 py-2 border border-input rounded-md bg-background focus:ring-2 focus:ring-ring focus:border-transparent"
              >
                <option value="all">All Orders</option>
                <option value="pending">Pending</option>
                <option value="processing">Processing</option>
                <option value="shipped">Shipped</option>
                <option value="delivered">Delivered</option>
              </select>
            </div>

            {/* Date Range */}
            <div>
              <Label className="mb-2">
                Date Range
              </Label>
              <div className="flex space-x-2">
                <Input
                  type="date"
                  value={dateRange.start}
                  onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
                  className="flex-1 text-sm"
                />
                <span className="self-center text-muted-foreground">to</span>
                <Input
                  type="date"
                  value={dateRange.end}
                  onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
                  className="flex-1 text-sm"
                />
              </div>
            </div>
          </div>

          {/* Active Filters Display */}
          {(searchTerm || filterStatus !== 'all' || dateRange.start || dateRange.end) && (
            <div className="mt-4 flex items-center justify-between">
              <div className="flex items-center text-sm text-muted-foreground">
                <Filter size={16} className="mr-2" />
                <span>
                  Showing {filteredOrders.length} of {orderHistory.length} orders
                </span>
              </div>
              <Button
                onClick={clearFilters}
                variant="ghost"
                size="sm"
              >
                Clear all filters
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Orders List */}
      {filteredOrders.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center">
            <Package size={48} className="mx-auto mb-4 text-muted-foreground" />
            <h3 className="text-xl font-semibold mb-2">No orders found</h3>
            <p className="text-muted-foreground mb-4">Try adjusting your filters</p>
            <Button
              onClick={clearFilters}
              variant="ghost"
            >
              Clear filters
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order) => (
            <OrderCard 
              key={order.order_id} 
              order={order}
              onTrack={() => handleTrackOrder(order.order_id)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default OrdersView;