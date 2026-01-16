import React from 'react';
import FormInput from '../../../components/FormInput';

const BillingForm = ({ billingInfo, onChange }) => {
  const handleChange = (field) => (e) => {
    let value = e.target.value;
    
    // Phone field: only allow numbers, spaces, hyphens, and plus sign
    if (field === 'phone') {
      value = value.replace(/[^\d\s\-+()]/g, '');
    }
    
    onChange({ ...billingInfo, [field]: value });
  };

  const getPhoneError = () => {
    const phone = billingInfo.phone;
    if (!phone) return null;
    
    // Check if phone contains any letters or special HTML characters
    if (/[a-zA-Z<>"'`]/g.test(phone)) {
      return 'Phone number should only contain digits, spaces, hyphens, parentheses, and plus sign';
    }
    
    // Check minimum length (at least 7 digits for most phone formats)
    const digitsOnly = phone.replace(/\D/g, '');
    if (digitsOnly.length < 7) {
      return 'Phone number must contain at least 7 digits';
    }
    
    return null;
  };

  const phoneError = getPhoneError();

  return (
    <div className="space-y-4">
      <FormInput
        label="Full Name"
        type="text"
        value={billingInfo.fullName}
        onChange={handleChange('fullName')}
        required
      />
      
      <FormInput
        label="Address"
        type="text"
        value={billingInfo.address}
        onChange={handleChange('address')}
        required
      />
      
      <div className="grid grid-cols-2 gap-4">
        <FormInput
          label="City"
          type="text"
          value={billingInfo.city}
          onChange={handleChange('city')}
          required
        />
        
        <FormInput
          label="Postal Code"
          type="text"
          value={billingInfo.postalCode}
          onChange={handleChange('postalCode')}
          required
        />
      </div>
      
      <div>
        <FormInput
          label="Phone"
          type="tel"
          value={billingInfo.phone}
          onChange={handleChange('phone')}
          placeholder="e.g., +1 (555) 123-4567"
          required
        />
        {phoneError && (
          <p className="mt-2 text-sm text-red-600 dark:text-red-400">
            ⚠️ {phoneError}
          </p>
        )}
      </div>
    </div>
  );
};

export default BillingForm;
