import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { AlertCircle, Info } from 'lucide-react';
import OrderSummary from '../components/OrderSummary';
import ErrorAlert from '../../../components/ErrorAlert';
import ShippingForm from '../components/ShippingForm';
import { Button } from '../../../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { profileAPI } from '../../../services/api';

const CheckoutView = ({
  user,
  cart,
  cartTotal,
  error,
  loading,
  completeOrder
}) => {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  
  const [shippingInfo, setShippingInfo] = useState({
    fullName: user?.name || '',
    address: '',
    city: '',
    postalCode: '',
    phone: ''
  });
  
  const [loadingShipping, setLoadingShipping] = useState(true);
  const [shippingError, setShippingError] = useState('');

  // Load saved shipping information
  useEffect(() => {
    const loadShippingInfo = async () => {
      if (!keycloak.authenticated) {
        setLoadingShipping(false);
        return;
      }

      try {
        const data = await profileAPI.fetchShippingBilling();
        
        // If shipping data exists, use it
        if (data.shipping && data.shipping.fullName) {
          setShippingInfo(data.shipping);
        }
      } catch (err) {
        console.error('Error loading shipping info:', err);
        setShippingError('Could not load saved shipping information');
      } finally {
        setLoadingShipping(false);
      }
    };

    loadShippingInfo();
  }, [keycloak]);

  // Redirect if cart is empty
  useEffect(() => {
    if (!cart || cart.length === 0) {
      alert('Your cart is empty!');
      navigate('/cart');
    }
  }, [cart, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Double check cart is not empty
    if (!cart || cart.length === 0) {
      alert('Your cart is empty!');
      navigate('/cart');
      return;
    }

    // Validate phone number
    const phone = shippingInfo.phone;
    if (!phone) {
      alert('Phone number is required');
      return;
    }

    // Check for invalid characters (XSS prevention)
    if (/[a-zA-Z<>"'`]/g.test(phone)) {
      alert('Phone number contains invalid characters. Please use only numbers, spaces, hyphens, parentheses, and plus sign.');
      return;
    }

    // Check minimum phone length (at least 7 digits)
    const digitsOnly = phone.replace(/\D/g, '');
    if (digitsOnly.length < 7) {
      alert('Phone number must contain at least 7 digits');
      return;
    }

    try {
      await completeOrder(shippingInfo);
    } catch (err) {
      console.error('Checkout error:', err);
    }
  };

  // Show loading while cart is being validated or shipping info is loading
  if (!cart || cart.length === 0 || loadingShipping) {
    return (
      <Card className="max-w-2xl mx-auto">
        <CardContent className="p-6">
          <p className="text-center text-muted-foreground">Loading...</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle>Checkout</CardTitle>
      </CardHeader>
      <CardContent>
        <ErrorAlert message={error || shippingError} />

        {shippingInfo.fullName && (
          <div className="mb-4 p-4 bg-primary/10 border border-primary/20 rounded-lg">
            <div className="flex items-start gap-2">
              <Info className="h-4 w-4 text-primary mt-0.5 flex-shrink-0" />
              <p className="text-sm">
                Using your saved shipping information. You can edit it below if needed.
              </p>
            </div>
          </div>
        )}

        <div className="mb-6">
          <OrderSummary cart={cart} cartTotal={cartTotal} />
        </div>

        <form onSubmit={handleSubmit}>
          <ShippingForm
            shippingInfo={shippingInfo}
            onChange={setShippingInfo}
          />

          <div className="mt-6">
            <Button
              type="submit"
              disabled={loading}
              className="w-full"
              size="lg"
            >
              {loading ? 'Processing...' : 'Complete Order'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default CheckoutView;