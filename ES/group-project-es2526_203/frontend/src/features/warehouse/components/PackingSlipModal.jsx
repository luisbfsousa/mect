import React, { useState } from 'react';
import { X, FileText, Download, Printer, AlertCircle } from 'lucide-react';
import { API_BASE_URL } from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';
import { Separator } from '../../../components/ui/separator';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';

const PackingSlipModal = ({ order, onClose, keycloak }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [packingSlip, setPackingSlip] = useState(null);

  if (!order) return null;

  const fetchPackingSlip = async () => {
    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/warehouse/orders/${order.order_id}/packing-slip`,
        {
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error('Failed to fetch packing slip data');
      }

      const data = await response.json();
      setPackingSlip(data);
    } catch (err) {
      setError(err.message || 'Failed to load packing slip');
      console.error('Error fetching packing slip:', err);
    } finally {
      setLoading(false);
    }
  };

  const downloadPDF = async () => {
    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/warehouse/orders/${order.order_id}/packing-slip/pdf`,
        {
          headers: {
            'Authorization': `Bearer ${keycloak.token}`
          }
        }
      );

      if (!response.ok) {
        throw new Error('Failed to download PDF');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `packing-slip-${order.order_id}.pdf`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message || 'Failed to download PDF');
      console.error('Error downloading PDF:', err);
    } finally {
      setLoading(false);
    }
  };

  const printPDF = async () => {
    try {
      setLoading(true);
      setError('');
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/warehouse/orders/${order.order_id}/packing-slip/pdf`,
        {
          headers: {
            'Authorization': `Bearer ${keycloak.token}`
          }
        }
      );

      if (!response.ok) {
        throw new Error('Failed to generate PDF for printing');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const printWindow = window.open(url, '_blank');
      if (printWindow) {
        printWindow.onload = () => {
          printWindow.print();
        };
      }
    } catch (err) {
      setError(err.message || 'Failed to print');
      console.error('Error printing:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return parseFloat(value || 0).toFixed(2);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatAddress = (address) => {
    if (!address) return 'Address not available';
    
    const parts = [];
    if (address.street) parts.push(address.street);
    if (address.city) parts.push(address.city);
    if (address.state) parts.push(address.state);
    if (address.zipCode) parts.push(address.zipCode);
    if (address.country) parts.push(address.country);
    
    return parts.join(', ');
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText size={24} />
            Packing Slip
          </DialogTitle>
          <DialogDescription>
            Order #{order.order_id}
          </DialogDescription>
        </DialogHeader>

        {/* Content */}
        <div className="overflow-y-auto flex-1 px-6">
          {error && (
            <div className="bg-destructive/10 border border-destructive/20 text-destructive px-6 py-3 rounded-lg flex items-start gap-2 mb-4">
              <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {!packingSlip ? (
            <div className="py-8 text-center">
              <FileText size={48} className="mx-auto mb-4 text-muted-foreground" />
              <p className="text-muted-foreground mb-6">
                Generate the packing slip to view order details, product information, and shipping address.
              </p>
              <Button
                onClick={fetchPackingSlip}
                disabled={loading}
              >
                {loading ? 'Generating...' : 'Generate Packing Slip'}
              </Button>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Order Header */}
              <Card className="bg-muted/50">
                <CardContent className="p-4">
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <p className="text-muted-foreground">Order ID</p>
                      <p className="font-bold text-lg">{packingSlip.order_id}</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Customer</p>
                      <p className="font-medium">{packingSlip.first_name || packingSlip.last_name ? `${packingSlip.first_name || ''} ${packingSlip.last_name || ''}`.trim() : 'N/A'}</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Order Date</p>
                      <p className="font-medium">{formatDate(packingSlip.created_at)}</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground">Status</p>
                      <p className="font-medium capitalize">{packingSlip.order_status}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Tracking Information */}
              <Card className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-700">
                <CardContent className="p-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-semibold text-blue-600 dark:text-blue-300">Tracking Number</p>
                      <p className="font-mono text-lg">{packingSlip.tracking_number || 'N/A'}</p>
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-blue-600 dark:text-blue-300">Shipping Provider</p>
                      <p className="font-medium">{packingSlip.shipping_provider || 'N/A'}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Shipping Address */}
              <div>
                <h4 className="text-lg font-bold mb-2">Ship To:</h4>
                <Card className="bg-muted/50">
                  <CardContent className="p-4">
                    <p className="whitespace-pre-line">
                      {formatAddress(packingSlip.shipping_address)}
                    </p>
                  </CardContent>
                </Card>
              </div>

              {/* Items Table */}
              <div>
                <h4 className="text-lg font-bold mb-2">Items to Pack ({packingSlip.item_count}):</h4>
                <Card>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Product Name</TableHead>
                        <TableHead className="text-center">Product ID</TableHead>
                        <TableHead className="text-center">Qty</TableHead>
                        <TableHead className="text-right">Unit Price</TableHead>
                        <TableHead className="text-right">Subtotal</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {packingSlip.items && packingSlip.items.map((item, index) => (
                        <TableRow key={index}>
                          <TableCell>{item.product_name}</TableCell>
                          <TableCell className="text-center text-muted-foreground">{item.product_id}</TableCell>
                          <TableCell className="text-center font-semibold">{item.quantity}</TableCell>
                          <TableCell className="text-right">${formatCurrency(item.unit_price)}</TableCell>
                          <TableCell className="text-right font-semibold">${formatCurrency(item.subtotal)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Card>
              </div>

              {/* Summary */}
              <Card className="bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-700">
                <CardContent className="p-4">
                  <div className="grid grid-cols-3 gap-4 text-center">
                    <div>
                      <p className="text-sm text-purple-600 dark:text-purple-300">Total Items</p>
                      <p className="text-2xl font-bold">{packingSlip.item_count}</p>
                    </div>
                    <div>
                      <p className="text-sm text-purple-600 dark:text-purple-300">Total Quantity</p>
                      <p className="text-2xl font-bold">{packingSlip.total_quantity}</p>
                    </div>
                    <div>
                      <p className="text-sm text-purple-600 dark:text-purple-300">Total Amount</p>
                      <p className="text-2xl font-bold">${formatCurrency(packingSlip.total_amount)}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card className="bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-700">
                <CardContent className="p-3 text-sm text-center italic text-yellow-800 dark:text-yellow-100">
                  Please verify all items before sealing the package.
                </CardContent>
              </Card>
            </div>
          )}
        </div>

        {/* Footer */}
        <DialogFooter>
          {packingSlip && (
            <>
              <Button
                onClick={printPDF}
                disabled={loading}
                variant="outline"
                className="flex items-center gap-2"
              >
                <Printer size={18} />
                Print
              </Button>
              <Button
                onClick={downloadPDF}
                disabled={loading}
                className="flex items-center gap-2 bg-green-600 hover:bg-green-700"
              >
                <Download size={18} />
                Download PDF
              </Button>
            </>
          )}
          <Button
            onClick={onClose}
            variant="secondary"
          >
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default PackingSlipModal;
