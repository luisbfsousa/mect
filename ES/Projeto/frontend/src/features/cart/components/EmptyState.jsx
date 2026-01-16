import React from 'react';
import { Button } from '../../../components/ui/button';

const EmptyState = ({ icon: Icon, title, message, actionLabel, onAction }) => (
  <div className="text-center py-12">
    {Icon && <Icon size={64} className="mx-auto text-muted-foreground mb-4" />}
    <p className="text-xl mb-4">{title}</p>
    {message && <p className="text-muted-foreground mb-4">{message}</p>}
    {actionLabel && onAction && (
      <Button onClick={onAction} size="lg">
        {actionLabel}
      </Button>
    )}
  </div>
);

export default EmptyState;