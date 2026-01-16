import React, { useState, useEffect, useRef } from 'react';
import { ShoppingCart, User, Menu, X, LogOut, Bell, Moon, Sun } from 'lucide-react';
import { useDarkMode } from '../../hooks/useDarkMode';
import { useKeycloak } from '@react-keycloak/web';
import { API_BASE_URL } from '../../services/api';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Separator } from '../ui/separator';

const Header = ({ user, cartCount, navigate }) => {
  const { keycloak } = useKeycloak();
  const { isDark, toggleDarkMode } = useDarkMode();
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const notificationRef = useRef(null);

  // Check if user has administrator role
  const isAdmin = user?.roles?.includes('administrator') || false;
  // Check if user has content-manager role
  const isContentManager = user?.roles?.includes('content-manager') || false;
  // Check if user has warehouse staff role
  const isWarehouseStaff = user?.roles?.includes('warehouse-staff') || false;

  // Fetch notifications when user is authenticated
  useEffect(() => {
    if (user && keycloak.authenticated) {
      fetchNotifications();
      // Poll for new notifications every 30 seconds
      const interval = setInterval(fetchNotifications, 30000);
      return () => clearInterval(interval);
    }
  }, [user, keycloak.authenticated]);

  // Close notification dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const fetchNotifications = async () => {
    try {
      await keycloak.updateToken(5);
      const response = await fetch(`${API_BASE_URL}/notifications`, {
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setNotifications(data);
        setUnreadCount(data.filter(n => !n.read).length);
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    }
  };

  const markAsRead = async (notificationId) => {
    try {
      await keycloak.updateToken(5);
      await fetch(`${API_BASE_URL}/notifications/${notificationId}/read`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${keycloak.token}`,
          'Content-Type': 'application/json'
        }
      });
      fetchNotifications();
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  };

  const handleLogout = () => {
    keycloak.logout();
  };

  return (
    <header className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-8">
            <h1
              className="text-2xl font-bold cursor-pointer hover:opacity-80 transition bg-gradient-to-r from-primary to-primary/70 bg-clip-text text-transparent"
              onClick={() => navigate('/')}
            >
              ShopHub
            </h1>
            <nav className="hidden md:flex gap-1">
              <Button variant="ghost" size="sm" onClick={() => navigate('/products')}>
                Products
              </Button>
              <Button variant="ghost" size="sm" onClick={() => navigate('/pages')}>
                Pages
              </Button>
              {user && (
                <Button variant="ghost" size="sm" onClick={() => navigate('/orders')}>
                  My Orders
                </Button>
              )}
              {isAdmin && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/admin/products')}
              >
                Admin Dashboard
              </Button>
              )}
              {isContentManager && (
                <>
                  <Button variant="ghost" size="sm" onClick={() => navigate('/landing-pages')}>
                    Landing Pages
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => navigate('/blog-management')}>
                    Blog Management
                  </Button>
                </>
              )}
              {isWarehouseStaff && (
                <Button variant="ghost" size="sm" onClick={() => navigate('/warehouse/orders')}>
                  Warehouse
                </Button>
              )}
            </nav>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={toggleDarkMode}
              title={isDark ? "Switch to Light Mode" : "Switch to Dark Mode"}
            >
              {isDark ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </Button>

            {user && (
              <div className="relative flex items-center" ref={notificationRef}>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setShowNotifications(!showNotifications)}
                  className="relative"
                >
                  <Bell className="h-5 w-5" />
                  {unreadCount > 0 && (
                    <Badge variant="danger" className="absolute -top-1 -right-1 h-5 w-5 p-0 flex items-center justify-center text-xs">
                      {unreadCount}
                    </Badge>
                  )}
                </Button>

                {showNotifications && (
                  <div className="absolute top-full right-0 mt-2 w-80 bg-card rounded-lg shadow-lg border z-50 max-h-96 overflow-y-auto">
                    <div className="p-3 border-b">
                      <h3 className="font-semibold">Notifications</h3>
                    </div>
                    {notifications.length === 0 ? (
                      <div className="p-6 text-center text-muted-foreground">
                        <Bell className="h-8 w-8 mx-auto mb-2 opacity-50" />
                        <p className="text-sm">No notifications yet</p>
                      </div>
                    ) : (
                      <div className="divide-y">
                        {notifications.map((notification) => (
                          <div
                            key={notification.id}
                            className={`p-3 hover:bg-accent cursor-pointer transition-colors ${
                              !notification.read ? 'bg-accent/50' : ''
                            }`}
                            onClick={() => {
                              markAsRead(notification.id);
                              if (notification.order_id) {
                                navigate('/orders');
                                setShowNotifications(false);
                              }
                            }}
                          >
                            <div className="flex items-start gap-2">
                              <div className="flex-1">
                                <p className="text-sm font-medium">
                                  {notification.title}
                                </p>
                                <p className="text-xs text-muted-foreground mt-1">
                                  {notification.message}
                                </p>
                              </div>
                              {!notification.read && (
                                <div className="w-2 h-2 bg-primary rounded-full mt-1"></div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate('/cart')}
              className="relative"
            >
              <ShoppingCart className="h-5 w-5" />
              {cartCount > 0 && (
                <Badge variant="danger" className="absolute -top-1 -right-1 h-5 w-5 p-0 flex items-center justify-center text-xs">
                  {cartCount}
                </Badge>
              )}
            </Button>


            {user ? (
              <div className="hidden md:flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/profile')}
                  className="gap-2"
                >
                  <User className="h-4 w-4" />
                  <span className="text-sm">{user.name}</span>
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleLogout}
                  title="Logout"
                >
                  <LogOut className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => keycloak.login()}
                className="hidden md:flex gap-2"
              >
                <User className="h-4 w-4" />
                <span className="text-sm">Sign In</span>
              </Button>
            )}

            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={() => setShowMobileMenu(!showMobileMenu)}
            >
              {showMobileMenu ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </Button>
          </div>
        </div>

        {showMobileMenu && (
          <nav className="md:hidden mt-3 pt-3 border-t flex flex-col gap-1">
            <Button variant="ghost" className="justify-start" onClick={() => { navigate('/products'); setShowMobileMenu(false); }}>
              Products
            </Button>
            <Button variant="ghost" className="justify-start" onClick={() => { navigate('/pages'); setShowMobileMenu(false); }}>
              Pages
            </Button>
            {user && (
              <Button variant="ghost" className="justify-start" onClick={() => { navigate('/orders'); setShowMobileMenu(false); }}>
                My Orders
              </Button>
            )}
            {isAdmin && (
              <Button
                variant="ghost"
                className="justify-start"
                onClick={() => { navigate('/admin/products'); setShowMobileMenu(false); }}
              >
                Admin Dashboard
              </Button>
            )}
            {isContentManager && (
              <>
                <Button variant="ghost" className="justify-start" onClick={() => { navigate('/landing-pages'); setShowMobileMenu(false); }}>
                  Landing Pages
                </Button>
                <Button variant="ghost" className="justify-start" onClick={() => { navigate('/blog-management'); setShowMobileMenu(false); }}>
                  Blog Management
                </Button>
              </>
            )}
            {isWarehouseStaff && (
              <Button variant="ghost" className="justify-start" onClick={() => { navigate('/warehouse/orders'); setShowMobileMenu(false); }}>
                Warehouse
              </Button>
            )}
            <Separator className="my-2" />
            {!user && (
              <Button variant="ghost" className="justify-start" onClick={() => { keycloak.login(); setShowMobileMenu(false); }}>
                <User className="h-4 w-4 mr-2" />
                Sign In
              </Button>
            )}
            {user && (
              <Button variant="ghost" className="justify-start text-destructive" onClick={() => { handleLogout(); setShowMobileMenu(false); }}>
                Logout
              </Button>
            )}
          </nav>
        )}
      </div>
    </header>
  );
};

export default Header;
// DebugInfo removed - keep file simple for production
