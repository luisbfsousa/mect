import React from 'react';
import { X, Truck } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';

const ShipOrderModal = ({ order, formData, onChange, onSubmit, onClose, loading }) => {
  if (!order) {
    return null;
  }

  const handleChange = (field) => (event) => {
    onChange(field, event.target.value);
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Truck size={24} />
            Mark Order #{order.order_id} as Shipped
          </DialogTitle>
          <DialogDescription>
            Notify the customer with optional tracking details.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="trackingNumber">
              Tracking Number <span className="text-muted-foreground font-normal">(optional)</span>
            </Label>
            <Input
              id="trackingNumber"
              type="text"
              value={formData.trackingNumber}
              onChange={handleChange('trackingNumber')}
              placeholder="e.g. TRK123456789"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="shippingProvider">
              Shipping Provider <span className="text-muted-foreground font-normal">(optional)</span>
            </Label>
            <Input
              id="shippingProvider"
              type="text"
              value={formData.shippingProvider}
              onChange={handleChange('shippingProvider')}
              placeholder="e.g. DHL, UPS, FedEx"
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={onSubmit}
            disabled={loading}
          >
            {loading ? 'Updating...' : 'Confirm Shipment'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default ShipOrderModal;
