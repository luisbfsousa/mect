import React from 'react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';

const EmptyProductsState = ({ onClearFilters }) => {
  return (
    <Card>
      <CardContent className="text-center py-12">
        <p className="text-xl text-muted-foreground">No products found</p>
        <div className="mt-4">
          <Button onClick={onClearFilters} variant="link">
            Clear filters
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

export default EmptyProductsState;