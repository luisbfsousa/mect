import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { Search, Edit, Lock, Unlock, Trash2, Eye, ChevronDown } from 'lucide-react';
import { API_BASE_URL } from '../../../services/api';
import AdminNavTabs from '../components/AdminNavTabs';
import CustomerDetailModal from '../components/CustomerDetailModal';
import CustomerEditModal from '../components/CustomerEditModal';
import AuditLogModal from '../components/AuditLogModal';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../../components/ui/table';

const CustomerManagementView = () => {
  const { keycloak } = useKeycloak();
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showAuditModal, setShowAuditModal] = useState(false);
  const [actionInProgress, setActionInProgress] = useState(null);
  const [expandedRow, setExpandedRow] = useState(null);

  useEffect(() => {
    if (keycloak.authenticated) {
      fetchCustomers();
    }
  }, [keycloak.authenticated]);

  const fetchCustomers = async () => {
    try {
      setLoading(true);
      await keycloak.updateToken(5);

      const response = await fetch(`${API_BASE_URL}/admin/customers`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch customers: ${response.status}`);
      }

      const data = await response.json();
      // Normalize backend snake_case to camelCase expected by UI
      const normalized = (Array.isArray(data) ? data : []).map((u) => ({
        userId: u.user_id ?? u.userId,
        email: u.email,
        firstName: u.first_name ?? u.firstName,
        lastName: u.last_name ?? u.lastName,
        phone: u.phone,
        role: u.role,
        isLocked: u.is_locked ?? u.isLocked,
        isDeactivated: u.is_deactivated ?? u.isDeactivated,
        createdAt: u.created_at ?? u.createdAt,
        updatedAt: u.updated_at ?? u.updatedAt,
      }));
      setCustomers(normalized);
    } catch (error) {
      console.error('Failed to fetch customers:', error);
      alert('Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  const filteredCustomers = customers.filter(customer =>
    customer.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.lastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    customer.userId?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleViewDetails = async (customer) => {
    setSelectedCustomer(customer);
    setShowDetailModal(true);
  };

  const handleEditCustomer = (customer) => {
    setSelectedCustomer(customer);
    setShowEditModal(true);
  };

  const handleViewAuditLogs = async (customer) => {
    setSelectedCustomer(customer);
    setShowAuditModal(true);
  };

  const handleLockAccount = async (customerId) => {
    if (!window.confirm('Are you sure you want to lock this account?')) {
      return;
    }

    try {
      setActionInProgress(customerId);
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/admin/customers/${customerId}/lock?reason=Locked by administrator`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to lock account: ${response.status}`);
      }

      alert('Account locked successfully');
      fetchCustomers();
    } catch (error) {
      console.error('Failed to lock account:', error);
      alert('Failed to lock account');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleUnlockAccount = async (customerId) => {
    if (!window.confirm('Are you sure you want to unlock this account?')) {
      return;
    }

    try {
      setActionInProgress(customerId);
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/admin/customers/${customerId}/unlock?reason=Unlocked by administrator`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to unlock account: ${response.status}`);
      }

      alert('Account unlocked successfully');
      fetchCustomers();
    } catch (error) {
      console.error('Failed to unlock account:', error);
      alert('Failed to unlock account');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleDeactivateAccount = async (customerId) => {
    if (!window.confirm('Are you sure you want to deactivate this account? The customer will not be able to log in.')) {
      return;
    }

    try {
      setActionInProgress(customerId);
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/admin/customers/${customerId}/deactivate?reason=Account deactivated by administrator`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to deactivate account: ${response.status}`);
      }

      alert('Account deactivated successfully');
      fetchCustomers();
    } catch (error) {
      console.error('Failed to deactivate account:', error);
      alert('Failed to deactivate account');
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReactivateAccount = async (customerId) => {
    if (!window.confirm('Are you sure you want to reactivate this account?')) {
      return;
    }

    try {
      setActionInProgress(customerId);
      await keycloak.updateToken(5);

      const response = await fetch(
        `${API_BASE_URL}/admin/customers/${customerId}/reactivate?reason=Account reactivated by administrator`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to reactivate account: ${response.status}`);
      }

      alert('Account reactivated successfully');
      fetchCustomers();
    } catch (error) {
      console.error('Failed to reactivate account:', error);
      alert('Failed to reactivate account');
    } finally {
      setActionInProgress(null);
    }
  };

  const getStatusBadges = (customer) => {
    const badges = [];
    if (customer.isLocked) {
      badges.push(
        <span key="locked" className="inline-block px-2 py-1 text-xs font-semibold text-red-600 bg-red-100 rounded">
          Locked
        </span>
      );
    }
    if (customer.isDeactivated) {
      badges.push(
        <span key="deactivated" className="inline-block px-2 py-1 text-xs font-semibold text-gray-600 bg-gray-100 rounded">
          Deactivated
        </span>
      );
    }
    return badges.length > 0 ? badges : null;
  };

  return (
    <div className="max-w-7xl mx-auto">
      <AdminNavTabs />

      <div className="bg-white dark:bg-gray-950 rounded-lg shadow border border-gray-200 dark:border-gray-800">
        <div>
          {/* Header */}
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Customer Management</h2>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-300">View and manage customer accounts</p>
          </div>

          {/* Search Bar */}
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <div className="flex items-center gap-2">
              <Search className="w-5 h-5 text-gray-400 dark:text-gray-300" />
              <input
                type="text"
                placeholder="Search by name, email, or ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            {loading ? (
              <div className="p-8 text-center text-gray-500 dark:text-gray-300">Loading customers...</div>
            ) : filteredCustomers.length === 0 ? (
              <div className="p-8 text-center text-gray-500 dark:text-gray-300">No customers found</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Customer</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredCustomers.map((customer) => (
                    <TableRow key={customer.userId}>
                      <TableCell className="whitespace-nowrap">
                        <div>
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                            {customer.firstName} {customer.lastName}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-300">{customer.userId}</p>
                        </div>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-gray-600 dark:text-gray-200">
                        {customer.email}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">
                        {getStatusBadges(customer) || (
                          <span className="text-xs text-green-600 bg-green-100 dark:text-green-400 dark:bg-green-900/30 px-2 py-1 rounded">Active</span>
                        )}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleViewDetails(customer)}
                            className="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded"
                            title="View Details"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleEditCustomer(customer)}
                            className="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded"
                            title="Edit Customer"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          {!customer.isLocked ? (
                            <button
                              onClick={() => handleLockAccount(customer.userId)}
                              disabled={actionInProgress === customer.userId}
                              className="p-2 text-orange-600 dark:text-orange-400 hover:bg-orange-50 dark:hover:bg-orange-900/30 rounded disabled:opacity-50"
                              title="Lock Account"
                            >
                              <Lock className="w-4 h-4" />
                            </button>
                          ) : (
                            <button
                              onClick={() => handleUnlockAccount(customer.userId)}
                              disabled={actionInProgress === customer.userId}
                              className="p-2 text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/30 rounded disabled:opacity-50"
                              title="Unlock Account"
                            >
                              <Unlock className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleDeactivateAccount(customer.userId)}
                            disabled={customer.isDeactivated || actionInProgress === customer.userId}
                            className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 rounded disabled:opacity-50"
                            title="Deactivate Account"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                          {customer.isDeactivated && (
                            <button
                              onClick={() => handleReactivateAccount(customer.userId)}
                              className="px-2 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700 dark:bg-green-700 dark:hover:bg-green-600"
                            >
                              Reactivate
                            </button>
                          )}
                          <button
                            onClick={() => handleViewAuditLogs(customer)}
                            className="p-2 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 rounded"
                            title="View Audit Logs"
                          >
                            <ChevronDown className="w-4 h-4" />
                          </button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </div>
      </div>

      {/* Modals */}
      {showDetailModal && (
        <CustomerDetailModal
          customer={selectedCustomer}
          keycloak={keycloak}
          onClose={() => setShowDetailModal(false)}
        />
      )}

      {showEditModal && (
        <CustomerEditModal
          customer={selectedCustomer}
          keycloak={keycloak}
          onClose={() => {
            setShowEditModal(false);
            fetchCustomers();
          }}
        />
      )}

      {showAuditModal && (
        <AuditLogModal
          customer={selectedCustomer}
          keycloak={keycloak}
          onClose={() => setShowAuditModal(false)}
        />
      )}
    </div>
  );
};

export default CustomerManagementView;
