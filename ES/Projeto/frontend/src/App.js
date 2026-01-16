import React, { useState, useEffect } from 'react'; 
import { Routes, Route, useNavigate } from 'react-router-dom';
import Header from './components/layout/Header'; 
import Footer from './components/layout/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import CartView from './features/cart/pages/CartView';
import CheckoutView from './features/checkout/pages/CheckoutView';
import LoginView from './features/auth/pages/LoginView';
import ProductDetailView from './features/products/pages/ProductDetailView';
import ProductsView from './features/products/pages/ProductsView';
import OrderConfirmationView from './features/orders/pages/OrderConfirmationView';
import OrdersView from './features/orders/pages/OrdersView';
import OrderTrackingView from './features/orders/pages/OrderTrackingView';
import WarehouseOrdersView from './features/warehouse/pages/WarehouseOrdersView';
import ProfileView from './features/profile/pages/ProfileView';
import ShippingBillingView from './features/profile/pages/ShippingBillingView';
import ProductManagementView from './features/admin/pages/ProductManagementView';
import OrderManagementView from './features/admin/pages/OrderManagementView';
import CustomerManagementView from './features/admin/pages/CustomerManagementView';
import InventoryManagementView from './features/admin/pages/InventoryManagementView'; 
import ReportsView from './features/admin/pages/ReportsView.jsx';
import HomeView from './features/home/pages/HomeView';
import PagesView from './features/pages/pages/PagesView';
import PageDetailView from './features/pages/pages/PageDetailView';
import LandingPagesView from './features/pages/pages/LandingPagesView';
import CreateLandingPageView from './features/pages/pages/CreateLandingPageView';
import EditLandingPageView from './features/pages/pages/EditLandingPageView';
import LandingPageDetailView from './features/pages/pages/LandingPageDetailView';
import BannersView from './features/banners/pages/BannersView';
import CreateBannerView from './features/banners/pages/CreateBannerView';
import BannerDetailView from './features/banners/pages/BannerDetailView';
import AdminBlogManagementView from './features/blog/pages/AdminBlogManagementView';
import BlogPostEditorView from './features/blog/pages/BlogPostEditorView';
import { useAuth } from './hooks/useAuth';
import { useCart } from './hooks/useCart';
import { useProducts } from './hooks/useProducts';
import { ordersAPI, cartAPI, categoriesAPI } from './services/api';
import ChatWidget from './features/chatbot/ChatWidget';

