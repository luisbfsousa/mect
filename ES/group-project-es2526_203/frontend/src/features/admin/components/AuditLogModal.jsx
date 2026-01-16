import React, { useState, useEffect } from 'react';
import { X, Loader } from 'lucide-react';
import { API_BASE_URL } from '../../../services/api';

const AuditLogModal = ({ customer, keycloak, onClose }) => {
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (customer) {
      fetchAuditLogs();
    }
  }, [customer]);

  const fetchAuditLogs = async () => {
    try {
      setLoading(true);
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/admin/customers/${customer.userId}/audit-logs`,
        {
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch audit logs: ${response.status}`);
      }

      const data = await response.json();
      setAuditLogs(data);
    } catch (error) {
      console.error('Failed to fetch audit logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const getActionLabel = (action) => {
    const labels = {
      'UPDATE_CUSTOMER_DETAILS': 'Updated Customer Details',
      'UPDATE_CUSTOMER_ADDRESSES': 'Updated Addresses',
      'RESET_PASSWORD': 'Reset Password',
      'LOCK_ACCOUNT': 'Locked Account',
      'UNLOCK_ACCOUNT': 'Unlocked Account',
      'DEACTIVATE_ACCOUNT': 'Deactivated Account',
      'REACTIVATE_ACCOUNT': 'Reactivated Account'
    };
    return labels[action] || action;
  };

  const getActionColor = (action) => {
    if (action.includes('UPDATE')) return 'bg-blue-50 border-blue-200 text-blue-800';
    if (action.includes('LOCK')) return 'bg-red-50 border-red-200 text-red-800';
    if (action.includes('UNLOCK') || action.includes('REACTIVATE')) return 'bg-green-50 border-green-200 text-green-800';
    if (action.includes('DEACTIVATE')) return 'bg-gray-50 border-gray-200 text-gray-800';
    if (action.includes('RESET')) return 'bg-orange-50 border-orange-200 text-orange-800';
    return 'bg-gray-50 border-gray-200 text-gray-800';
  };

  if (!customer) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-96 overflow-y-auto overscroll-contain relative">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 sticky top-0 bg-white z-10">
          <h3 className="text-lg font-bold text-gray-900">Audit Log - {customer.email}</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader className="w-6 h-6 text-blue-600 animate-spin" />
              <span className="ml-2 text-gray-600">Loading audit logs...</span>
            </div>
          ) : auditLogs.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              No audit logs found for this customer
            </div>
          ) : (
            <div className="space-y-3">
              {auditLogs.map((log) => (
                <div key={log.id} className={`p-4 border rounded-lg ${getActionColor(log.action)}`}>
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <p className="font-semibold text-sm">
                        {getActionLabel(log.action)}
                      </p>
                      <p className="text-xs opacity-75 mt-1">
                        {new Date(log.createdAt).toLocaleString()}
                      </p>
                      <p className="text-xs opacity-75">
                        Admin: {log.adminUserId}
                      </p>
                      {log.details && log.details.reason && (
                        <p className="text-xs mt-2">
                          <strong>Reason:</strong> {log.details.reason}
                        </p>
                      )}
                    </div>
                  </div>
                  
                  {/* Show details if available */}
                  {log.details && Object.keys(log.details).length > 0 && (
                    <div className="mt-3 pt-3 border-t border-current border-opacity-20 text-xs">
                      <details className="cursor-pointer">
                        <summary className="font-semibold opacity-75">Show Details</summary>
                        <pre className="mt-2 text-xs overflow-auto max-h-32 opacity-75 whitespace-pre-wrap break-words">
                          {JSON.stringify(log.details, null, 2)}
                        </pre>
                      </details>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
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

export default AuditLogModal;
