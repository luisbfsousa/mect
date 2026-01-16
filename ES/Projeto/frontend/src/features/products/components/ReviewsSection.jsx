import React, { useState } from 'react';
import { Star } from 'lucide-react';
import { Button } from '../../../components/ui/button';
import { Card, CardContent } from '../../../components/ui/card';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';

const ReviewsSection = ({
  productId,
  reviews = [],
  onSubmitReview,
  currentUser,
  canSubmitReviews,
  canDisplayReviews
}) => {
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [newReview, setNewReview] = useState({
    rating: 5,
    title: '',
    comment: ''
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSubmitReview) {
      onSubmitReview({
        ...newReview,
        productId,
        date: new Date().toISOString()
      });
    }
    // Reset form
    setNewReview({
      rating: 5,
      title: '',
      comment: ''
    });
    setShowReviewForm(false);
  };

  const renderStars = (rating) => {
    return (
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            size={16}
            className={star <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}
          />
        ))}
      </div>
    );
  };

  const averageRating = reviews.length > 0
    ? (reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length).toFixed(1)
    : 0;

  const ratingDistribution = [5, 4, 3, 2, 1].map(rating => ({
    rating,
    count: reviews.filter(r => r.rating === rating).length,
    percentage: reviews.length > 0
      ? (reviews.filter(r => r.rating === rating).length / reviews.length) * 100
      : 0
  }));

  return (
    <div className="mt-12">
      <h2 className="text-2xl font-bold mb-6">Customer Reviews</h2>

      {/* Reviews Summary */}
      <Card className="mb-6">
        <CardContent className="p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Average Rating */}
          <div className="flex items-center gap-4">
            <div className="text-center">
              <div className="text-5xl font-bold">{averageRating}</div>
              <div className="flex justify-center mt-2 mb-1">
                {renderStars(Math.round(averageRating))}
              </div>
              <div className="text-sm text-muted-foreground">
                {reviews.length} {reviews.length === 1 ? 'review' : 'reviews'}
              </div>
            </div>
          </div>

          {/* Rating Distribution */}
          <div className="space-y-2">
            {ratingDistribution.map(({ rating, count, percentage }) => (
              <div key={rating} className="flex items-center gap-2">
                <span className="text-sm font-medium w-12">{rating} stars</span>
                <div className="flex-1 bg-muted rounded-full h-2">
                  <div
                    className="bg-yellow-400 dark:bg-yellow-500 h-2 rounded-full"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
                <span className="text-sm text-muted-foreground w-8">{count}</span>
              </div>
            ))}
          </div>
        </div>

          {/* Write Review Button */}
          <div className="mt-6">
            {canSubmitReviews && currentUser && currentUser.roles.includes('customer') ? (
              <Button
                onClick={() => setShowReviewForm(!showReviewForm)}
                variant="secondary"
                className="w-full md:w-auto"
              >
                {showReviewForm ? 'Cancel' : 'Write a Review'}
              </Button>
            ) : (
              <p className="text-sm text-muted-foreground">
                {canSubmitReviews
                  ? (!currentUser
                      ? 'You must be logged in to write a review.'
                      : 'Only customers can write reviews.')
                  : 'Review submissions are currently disabled.'}
              </p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Review Form */}
      {showReviewForm && canSubmitReviews && (
        <Card className="mb-6">
          <CardContent className="p-6">
            <h3 className="text-lg font-semibold mb-4">Write Your Review</h3>

            <form onSubmit={handleSubmit}>
              {/* Rating Selection */}
              <div className="mb-4">
                <Label className="mb-2 block">
                  Your Rating *
                </Label>
                <div className="flex gap-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setNewReview({ ...newReview, rating: star })}
                      className="focus:outline-none"
                    >
                      <Star
                        size={32}
                        className={
                          star <= newReview.rating
                            ? 'fill-yellow-400 text-yellow-400 hover:scale-110 transition'
                            : 'text-muted hover:text-yellow-400 hover:scale-110 transition'
                        }
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Review Title */}
              <div className="mb-4">
                <Label htmlFor="reviewTitle" className="mb-2 block">
                  Review Title *
                </Label>
                <Input
                  id="reviewTitle"
                  type="text"
                  required
                  value={newReview.title}
                  onChange={(e) => setNewReview({ ...newReview, title: e.target.value })}
                  placeholder="Sum up your review in one line"
                />
              </div>

              {/* Review Comment */}
              <div className="mb-4">
                <Label htmlFor="reviewComment" className="mb-2 block">
                  Your Review *
                </Label>
                <textarea
                  id="reviewComment"
                  required
                  value={newReview.comment}
                  onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                  className="w-full px-3 py-2 bg-background border border-input rounded-md focus:outline-none focus:ring-2 focus:ring-ring min-h-[100px]"
                  rows="4"
                  placeholder="Share your experience with this product"
                />
              </div>

              <div className="flex gap-3">
                <Button type="submit" className="flex-1">
                  Submit Review
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowReviewForm(false)}
                >
                  Cancel
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Reviews List */}
      <div className="space-y-4">
        {!canDisplayReviews ? (
          <Card>
            <CardContent className="text-center py-12">
              <p className="text-muted-foreground">Review listings are currently disabled.</p>
            </CardContent>
          </Card>
        ) : reviews.length === 0 ? (
          <Card>
            <CardContent className="text-center py-12">
              <p className="text-muted-foreground mb-4">No reviews yet. Be the first to review this product!</p>
              {canSubmitReviews && !showReviewForm && (
                <Button onClick={() => setShowReviewForm(true)}>
                  Write the First Review
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          reviews.map((review, index) => (
            <Card key={index}>
              <CardContent className="p-6">
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <div className="flex items-center gap-3 mb-2">
                      {renderStars(review.rating)}
                      <span className="font-semibold text-lg">{review.title}</span>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      By {review.userName} on {new Date(review.date).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                <p className="leading-relaxed">{review.comment}</p>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default ReviewsSection;