function App() {
  const navigate = useNavigate();
  
  const auth = useAuth(navigate);
  const cart = useCart(auth.user);
  const productState = useProducts();
  const [categories, setCategories] = useState(['All']);

  // Fetch categories from API
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await categoriesAPI.getAll();
        const categoryNames = ['All', ...data.map(cat => cat.name)];
        setCategories(categoryNames);
      } catch (err) {
        console.error('Failed to fetch categories:', err);
        // Fallback to default categories if API fails
        setCategories(['All', 'Electronics', 'Clothing', 'Home & Garden', 'Books', 'Sports']);
      }
    };

    fetchCategories();
  }, []);

  const handleCheckout = () => {
    if (cart.cart.length === 0) {
      alert('Your cart is empty!');
      return;
    }
    if (!auth.user) {
      alert('Please sign in to continue with checkout');
      navigate('/login');
      return;
    }
    navigate('/checkout');
  };

  const completeOrder = async (shippingInfo) => {
    auth.setError('');

    console.log('Cart state:', cart.cart);
    console.log('Cart items:', cart.cart.length);

    try {
      const orderData = {
        items: cart.cart.map(item => ({
          product_id: item.product_id,
          quantity: item.quantity,
          price: item.unit_price,
          product_name: item.product_name
        })),
        total: cart.cartTotal,
        shipping: {
          cost: 0,
          address: shippingInfo
        }
      };

      console.log('Sending order data:', orderData);

      const order = await ordersAPI.createOrder(orderData);
      auth.setOrderHistory([order, ...auth.orderHistory]);
      await cartAPI.clearCart();
      cart.clearCart();
      navigate('/order-confirmation');
    } catch (err) {
      console.error('Order error:', err);
      auth.setError(err.message);
      alert('Failed to place order: ' + err.message);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header user={auth.user} cartCount={cart.cartCount} navigate={navigate} />

      <main className="flex-grow max-w-7xl mx-auto px-4 py-8 w-full">
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={
            <HomeView
              PRODUCTS={productState.products}
              CATEGORIES={categories}
              setSelectedCategory={productState.setSelectedCategory}
              setSelectedProduct={productState.setSelectedProduct}
              navigate={navigate}
              addToCart={cart.addToCart}
              loading={productState.loading}
              error={productState.error}
            />
          } />
          
          <Route path="/products" element={
            <ProductsView
              searchQuery={productState.searchQuery}
              setSearchQuery={productState.setSearchQuery}
              showFilters={productState.showFilters}
              setShowFilters={productState.setShowFilters}
              CATEGORIES={categories}
              selectedCategory={productState.selectedCategory}
              setSelectedCategory={productState.setSelectedCategory}
              priceRange={productState.priceRange}
              setPriceRange={productState.setPriceRange}
              filteredProducts={productState.filteredProducts}
              setSelectedProduct={productState.setSelectedProduct}
              navigate={navigate}
              addToCart={cart.addToCart}
              loading={productState.loading}
              error={productState.error}
            />
          } />
          
          <Route path="/productDetail" element={
            <ProductDetailView
              selectedProduct={productState.selectedProduct}
              addToCart={cart.addToCart}
              navigate={navigate}
            />
          } />
          
          <Route path="/login" element={
            <LoginView
              error={auth.error}
              loading={auth.loading}
              handleRegister={auth.handleRegister}
              handleLogin={auth.handleLogin}
              setError={auth.setError}
            />
          } />

          {/* Public Blog/Pages Routes */}
          <Route path="/pages" element={<PagesView />} />
          <Route path="/pages/:slug" element={<PageDetailView />} />
          
          {/* Landing Pages Routes */}
          <Route path="/landing-pages" element={
            <ProtectedRoute roles={['content-manager']}>
              <LandingPagesView />
            </ProtectedRoute>
          } />
          <Route path="/landing-pages/create" element={
            <ProtectedRoute roles={['content-manager']}>
              <CreateLandingPageView />
            </ProtectedRoute>
          } />
          <Route path="/landing-pages/:id" element={<LandingPageDetailView />} />
          <Route path="/landing-pages/:id/edit" element={
            <ProtectedRoute roles={['content-manager']}>
              <EditLandingPageView />
            </ProtectedRoute>
          } />

          {/* Banners Routes */}
          <Route path="/banners" element={
            <ProtectedRoute roles={["content-manager"]}>
              <BannersView />
            </ProtectedRoute>
          } />
          <Route path="/banners/create" element={
            <ProtectedRoute roles={["content-manager"]}>
              <CreateBannerView />
            </ProtectedRoute>
          } />
          <Route path="/banners/:id" element={<BannerDetailView />} />
          <Route path="/banners/:id/edit" element={
            <ProtectedRoute roles={["content-manager"]}>
              <BannerDetailView />
            </ProtectedRoute>
          } />

          {/* Protected Routes - Require authentication only */}
          <Route path="/cart" element={
            <ProtectedRoute>
              <CartView
                cart={cart.cart}
                cartTotal={cart.cartTotal}
                updateQuantity={cart.updateQuantity}
                removeFromCart={cart.removeFromCart}
                navigate={navigate}
              />
            </ProtectedRoute>
          } />
          
          <Route path="/order-confirmation" element={
            <ProtectedRoute>
              <OrderConfirmationView navigate={navigate} />
            </ProtectedRoute>
          } />
          
          <Route path="/profile" element={
            <ProtectedRoute>
              <ProfileView
                user={auth.user}
                orderHistory={auth.orderHistory}
                handleLogout={auth.handleLogout}
              />
            </ProtectedRoute>
          } />

          <Route path="/profile/shipping-billing" element={
            <ProtectedRoute>
              <ShippingBillingView user={auth.user} />
            </ProtectedRoute>
          } />

          {/* Admin Routes */}
          <Route path="/admin/orders" element={
            <ProtectedRoute roles={['administrator']}>
              <OrderManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/customers" element={
            <ProtectedRoute roles={['administrator']}>
              <CustomerManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/products" element={
            <ProtectedRoute roles={['administrator']}>
              <ProductManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/inventory" element={
            <ProtectedRoute roles={['administrator']}>
              <InventoryManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/reports" element={
            <ProtectedRoute roles={['administrator']}>
              <ReportsView />
            </ProtectedRoute>
          } />

          {/* Blog Management Routes - Content Manager Only */}
          <Route path="/blog-management" element={
            <ProtectedRoute roles={['content-manager']}>
              <AdminBlogManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/blog" element={
            <ProtectedRoute roles={['content-manager']}>
              <AdminBlogManagementView />
            </ProtectedRoute>
          } />

          <Route path="/admin/blog/new" element={
            <ProtectedRoute roles={['content-manager']}>
              <BlogPostEditorView />
            </ProtectedRoute>
          } />

          <Route path="/admin/blog/edit/:id" element={
            <ProtectedRoute roles={['content-manager']}>
              <BlogPostEditorView />
            </ProtectedRoute>
          } />
          
          {/* Protected Routes - Require specific roles */}
          <Route path="/checkout" element={
            <ProtectedRoute roles={['customer', 'administrator']}>
              <CheckoutView
                user={auth.user}
                cart={cart.cart}
                cartTotal={cart.cartTotal}
                error={auth.error}
                loading={auth.loading}
                completeOrder={completeOrder}
              />
            </ProtectedRoute>
          } />

          <Route path="/warehouse/orders" element={
            <ProtectedRoute roles={['warehouse-staff']}>
              <WarehouseOrdersView />
            </ProtectedRoute>
          } />
          
          <Route path="/orders" element={
            <ProtectedRoute roles={['customer', 'administrator', 'warehouse-staff']}>
              <OrdersView orderHistory={auth.orderHistory} />
            </ProtectedRoute>
          } />

          <Route path="/orders/:orderId/track" element={
            <ProtectedRoute roles={['customer', 'administrator', 'warehouse-staff']}>
              <OrderTrackingView />
            </ProtectedRoute>
          } />
        </Routes>
      </main>

      <Footer />

      {/* Chatbot Widget */}
      <ChatWidget />
    </div>
  );
}

export default App;