import keycloak from '../config/keycloak'; 

// Default to a relative path so the frontend talks to the same host that served it.
// Override with REACT_APP_API_URL when needed (e.g. pointing to a remote API during development).
export const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';
const API_URL = API_BASE_URL;

// Helper to get auth headers with token refresh
const getAuthHeaders = async () => {
  const headers = { 'Content-Type': 'application/json' };
  
  if (keycloak.authenticated) {
    try {
      console.log('🔄 Checking token validity...');
      console.log('Token expires in:', keycloak.tokenParsed?.exp - Math.floor(Date.now() / 1000), 'seconds');
      
      const refreshed = await keycloak.updateToken(60);
      
      if (refreshed) {
        console.log('✅ Token refreshed successfully');
        console.log('New token expires at:', new Date(keycloak.tokenParsed.exp * 1000));
      } else {
        console.log('ℹ️ Token still valid, no refresh needed');
      }
      
      if (!keycloak.token) {
        console.error('❌ No token available after refresh attempt');
        throw new Error('No token available');
      }
      
      const tokenPreview = keycloak.token.substring(0, 20) + '...' + keycloak.token.substring(keycloak.token.length - 20);
      console.log('📝 Token preview:', tokenPreview);
      
      console.log('Token claims:', {
        sub: keycloak.tokenParsed?.sub,
        email: keycloak.tokenParsed?.email,
        exp: keycloak.tokenParsed?.exp,
        iat: keycloak.tokenParsed?.iat,
        azp: keycloak.tokenParsed?.azp,
        realm_roles: keycloak.tokenParsed?.realm_access?.roles
      });
      
      headers['Authorization'] = `Bearer ${keycloak.token}`;
      console.log('✅ Authorization header set');
      
    } catch (error) {
      console.error('❌ Token refresh failed:', error);
      console.log('🔄 Attempting to re-authenticate...');
      keycloak.login();
      throw new Error('Authentication failed - redirecting to login');
    }
  } else {
    console.warn('⚠️ User not authenticated');
    throw new Error('User not authenticated');
  }
  
  return headers;
};

