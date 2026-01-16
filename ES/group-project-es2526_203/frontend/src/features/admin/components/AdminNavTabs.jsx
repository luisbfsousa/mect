import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Package, ClipboardList, BarChart3, FileText, Users } from 'lucide-react';
import { Card } from '../../../components/ui/card';

const AdminNavTabs = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <Card className="mb-4">
      <div className="flex border-b">
        <button
          onClick={() => navigate('/admin/products')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold transition ${
            isActive('/admin/products')
              ? 'text-primary border-b-2 border-primary bg-accent'
              : 'text-muted-foreground hover:text-primary hover:bg-accent'
          }`}
        >
          <Package className="h-4 w-4" />
          Products
        </button>

        <button
          onClick={() => navigate('/admin/orders')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold transition ${
            isActive('/admin/orders')
              ? 'text-primary border-b-2 border-primary bg-accent'
              : 'text-muted-foreground hover:text-primary hover:bg-accent'
          }`}
        >
          <ClipboardList className="h-4 w-4" />
          Orders
        </button>

        <button
          onClick={() => navigate('/admin/inventory')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold transition ${
            isActive('/admin/inventory')
              ? 'text-primary border-b-2 border-primary bg-accent'
              : 'text-muted-foreground hover:text-primary hover:bg-accent'
          }`}
        >
          <BarChart3 className="h-4 w-4" />
          Inventory
        </button>

        <button
          onClick={() => navigate('/admin/reports')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold transition ${
            isActive('/admin/reports')
              ? 'text-primary border-b-2 border-primary bg-accent'
              : 'text-muted-foreground hover:text-primary hover:bg-accent'
          }`}
        >
          <FileText className="h-4 w-4" />
          Reports
        </button>

        <button
          onClick={() => navigate('/admin/customers')}
          className={`flex items-center gap-2 px-6 py-3 font-semibold transition ${
            isActive('/admin/customers')
              ? 'text-primary border-b-2 border-primary bg-accent'
              : 'text-muted-foreground hover:text-primary hover:bg-accent'
          }`}
        >
          <Users className="h-4 w-4" />
          Customers
        </button>
      </div>
    </Card>
  );
};

export default AdminNavTabs;
