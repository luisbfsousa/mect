import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { Package } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';
import StarRating from '../components/StarRating';
import ProductPriceCard from '../components/ProductPriceCard';
import ReviewsSection from '../components/ReviewsSection';
import { reviewsAPI } from '../../../services/api';

const ProductDetailView = ({
  selectedProduct,
  addToCart,
  navigate
}) => {
  const { keycloak } = useKeycloak();
  const [adding, setAdding] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [loadingReviews, setLoadingReviews] = useState(true);
  const [reviewsError, setReviewsError] = useState(null);
  const canFetchReviews = true;
  const canSubmitReviews = true;
  const canDisplayReviews = true;
  const shouldModerate = false;

  // Get authenticated user info
  const currentUser = keycloak.authenticated ? {
    username: keycloak.tokenParsed?.preferred_username,
    roles: keycloak.tokenParsed?.realm_access?.roles || [],
  } : null;

  // Fetch reviews when product changes
  useEffect(() => {
    const fetchReviews = async () => {
      if (!selectedProduct?.id || !canFetchReviews) {
        if (!canFetchReviews) {
          setReviews([]);
        }
        return;
      }
      
      try {
        setLoadingReviews(true);
        setReviewsError(null);
        const data = await reviewsAPI.getByProductId(selectedProduct.id);
        
        // Map backend review structure to frontend format
        const mappedReviews = data.map(review => ({
          reviewId: review.review_id,
          rating: review.rating,
          title: review.title,
          comment: review.comment,
          userName: review.user_name,
          date: review.created_at,
          verifiedPurchase: review.verified_purchase || false
        }));
        
        setReviews(mappedReviews);
      } catch (err) {
        console.error('Failed to fetch reviews:', err);
        setReviewsError('Failed to load reviews');
        setReviews([]);
      } finally {
        setLoadingReviews(false);
      }
    };

    fetchReviews();
  }, [selectedProduct?.id, canFetchReviews]);

  if (!selectedProduct) return null;

  const handleAddToCart = async () => {
    setAdding(true);
    try {
      const success = await addToCart(selectedProduct);
      if (success) {
        // Opcional: navegar para o cart após sucesso
        // navigate('/cart');
      }
    } finally {
      setAdding(false);
    }
  };

  const handleSubmitReview = async (newReview) => {
    if (!canSubmitReviews) {
      alert('Review submission is currently disabled.');
      return;
    }
    // Check if user is logged in
    if (!keycloak.authenticated) {
      alert('You must be logged in to write a review');
      keycloak.login();
      return;
    }

    // Check if user is a customer
    if (!currentUser.roles.includes('customer')) {
      alert('Only customers can write reviews');
      return;
    }

    // Check if username is available
    if (!currentUser.username) {
      alert('Username not found. Please try logging in again.');
      return;
    }

    try {
      // Prepare review data for API
      const reviewData = {
        product_id: selectedProduct.id,
        rating: newReview.rating,
        title: newReview.title,
        comment: newReview.comment,
        user_name: currentUser.username,
        verified_purchase: false,
        status: shouldModerate ? 'pending' : 'published',
        flagged: shouldModerate
      };

      console.log('Submitting review:', reviewData);
      console.log('Current user:', currentUser);

      // Submit review to API
      const createdReview = await reviewsAPI.create(reviewData);
      
      // Add the new review to the list with proper formatting
      const formattedReview = {
        reviewId: createdReview.review_id,
        rating: createdReview.rating,
        title: createdReview.title,
        comment: createdReview.comment,
        userName: createdReview.user_name,
        date: createdReview.created_at,
        verifiedPurchase: createdReview.verified_purchase || false
      };
      
      if (!shouldModerate) {
        setReviews([formattedReview, ...reviews]);
      }
      alert(shouldModerate ? 'Your review was submitted for moderation!' : 'Thank you for your review!');
    } catch (err) {
      console.error('Failed to submit review:', err);
      const errorMessage = err.message || 'Failed to submit review';
      alert('Failed to submit review: ' + errorMessage);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <Button
          onClick={() => navigate('/products')}
          variant="ghost"
          className="mb-4 flex items-center gap-2"
        >
          ← Back to Products
        </Button>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="bg-muted rounded-lg h-96 flex items-center justify-center overflow-hidden">
          {selectedProduct.image ? (
            <img 
              src={selectedProduct.image} 
              alt={selectedProduct.name}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = 'https://via.placeholder.com/800x600?text=No+Image';
              }}
            />
          ) : (
            <Package className="h-32 w-32 text-muted-foreground" />
          )}
          </div>

          <div>
            <h1 className="text-3xl font-bold mb-4">{selectedProduct.name}</h1>

            <div className="mb-4">
              <StarRating
                rating={selectedProduct.rating}
                reviews={selectedProduct.reviews}
              />
            </div>

            <p className="text-foreground mb-6">{selectedProduct.description}</p>

          <div className="mb-6">
            <ProductPriceCard 
              price={selectedProduct.price} 
              stock={selectedProduct.stock} 
            />
          </div>

          <div className="flex gap-4">
            <Button
              onClick={handleAddToCart}
              className="flex-1"
              size="lg"
              disabled={selectedProduct.stock === 0 || adding}
            >
              {adding ? 'Adding...' : selectedProduct.stock === 0 ? 'Out of Stock' : 'Add to Cart'}
            </Button>

            <Button
              onClick={() => navigate('/cart')}
              variant="outline"
              size="lg"
            >
              View Cart
            </Button>
          </div>
          </div>
        </div>

        {/* Reviews Section */}
        {loadingReviews ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
            <p className="mt-4 text-muted-foreground">Loading reviews...</p>
          </div>
        ) : reviewsError ? (
          <Card className="border-destructive mt-12">
            <div className="bg-destructive/10 text-destructive px-4 py-3">
              {reviewsError}
            </div>
          </Card>
      ) : (
          <ReviewsSection
            productId={selectedProduct.id}
            reviews={reviews}
            onSubmitReview={handleSubmitReview}
            currentUser={currentUser}
            canSubmitReviews={canSubmitReviews}
            canDisplayReviews={canDisplayReviews}
          />
        )}
      </CardContent>
    </Card>
  );
};

export default ProductDetailView;