// Public API call function (no authentication required)
const publicApiCall = async (endpoint, options = {}) => {
  try {
    console.log(`📡 Public API Call: ${options.method || 'GET'} ${endpoint} -> base ${API_URL}`);
    
    const headers = { 'Content-Type': 'application/json' };
    
    const config = {
      ...options,
      headers: {
        ...headers,
        ...options.headers,
      },
    };

    const fullUrl = `${API_URL}${endpoint}`;
    console.log('Full URL:', fullUrl);

    const response = await fetch(fullUrl, config);
    
    console.log(`📥 Response status: ${response.status} ${response.statusText}`);

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Unknown error' }));
      console.error('❌ API Error:', error);
      throw new Error(error.error || `HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();
    console.log('✅ Public API Response received');
    return data;
    
  } catch (error) {
    console.error('❌ Public API call failed:', error);
    throw error;
  }
};

// Generic API call function (requires authentication)
const apiCall = async (endpoint, options = {}) => {
  try {
    console.log(`📡 API Call: ${options.method || 'GET'} ${endpoint}`);
    console.log('Request body:', options.body);
    
    const headers = await getAuthHeaders();
    
    const config = {
      ...options,
      headers: {
        ...headers,
        ...options.headers,
      },
    };

    const fullUrl = `${API_URL}${endpoint}`;
    console.log('Full URL:', fullUrl); 

    const response = await fetch(fullUrl, config);
    
    console.log(`📥 Response status: ${response.status} ${response.statusText}`);
    
    console.log('Response headers:', {
      'content-type': response.headers.get('content-type'),
      'www-authenticate': response.headers.get('www-authenticate')
    });
    
    if (response.status === 401) {
      console.error('❌ 401 Unauthorized - Token invalid or expired');
      
      try {
        await keycloak.updateToken(-1);
        console.log('🔄 Token force refreshed, please retry');
        throw new Error('Token refreshed - please retry your action');
      } catch (refreshError) {
        console.error('Failed to refresh token, redirecting to login');
        keycloak.login();
        throw new Error('Session expired - redirecting to login');
      }
    }

    if (response.status === 403) {
      console.error('❌ 403 Forbidden - Action not allowed');
      
      // Try to parse backend error shape { error, message, status, timestamp }
      let backendMessage = 'Forbidden';
      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('application/json')) {
        const errorJson = await response.json().catch(() => null);
        if (errorJson?.message) backendMessage = errorJson.message;
      } else {
        const text = await response.text().catch(() => '');
        if (text) backendMessage = text;
      }

      // Surface a friendly message when account is locked/deactivated
      const normalized = backendMessage.toLowerCase();
      if (normalized.includes('locked')) {
        throw new Error('Your account is locked');
      }
      if (normalized.includes('deactivated')) {
        throw new Error('Your account is deactivated');
      }
      
      throw new Error(backendMessage || 'Forbidden');
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Unknown error' }));
      console.error('❌ API Error:', error);
      throw new Error(error.error || `HTTP ${response.status}: ${response.statusText}`);
    }

    // Handle 204 No Content - don't try to parse JSON
    if (response.status === 204) {
      console.log('✅ API Response: 204 No Content');
      return null;
    }

    const data = await response.json();
    console.log('✅ API Response received');
    return data;
    
  } catch (error) {
    console.error('❌ API call failed:', error);
    throw error;
  }
};

// Products API
export const productsAPI = {
  getAll: async () => {
    return publicApiCall('/products');
  },
  
  getById: async (id) => {
    return publicApiCall(`/products/${id}`);
  },
  
  search: async (query) => {
    const products = await publicApiCall('/products');
    return products.filter(p => 
      p.name.toLowerCase().includes(query.toLowerCase()) ||
      p.description.toLowerCase().includes(query.toLowerCase())
    );
  },

  fetchProducts: async () => {
    return publicApiCall('/products');
  },

  createProduct: async (productData) => {
    return apiCall('/products', {
      method: 'POST',
      body: JSON.stringify(productData)
    });
  },

  updateProduct: async (id, productData) => {
    return apiCall(`/products/${id}`, {
      method: 'PUT',
      body: JSON.stringify(productData)
    });
  },

  deleteProduct: async (id) => {
    return apiCall(`/products/${id}`, {
      method: 'DELETE'
    });
  },
};

// Categories API
export const categoriesAPI = {
  getAll: async () => {
    return publicApiCall('/categories');
  },
};

export const analyticsAPI = {
  async getSales({ startDate, endDate, categoryId } = {}) {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (categoryId) params.append('categoryId', categoryId);
    return apiCall(`/admin/analytics/sales?${params.toString()}`);
  }
};

// Blog API
export const blogAPI = {
  getPublishedPosts: async () => {
    return publicApiCall('/blog/posts');
  },
  
  getPostById: async (id) => {
    return publicApiCall(`/blog/posts/${id}`);
  },
  
  getAllPosts: async () => {
    return apiCall('/blog/admin/posts');
  },
  
  createPost: async (postData) => {
    return apiCall('/blog/admin/posts', {
      method: 'POST',
      body: JSON.stringify(postData)
    });
  },
  
  updatePost: async (id, postData) => {
    return apiCall(`/blog/admin/posts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(postData)
    });
  },
  
  deletePost: async (id) => {
    return apiCall(`/blog/admin/posts/${id}`, {
      method: 'DELETE'
    });
  },
};

// Reviews API
export const reviewsAPI = {
  getByProductId: async (productId) => {
    return publicApiCall(`/reviews/product/${productId}`);
  },

  getStats: async (productId) => {
    return publicApiCall(`/reviews/product/${productId}/stats`);
  },

  create: async (reviewData) => {
    return apiCall('/reviews', {
      method: 'POST',
      body: JSON.stringify(reviewData)
    });
  },

  update: async (reviewId, reviewData) => {
    return apiCall(`/reviews/${reviewId}`, {
      method: 'PUT',
      body: JSON.stringify(reviewData)
    });
  },

  delete: async (reviewId) => {
    return apiCall(`/reviews/${reviewId}`, {
      method: 'DELETE'
    });
  }
};

