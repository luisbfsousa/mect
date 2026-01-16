import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { Button } from '../../../components/ui/button';
import { Card } from '../../../components/ui/card';
import ErrorAlert from '../../../components/ErrorAlert';
import { profileAPI } from '../../../services/api';

const ShippingBillingView = () => {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();

  const [shippingInfo, setShippingInfo] = useState({
    fullName: '',
    address: '',
    city: '',
    postalCode: '',
    phone: ''
  });

  const [billingInfo, setBillingInfo] = useState({
    fullName: '',
    address: '',
    city: '',
    postalCode: '',
    phone: ''
  });

  const [sameAsShipping, setSameAsShipping] = useState(false);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Load existing shipping/billing info on mount
  useEffect(() => {
    const loadShippingBilling = async () => {
      if (!keycloak.authenticated) {
        setInitialLoading(false);
        return;
      }

      try {
        const data = await profileAPI.fetchShippingBilling();
        if (data.shipping) {
          setShippingInfo(data.shipping);
        }
        console.log(data.shipping)
        
        if (data.billing) {
          setBillingInfo(data.billing);
        }

        // Check if billing is same as shipping
        if (data.shipping && data.billing) {
          const isSame = 
            data.shipping.fullName === data.billing.fullName &&
            data.shipping.address === data.billing.address &&
            data.shipping.city === data.billing.city &&
            data.shipping.postalCode === data.billing.postalCode &&
            data.shipping.phone === data.billing.phone;
          setSameAsShipping(isSame);
        }
      } catch (err) {
        console.error('Error loading shipping/billing info:', err);
        setError('Failed to load shipping/billing information');
      } finally {
        setInitialLoading(false);
      }
    };

    loadShippingBilling();
  }, [keycloak]);

  const handleShippingChange = (e) => {
    const { name, value } = e.target;
    setShippingInfo(prev => ({
      ...prev,
      [name]: value
    }));
    
    // If "same as shipping" is checked, update billing too
    if (sameAsShipping) {
      setBillingInfo(prev => ({
        ...prev,
        [name]: value
      }));
    }
  };

  const handleBillingChange = (e) => {
    const { name, value } = e.target;
    setBillingInfo(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSameAsShippingToggle = (e) => {
    const checked = e.target.checked;
    setSameAsShipping(checked);
    
    if (checked) {
      setBillingInfo({ ...shippingInfo });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      setLoading(true);
      
      // Use the API service instead of direct fetch
      await profileAPI.updateShippingBilling(shippingInfo, billingInfo);

      setSuccess('Shipping and billing information updated successfully!');
      
      // Redirect after 2 seconds
      setTimeout(() => {
        navigate('/profile');
      }, 2000);
    } catch (err) {
      console.error('Error updating shipping/billing info:', err);
      setError(err.message || 'Failed to update shipping and billing information');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/profile');
  };

  if (!keycloak.authenticated) {
    return (
      <Card className="max-w-4xl mx-auto p-6">
        <p className="text-center text-muted-foreground">Please log in to access this page.</p>
      </Card>
    );
  }

  if (initialLoading) {
    return (
      <Card className="max-w-4xl mx-auto p-6">
        <p className="text-center text-muted-foreground">Loading...</p>
      </Card>
    );
  }

  return (
    <Card className="max-w-4xl mx-auto p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-800 dark:text-gray-100">Update Shipping & Billing Information</h2>
        <p className="text-gray-600 dark:text-gray-300 mt-2">Manage your default shipping and billing addresses</p>
      </div>

      <ErrorAlert message={error} />
      
      {success && (
        <div className="mb-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded-lg">
          {success}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Shipping Information */}
        <div className="mb-8">
          <h3 className="text-xl font-semibold text-gray-800 dark:text-gray-100 mb-4">Shipping Address</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Full Name *
              </label>
              <input
                type="text"
                name="fullName"
                value={shippingInfo.fullName}
                onChange={handleShippingChange}
                required
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Address *
              </label>
              <input
                type="text"
                name="address"
                value={shippingInfo.address}
                onChange={handleShippingChange}
                required
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  City *
                </label>
                <input
                  type="text"
                  name="city"
                  value={shippingInfo.city}
                  onChange={handleShippingChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Postal Code *
                </label>
                <input
                  type="text"
                  name="postalCode"
                  value={shippingInfo.postalCode}
                  onChange={handleShippingChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Phone *
              </label>
              <input
                type="tel"
                name="phone"
                value={shippingInfo.phone}
                onChange={handleShippingChange}
                required
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
              />
            </div>
          </div>
        </div>

        {/* Billing Same as Shipping Checkbox */}
        <div className="mb-6">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={sameAsShipping}
              onChange={handleSameAsShippingToggle}
              className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <span className="ml-2 text-sm text-gray-700 dark:text-gray-300">
              Billing address is the same as shipping address
            </span>
          </label>
        </div>

        {/* Billing Information */}
        {!sameAsShipping && (
          <div className="mb-8">
            <h3 className="text-xl font-semibold text-gray-800 dark:text-gray-100 mb-4">Billing Address</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Full Name *
                </label>
                <input
                  type="text"
                  name="fullName"
                  value={billingInfo.fullName}
                  onChange={handleBillingChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Address *
                </label>
                <input
                  type="text"
                  name="address"
                  value={billingInfo.address}
                  onChange={handleBillingChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    City *
                  </label>
                  <input
                    type="text"
                    name="city"
                    value={billingInfo.city}
                    onChange={handleBillingChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Postal Code *
                  </label>
                  <input
                    type="text"
                    name="postalCode"
                    value={billingInfo.postalCode}
                    onChange={handleBillingChange}
                    required
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Phone *
                </label>
                <input
                  type="tel"
                  name="phone"
                  value={billingInfo.phone}
                  onChange={handleBillingChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                />
              </div>
            </div>
          </div>
        )}

        <div className="flex space-x-4">
          <Button
            type="button"
            onClick={handleCancel}
            variant="outline"
          >
            Cancel
          </Button>

          <Button
            type="submit"
            disabled={loading}
          >
            {loading ? 'Saving...' : 'Save Changes'}
          </Button>
        </div>
      </form>
    </Card>
  );
};

export default ShippingBillingView;