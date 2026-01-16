import React, { useEffect, useState } from 'react';
import { Button } from '../../../components/ui/button';

const HeroSection = ({ onShopNow, banners = [] }) => {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (!Array.isArray(banners) || banners.length <= 1) return;
    const interval = setInterval(() => {
      setIndex(prev => (prev + 1) % banners.length);
    }, 7000);
    return () => clearInterval(interval);
  }, [banners]);

  if (!Array.isArray(banners) || banners.length === 0) {
    return (
      <section className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 rounded-xl p-8 md:p-12">
        <div className="max-w-3xl">
          <h2 className="text-4xl font-bold text-gray-900 dark:text-gray-100 mb-4">Welcome to ShopHub</h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 mb-6">Discover amazing products at unbeatable prices</p>
          <Button onClick={() => onShopNow && onShopNow(null)} size="lg">Shop Now</Button>
        </div>
      </section>
    );
  }

  const trackStyle = {
    transform: `translateX(-${index * 100}%)`,
    transition: 'transform 0.8s ease-in-out'
  };

  return (
    <section className="rounded-xl overflow-hidden shadow-md bg-white dark:bg-gray-800 relative">
      <div className="w-full h-64 md:h-96 overflow-hidden">
        <div className="flex w-full h-full carousel-track" style={trackStyle}>
          {banners.map((b, i) => {
            let img = null;
            let btnCat = null;
            try {
              if (b.metadata) {
                const parsed = typeof b.metadata === 'string' ? JSON.parse(b.metadata) : b.metadata;
                img = parsed?.image_url || null;
                btnCat = parsed?.button_category || null;
              }
            } catch (e) {
              img = null;
            }

            return (
              <div key={i} className="min-w-full h-full relative carousel-slide">
                {img ? (
                  <img src={img} alt={b.title} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full bg-gradient-to-r from-blue-50 to-indigo-50" />
                )}

                <div className="absolute left-4 bottom-4 md:left-12 md:bottom-6 text-white">
                  <h2 className="text-2xl md:text-3xl font-bold drop-shadow-md">{b.title}</h2>
                  <p className="mt-1 text-sm md:text-lg drop-shadow-md">{b.description}</p>
                  <div className="mt-3">
                    <Button onClick={() => onShopNow(btnCat)} size="lg">Shop Now</Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* indicators bottom-center */}
        {banners.length > 1 && (
          <div className="absolute left-0 right-0 bottom-2 flex justify-center items-center space-x-2">
            {banners.map((_, i) => (
              <button
                key={i}
                onClick={() => setIndex(i)}
                className={`w-3 h-3 rounded-full ${i === index ? 'bg-white dark:bg-white/80' : 'bg-white/60 dark:bg-white/40'} border border-white/40`}
                aria-label={`Go to slide ${i + 1}`}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
};

export default HeroSection;