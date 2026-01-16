import React, { useEffect, useState } from 'react';
import { X, Mail, Phone, MapPin } from 'lucide-react';
import { API_BASE_URL } from '../../../services/api';

const CustomerDetailModal = ({ customer, keycloak, onClose }) => {
  const [addressInfo, setAddressInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (customer) {
      fetchAddressInfo();
    }
  }, [customer]);

  const fetchAddressInfo = async () => {
    try {
      await keycloak.updateToken(5);
      // This would typically call an endpoint to get shipping/billing info
      // For now, we'll just mark loading as complete
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch address info:', error);
      setLoading(false);
    }
  };

  if (!customer) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white">
          <h3 className="text-lg font-bold text-gray-900">Customer Details</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Personal Information */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">Personal Information</h4>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-gray-600 uppercase">First Name</p>
                <p className="text-sm font-medium text-gray-900">{customer.firstName}</p>
              </div>
              <div>
                <p className="text-xs text-gray-600 uppercase">Last Name</p>
                <p className="text-sm font-medium text-gray-900">{customer.lastName}</p>
              </div>
            </div>
          </div>

          {/* Contact Information */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">Contact Information</h4>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <Mail className="w-4 h-4 text-gray-400" />
                <div>
                  <p className="text-xs text-gray-600 uppercase">Email</p>
                  <p className="text-sm font-medium text-gray-900">{customer.email}</p>
                </div>
              </div>
              {customer.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="w-4 h-4 text-gray-400" />
                  <div>
                    <p className="text-xs text-gray-600 uppercase">Phone</p>
                    <p className="text-sm font-medium text-gray-900">{customer.phone}</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Account Status */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">Account Status</h4>
            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-center gap-2">
                <span className={`w-3 h-3 rounded-full ${customer.isLocked ? 'bg-red-500' : 'bg-green-500'}`}></span>
                <span className="text-sm text-gray-900">
                  {customer.isLocked ? 'Locked' : 'Unlocked'}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`w-3 h-3 rounded-full ${customer.isDeactivated ? 'bg-gray-500' : 'bg-green-500'}`}></span>
                <span className="text-sm text-gray-900">
                  {customer.isDeactivated ? 'Deactivated' : 'Active'}
                </span>
              </div>
            </div>
          </div>

          {/* Account Metadata */}
          <div>
            <h4 className="text-sm font-semibold text-gray-900 mb-4">Account Details</h4>
            <div className="grid grid-cols-1 gap-2 text-xs">
              <div>
                <p className="text-gray-600">User ID</p>
                <p className="font-mono text-gray-900">{customer.userId}</p>
              </div>
              <div>
                <p className="text-gray-600">Role</p>
                <p className="text-gray-900">{customer.role}</p>
              </div>
              {customer.createdAt && (
                <div>
                  <p className="text-gray-600">Created At</p>
                  <p className="text-gray-900">{new Date(customer.createdAt).toLocaleString()}</p>
                </div>
              )}
              {customer.updatedAt && (
                <div>
                  <p className="text-gray-600">Last Updated</p>
                  <p className="text-gray-900">{new Date(customer.updatedAt).toLocaleString()}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onClose}
            className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default CustomerDetailModal;
