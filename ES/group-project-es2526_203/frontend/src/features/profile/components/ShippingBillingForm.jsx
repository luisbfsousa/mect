import React, { useState, useEffect } from 'react';
import ShippingForm from '../../checkout/components/ShippingForm';
import BillingForm from './BillingForm';

const ShippingBillingForm = ({ shippingInfo, billingInfo, onShippingChange, onBillingChange }) => {
  const [sameAsShipping, setSameAsShipping] = useState(false);

  // When "same as shipping" is checked, copy shipping info to billing
  useEffect(() => {
    if (sameAsShipping) {
      onBillingChange({ ...shippingInfo });
    }
  }, [sameAsShipping, shippingInfo, onBillingChange]);

  const handleSameAsShippingChange = (e) => {
    setSameAsShipping(e.target.checked);
  };

  return (
    <div className="space-y-8">
      {/* Shipping Information Section */}
      <div>
        <h3 className="text-lg font-semibold mb-4 text-gray-800">Shipping Information</h3>
        <ShippingForm 
          shippingInfo={shippingInfo} 
          onChange={onShippingChange} 
        />
      </div>

      {/* Billing Information Section */}
      <div>
        <div className="flex items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-800">Billing Information</h3>
        </div>
        
        <div className="mb-4">
          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={sameAsShipping}
              onChange={handleSameAsShippingChange}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span className="text-sm text-gray-600">Billing address is the same as shipping address</span>
          </label>
        </div>

        <BillingForm 
          billingInfo={billingInfo} 
          onChange={onBillingChange} 
        />
      </div>
    </div>
  );
};

export default ShippingBillingForm;
