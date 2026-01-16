import React from 'react';
import { Card, CardContent } from '../../../components/ui/card';
import { Label } from '../../../components/ui/label';
import { Button } from '../../../components/ui/button';

const FilterPanel = ({
  categories,
  selectedCategory,
  onCategoryChange,
  priceRange,
  onPriceChange
}) => {
  return (
    <Card className="mt-4">
      <CardContent className="pt-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <Label className="block font-semibold mb-2">Category</Label>
            <div className="flex flex-wrap gap-2">
              {categories.map(cat => (
                <Button
                  key={cat}
                  onClick={() => onCategoryChange(cat)}
                  variant={selectedCategory === cat ? 'default' : 'outline'}
                  size="sm"
                >
                  {cat}
                </Button>
              ))}
            </div>
          </div>
          <div>
            <Label className="block font-semibold mb-2">
              Price Range: {priceRange[0]} € - {priceRange[1]} €
            </Label>
            <input
              type="range"
              min="0"
              max="300"
              value={priceRange[1]}
              onChange={(e) => onPriceChange([0, parseInt(e.target.value)])}
              className="w-full"
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default FilterPanel;