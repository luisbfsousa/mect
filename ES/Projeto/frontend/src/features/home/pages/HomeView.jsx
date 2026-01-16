import React, { useEffect, useState } from 'react';
import HeroSection from '../components/HeroSection';
import FeaturedProducts from '../components/FeaturedProducts';
import CategoryCard from '../components/CategoryCard';
import api from '../../../services/api';

const HomeView = ({
  PRODUCTS,
  CATEGORIES,
  setSelectedCategory,
  setSelectedProduct,
  navigate,
  addToCart,
  loading,
  error
}) => {
  const handleViewDetails = (product) => {
    setSelectedProduct(product);
    navigate('/productDetail');
  };

  const handleAddToCart = (product) => {
    addToCart(product);
  };

  const handleCategoryClick = (category) => {
    setSelectedCategory(category);
    navigate('/products');
  };

  const handleShopNow = (category) => {
    if (category && category !== 'none') {
      const normalize = (cat) => {
        if (cat && typeof cat === 'object') {
          if (cat.name) return cat.name;
          if (cat.categoryId) return String(cat.categoryId);
          if (cat.id) return String(cat.id);
          if (cat.slug) return cat.slug;
        }

        if (!cat) return cat;
        if (!Array.isArray(CATEGORIES)) return cat;
        const found = CATEGORIES.find(c => {
          if (typeof c === 'string') return c === cat;
          return (c.categoryId && String(c.categoryId) === String(cat)) || c.name === cat || c.name === String(cat);
        });
        if (!found) return cat;
        return typeof found === 'string' ? found : (found.name || String(found.categoryId));
      };

      const normalized = normalize(category);
      if (typeof setSelectedCategory === 'function') {
        setSelectedCategory(normalized);
      }
      navigate(`/products?category=${encodeURIComponent(normalized)}`);
    } else {
      if (typeof setSelectedCategory === 'function') {
        setSelectedCategory('All');
      }
      navigate('/products');
    }
  };

  const [bannerPages, setBannerPages] = useState([]);

  useEffect(() => {
    const fetchBanner = async () => {
      try {
        const banners = await api.banners.getActive();
        if (Array.isArray(banners) && banners.length > 0) {
          setBannerPages(banners);
          return;
        }

        const pages = await api.pages.getPublished();
        const now = new Date();
        const publishedBanners = Array.isArray(pages)
          ? pages.filter(p => {
              if (!p.isPublished) return false;
              if (p.startDate && new Date(p.startDate) > now) return false;
              if (p.endDate && new Date(p.endDate) < now) return false;
              return true;
            })
          : [];
        setBannerPages(publishedBanners);
      } catch (e) {
        setBannerPages([]);
      }
    };
    fetchBanner();
  }, []);

  const displayCategories = CATEGORIES.filter(c => c !== 'All');

  return (
    <div className="space-y-12">
  <HeroSection onShopNow={handleShopNow} banners={bannerPages} />
      
      {loading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
          <p className="mt-4 text-muted-foreground">Loading products...</p>
        </div>
      ) : error ? (
        <div className="bg-destructive/10 border border-destructive/20 text-destructive px-4 py-3 rounded-lg">
          {error}
        </div>
      ) : (
        <FeaturedProducts
          products={PRODUCTS}
          onViewDetails={handleViewDetails}
          onAddToCart={handleAddToCart}
        />
      )}
      
      <section>
        <h3 className="text-2xl font-bold mb-6">Shop by Category</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {displayCategories.map(category => (
            <CategoryCard
              key={category}
              category={category}
              onClick={() => handleCategoryClick(category)}
            />
          ))}
        </div>
      </section>
    </div>
  );
};

export default HomeView;
