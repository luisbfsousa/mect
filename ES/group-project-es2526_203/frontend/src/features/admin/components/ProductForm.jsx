import React from 'react';
import FormInput from '../../../components/FormInput';

const ProductForm = ({ product, onChange }) => {
  const handleChange = (field) => (e) => {
    const value = e.target.type === 'number' ? parseFloat(e.target.value) || 0 : e.target.value;
    onChange({ ...product, [field]: value });
  };

  const handleAttributesChange = (e) => {
    try {
      const attributes = JSON.parse(e.target.value);
      onChange({ ...product, attributes });
    } catch (error) {
      // If JSON is invalid, store as string for now
      onChange({ ...product, attributes: e.target.value });
    }
  };

  return (
    <div className="space-y-4">
      <FormInput
        label="Product Name"
        type="text"
        value={product.name}
        onChange={handleChange('name')}
        required
      />
      
      <div>
        <label className="block font-semibold mb-2 dark:text-gray-200">Description</label>
        <textarea
          value={product.description}
          onChange={handleChange('description')}
          className="w-full border-2 border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-lg px-4 py-2 focus:border-blue-500 focus:outline-none"
          rows="3"
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <FormInput
          label="Price ($)"
          type="number"
          value={product.price}
          onChange={handleChange('price')}
          required
        />
        
        <FormInput
          label="Stock Quantity"
          type="number"
          value={product.stock}
          onChange={handleChange('stock')}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block font-semibold mb-2">Category</label>
          <select
            value={product.category}
            onChange={handleChange('category')}
            className="w-full border-2 border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-lg px-4 py-2 focus:border-blue-500 focus:outline-none"
            required
          >
            <option value="">Select Category</option>
            <option value="Electronics">Electronics</option>
            <option value="Home">Home</option>
            <option value="Sports">Sports</option>
            <option value="Accessories">Accessories</option>
          </select>
        </div>

        <FormInput
          label="Image (Emoji/URL)"
          type="text"
          value={product.image}
          onChange={handleChange('image')}
          placeholder="🎧 or https://..."
          required
        />
      </div>


      <div>
        <label className="block font-semibold mb-2">Attributes (JSON format)</label>
        <textarea
          value={typeof product.attributes === 'string' ? product.attributes : JSON.stringify(product.attributes, null, 2)}
          onChange={handleAttributesChange}
          className="w-full border-2 border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-lg px-4 py-2 focus:border-blue-500 focus:outline-none font-mono text-sm"
          rows="4"
          placeholder='{"size": ["S", "M", "L"], "color": ["Red", "Blue"]}'
        />
        <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
          Enter attributes as JSON object with arrays for size, color, etc.
        </p>
      </div>
    </div>
  );
};

export default ProductForm;
