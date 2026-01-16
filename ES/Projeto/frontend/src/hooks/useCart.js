import { useState, useEffect, useCallback } from 'react';
import { cartAPI } from '../services/api';

export const useCart = (user) => {
  const [cart, setCart] = useState([]);
  const [loading, setLoading] = useState(false);

  // Carregar carrinho quando user muda (usando sub como ID único)
  useEffect(() => {
    console.log('useCart: User changed:', user);
    if (user?.sub || user?.user_id) {
      console.log('useCart: Loading cart for user');
      loadCart();
    } else {
      console.log('useCart: No user, clearing cart');
      setCart([]);
    }
  }, [user?.sub, user?.user_id]); // Verificar ambos

  const loadCart = async () => {
    try {
      setLoading(true);
      const cartData = await cartAPI.getCart();
      console.log('Cart loaded:', cartData);
      setCart(cartData || []);
    } catch (error) {
      console.error('Failed to load cart:', error);
      setCart([]);
    } finally {
      setLoading(false);
    }
  };

  const addToCart = useCallback(async (product) => {
    console.log('=== ADD TO CART DEBUG ===');
    console.log('User object:', user);
    console.log('User.sub:', user?.sub);
    console.log('User.user_id:', user?.user_id);
    console.log('Product:', product);

    if (!user) {
      console.error('❌ No user object found');
      alert('Please login to add items to cart');
      return false;
    }

    if (!user.sub && !user.user_id) {
      console.error('❌ User object exists but no sub/user_id:', user);
      alert('Authentication error. Please logout and login again.');
      return false;
    }

    try {
      const productId = product.product_id || product.id;
      
      if (!productId) {
        console.error('❌ Product ID is missing:', product);
        alert('Invalid product');
        return false;
      }

      console.log('✅ Calling API to add product:', productId);
      
      // Fazer a chamada API
      const result = await cartAPI.addToCart(productId, 1);
      console.log('✅ API response:', result);
      
      // Recarregar o carrinho completo para garantir sincronização
      await loadCart();
      
      // Feedback visual
      alert('Product added to cart!');
      return true;
      
    } catch (error) {
      console.error('❌ Failed to add to cart:', error);
      alert('Failed to add product to cart: ' + error.message);
      return false;
    }
  }, [user, loadCart]); // user completo como dependência

  const removeFromCart = useCallback(async (cartId) => {
    try {
      console.log('Removing cart item:', cartId);
      await cartAPI.removeFromCart(cartId);
      setCart(prevCart => prevCart.filter(item => item.cart_id !== cartId));
    } catch (error) {
      console.error('Failed to remove from cart:', error);
      alert('Failed to remove item from cart');
    }
  }, []);

  const updateQuantity = useCallback(async (cartId, newQuantity) => {
    if (newQuantity === 0) {
      return removeFromCart(cartId);
    }
    
    try {
      console.log('Updating quantity:', cartId, newQuantity);
      const updated = await cartAPI.updateQuantity(cartId, newQuantity);
      console.log('Updated item:', updated);
      
      setCart(prevCart => prevCart.map(item =>
        item.cart_id === cartId ? { ...item, quantity: newQuantity } : item
      ));
    } catch (error) {
      console.error('Failed to update quantity:', error);
      alert('Failed to update quantity');
    }
  }, [removeFromCart]);

  const clearCart = useCallback(async () => {
    try {
      console.log('Clearing cart');
      await cartAPI.clearCart();
      setCart([]);
    } catch (error) {
      console.error('Failed to clear cart:', error);
      alert('Failed to clear cart');
    }
  }, []);

  const cartTotal = cart.reduce((sum, item) => sum + (item.unit_price * item.quantity), 0);
  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0);

  return {
    cart,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    loadCart, // Exportar para poder recarregar manualmente
    cartTotal,
    cartCount,
    loading
  };
};