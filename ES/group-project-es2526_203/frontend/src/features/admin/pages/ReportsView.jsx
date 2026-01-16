import React, { useEffect, useState } from 'react';
import { analyticsAPI, categoriesAPI } from '../../../services/api';
import { Card } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import AdminNavTabs from '../components/AdminNavTabs';

function ReportsView() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [data, setData] = useState(null);

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const cats = await categoriesAPI.getAll();
        setCategories(cats);
      } catch (e) {
        console.error(e);
      }
    };
    loadCategories();
  }, []);

  const fetchReport = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await analyticsAPI.getSales({ startDate, endDate, categoryId: categoryId || undefined });
      setData(res);
    } catch (e) {
      setError(e.message || 'Failed to load report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto">
      <AdminNavTabs />
      <Card>
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Sales Reports & Analytics</h2>
          <p className="mt-1 text-sm text-gray-600 dark:text-gray-300">Generate detailed sales analytics</p>
        </div>

        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <label className="block text-sm mb-1 text-gray-700 dark:text-gray-300">Start Date</label>
              <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} className="border border-gray-300 dark:border-gray-600 rounded px-3 py-2 w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
            <div>
              <label className="block text-sm mb-1 text-gray-700 dark:text-gray-300">End Date</label>
              <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} className="border border-gray-300 dark:border-gray-600 rounded px-3 py-2 w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
            <div>
              <label className="block text-sm mb-1 text-gray-700 dark:text-gray-300">Product Category</label>
              <select value={categoryId} onChange={e => setCategoryId(e.target.value)} className="border border-gray-300 dark:border-gray-600 rounded px-3 py-2 w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                <option value="">All</option>
                {categories.map(c => (
                  <option key={c.category_id || c.categoryId || c.name} value={c.category_id || c.categoryId}>{c.name}</option>
                ))}
              </select>
            </div>
            <div>
              <Button
                onClick={fetchReport}
                className="w-full"
              >
                Generate Report
              </Button>
            </div>
          </div>
        </div>

        <div className="p-6">
          {loading && <p className="text-gray-700 dark:text-gray-300">Loading...</p>}
          {error && <p className="text-red-600 dark:text-red-400">{error}</p>}

          {data && (
            <div className="space-y-6">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card className="p-4">
              <div className="text-sm text-muted-foreground">Revenue</div>
              <div className="text-2xl font-bold">{Number(data.revenue || 0).toFixed(2)} €</div>
            </Card>
            <Card className="p-4">
              <div className="text-sm text-muted-foreground">Total Orders</div>
              <div className="text-2xl font-bold">{data.totalOrders || 0}</div>
            </Card>
          </div>

          <div>
            <h2 className="text-xl font-semibold mb-3">Best-Selling Products (Top 3)</h2>
            <Card className="overflow-x-auto">
              <table className="min-w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="text-left p-3 text-gray-700 dark:text-gray-300 font-semibold">Product</th>
                    <th className="text-right p-3 text-gray-700 dark:text-gray-300 font-semibold">Quantity</th>
                    <th className="text-right p-3 text-gray-700 dark:text-gray-300 font-semibold">Sales</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.bestSellingProducts || []).length === 0 ? (
                    <tr>
                      <td colSpan="3" className="p-4 text-center text-gray-500 dark:text-gray-400">No products sold in this period</td>
                    </tr>
                  ) : (
                    (data.bestSellingProducts || []).map((p) => (
                      <tr key={p.productId} className="border-t border-gray-200 dark:border-gray-700">
                        <td className="p-3 text-gray-900 dark:text-gray-100">{p.name}</td>
                        <td className="p-3 text-right text-gray-900 dark:text-gray-100">{p.quantity}</td>
                        <td className="p-3 text-right text-gray-900 dark:text-gray-100">${Number(p.sales || 0).toFixed(2)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </Card>
          </div>

          <div>
            <h2 className="text-xl font-semibold mb-3">All Sales</h2>
            <Card className="overflow-x-auto">
              <table className="min-w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="text-left p-3 text-gray-700 dark:text-gray-300 font-semibold">Product</th>
                    <th className="text-right p-3 text-gray-700 dark:text-gray-300 font-semibold">Quantity</th>
                    <th className="text-right p-3 text-gray-700 dark:text-gray-300 font-semibold">Sales</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.allSalesProducts || []).length === 0 ? (
                    <tr>
                      <td colSpan="3" className="p-4 text-center text-gray-500 dark:text-gray-400">No products sold in this period</td>
                    </tr>
                  ) : (
                    (data.allSalesProducts || []).map((p) => (
                      <tr key={p.productId} className="border-t border-gray-200 dark:border-gray-700">
                        <td className="p-3 text-gray-900 dark:text-gray-100">{p.name}</td>
                        <td className="p-3 text-right text-gray-900 dark:text-gray-100">{p.quantity}</td>
                        <td className="p-3 text-right text-gray-900 dark:text-gray-100">{Number(p.sales || 0).toFixed(2)} €</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </Card>
          </div>

          <div>
            <h2 className="text-xl font-semibold mb-3">Customer Demographics (by City)</h2>
            <Card className="overflow-x-auto">
              <table className="min-w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="text-left p-3 text-gray-700 dark:text-gray-300 font-semibold">City</th>
                    <th className="text-right p-3 text-gray-700 dark:text-gray-300 font-semibold">Orders</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.customerDemographics || []).length === 0 ? (
                    <tr>
                      <td colSpan="2" className="p-4 text-center text-gray-500 dark:text-gray-400">No customer data available</td>
                    </tr>
                  ) : (
                    (data.customerDemographics || []).map((c, idx) => (
                      <tr key={idx} className="border-t border-gray-200 dark:border-gray-700">
                        <td className="p-3 text-gray-900 dark:text-gray-100">{c.city || 'Unknown'}</td>
                        <td className="p-3 text-right text-gray-900 dark:text-gray-100">{c.orders}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </Card>
          </div>
         </div>
          )}
        </div>
      </Card>
    </div>
  );
}

export default ReportsView;
