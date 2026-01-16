import React from 'react';
import ProductForm from './ProductForm';
import { Button } from '../../../components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';

const ProductModal = ({
  isOpen,
  onClose,
  product,
  onChange,
  onSave,
  isEditing,
  loading
}) => {
  const handleSave = (e) => {
    e.preventDefault();
    onSave();
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl">
            {isEditing ? 'Edit Product' : 'Add New Product'}
          </DialogTitle>
          <DialogDescription>
            {isEditing ? 'Update the product information below.' : 'Fill in the details to add a new product to your catalog.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSave}>
          <ProductForm
            product={product}
            onChange={onChange}
          />

          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              disabled={loading}
            >
              {loading ? 'Saving...' : (isEditing ? 'Update Product' : 'Add Product')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default ProductModal;
