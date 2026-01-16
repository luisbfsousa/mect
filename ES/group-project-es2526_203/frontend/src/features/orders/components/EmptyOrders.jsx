import React from 'react';
import { Package } from 'lucide-react';
import { Card } from '../../../components/ui/card';

const EmptyOrders = () => {
  return (
    <Card className="text-center py-12">
      <Package size={64} className="mx-auto text-muted-foreground mb-4" />
      <p className="text-xl">No orders yet</p>
    </Card>
  );
};

export default EmptyOrders;