import React from 'react';
import { Package, Star } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from '../../../components/ui/table';
import { Card } from '../../../components/ui/card';

const ProductListTable = ({ products, onEdit, onDelete }) => {
  if (products.length === 0) {
    return (
      <div className="text-center py-12">
        <Package className="mx-auto h-16 w-16 text-muted-foreground mb-4" />
        <h3 className="text-xl font-semibold mb-2">No Products Found</h3>
        <p className="text-muted-foreground">Start by adding your first product to the catalog.</p>
      </div>
    );
  }

  return (
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Image</TableHead>
            <TableHead>Name</TableHead>
            <TableHead>Category</TableHead>
            <TableHead>Price</TableHead>
            <TableHead>Stock</TableHead>
            <TableHead>Rating</TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {products.map((product) => (
            <TableRow key={product.id}>
              <TableCell>
                <div className="text-2xl">{product.image}</div>
              </TableCell>
              <TableCell>
                <div className="font-medium">{product.name}</div>
                <div className="text-sm text-muted-foreground truncate max-w-xs">
                  {product.description}
                </div>
              </TableCell>
              <TableCell>
                <Badge variant="secondary">
                  {product.category}
                </Badge>
              </TableCell>
              <TableCell>
                <span className="font-semibold text-green-600 dark:text-green-400">
                  {product.price.toFixed(2)} €
                </span>
              </TableCell>
              <TableCell>
                <Badge variant={
                  product.stock > 10 ? 'default' :
                  product.stock > 0 ? 'warning' :
                  'destructive'
                }>
                  {product.stock} units
                </Badge>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                  <span className="text-sm font-medium">{product.rating}</span>
                  <span className="text-xs text-muted-foreground">({product.reviews})</span>
                </div>
              </TableCell>
              <TableCell>
                <div className="flex gap-2">
                  <Button
                    onClick={() => onEdit(product)}
                    size="sm"
                    variant="ghost"
                  >
                    Edit
                  </Button>
                  <Button
                    onClick={() => onDelete(product.id)}
                    variant="ghost"
                    size="sm"
                    className="text-destructive hover:text-destructive"
                  >
                    Delete
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  );
};

export default ProductListTable;