// Cart API
export const cartAPI = {
  getCart: async () => {
    return apiCall('/cart');
  },

  addToCart: async (productId, quantity) => {
    console.log('Adding to cart:', { productId, quantity });
    return apiCall('/cart', {
      method: 'POST',
      body: JSON.stringify({ product_id: productId, quantity })
    });
  },

  updateQuantity: async (cartId, quantity) => {
    console.log('🔄 Updating cart quantity:', { cartId, quantity });
    return apiCall(`/cart/${cartId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity })
    });
  },

  removeFromCart: async (cartId) => {
    console.log('🗑️ Removing from cart:', cartId);
    return apiCall(`/cart/${cartId}`, {
      method: 'DELETE'
    });
  },

  clearCart: async () => {
    console.log('🧹 Clearing cart');
    return apiCall('/cart', {
      method: 'DELETE'
    });
  }
};

// Orders API
export const ordersAPI = {
  fetchOrders: async () => {
    return apiCall('/orders');
  },

  getById: async (id) => {
    return apiCall(`/orders/${id}`);
  },
  
  createOrder: async (orderData) => {
    return apiCall('/orders', {
      method: 'POST',
      body: JSON.stringify(orderData)
    });
  },

  updateStatus: async (orderId, status) => {
    return apiCall(`/orders/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
  },
};

// Auth API
export const authAPI = {
  register: async () => {
    keycloak.register();
    return Promise.resolve({ redirected: true });
  },

  login: async () => {
    keycloak.login();
    return Promise.resolve({ redirected: true });
  }
};

// Profile API
export const profileAPI = {
  getProfile: async () => {
    return apiCall('/profile');
  },

  updateProfile: async (profileData) => {
    return apiCall('/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData)
    });
  },

  getAddresses: async () => {
    return apiCall('/profile/addresses');
  },

  saveAddress: async (addressData) => {
    return apiCall('/profile/addresses', {
      method: 'POST',
      body: JSON.stringify(addressData)
    });
  },

  fetchShippingBilling: async () => {
    const data = await apiCall('/profile/shipping-billing');
    
    return {
      shipping: data.shipping || {
        fullName: '',
        address: '',
        city: '',
        postalCode: '',
        phone: ''
      },
      billing: data.billing || {
        fullName: '',
        address: '',
        city: '',
        postalCode: '',
        phone: ''
      }
    };
  },

  updateShippingBilling: async (shippingInfo, billingInfo) => {
    return apiCall('/profile/shipping-billing', {
      method: 'PUT',
      body: JSON.stringify({ shipping: shippingInfo, billing: billingInfo })
    });
  }
};

const api = {
  products: productsAPI,
  categories: categoriesAPI,
  cart: cartAPI,
  orders: ordersAPI,
  auth: authAPI,
  profile: profileAPI,
  blog: blogAPI,
  reviews: reviewsAPI
};

export default api;

// Pages / Landing Pages API
export const pagesAPI = {
  // Public reads (no auth required on backend)
  getAll: async () => {
    return apiCall('/v1/landing-pages');
  },

  // Public: get published landing pages (no auth required)
  getPublished: async () => {
    return publicApiCall('/v1/landing-pages/published');
  },

  getById: async (id) => {
    return apiCall(`/v1/landing-pages/${id}`);
  },

  // Mutations require authentication
  create: async (pageData) => {
    return apiCall('/v1/landing-pages', {
      method: 'POST',
      body: JSON.stringify(pageData)
    });
  },

  update: async (id, pageData) => {
    return apiCall(`/v1/landing-pages/${id}`, {
      method: 'PUT',
      body: JSON.stringify(pageData)
    });
  },

  publish: async (id) => {
    return apiCall(`/v1/landing-pages/${id}/publish`, {
      method: 'PUT'
    });
  },

  unpublish: async (id) => {
    return apiCall(`/v1/landing-pages/${id}/unpublish`, {
      method: 'PUT'
    });
  },

  delete: async (id) => {
    return apiCall(`/v1/landing-pages/${id}`, {
      method: 'DELETE'
    });
  }
};

// Also expose via default export map for convenience
api.pages = pagesAPI;

// Banners API
export const bannersAPI = {
  // Public: get currently active banners (respecting schedule and published flag)
  getActive: async () => {
    return publicApiCall('/v1/banners/active');
  },

  // Admin: list all banners
  getAll: async () => {
    return apiCall('/v1/banners');
  },

  getById: async (id) => {
    return apiCall(`/v1/banners/${id}`);
  },

  create: async (bannerData) => {
    return apiCall('/v1/banners', {
      method: 'POST',
      body: JSON.stringify(bannerData)
    });
  },

  update: async (id, bannerData) => {
    return apiCall(`/v1/banners/${id}`, {
      method: 'PUT',
      body: JSON.stringify(bannerData)
    });
  },

  publish: async (id) => {
    return apiCall(`/v1/banners/${id}/publish`, {
      method: 'PUT'
    });
  },

  delete: async (id) => {
    return apiCall(`/v1/banners/${id}`, {
      method: 'DELETE'
    });
  }
};

api.banners = bannersAPI;
