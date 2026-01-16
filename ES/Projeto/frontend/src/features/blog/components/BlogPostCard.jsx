import React from 'react';
import { Card, CardContent } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';

const BlogPostCard = ({ post, onClick }) => {
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const getExcerpt = (content, maxLength = 150) => {
    if (!content) return 'No content available';
    
    // Remove markdown syntax for excerpt
    const plainText = content
      .replace(/#{1,6}\s/g, '')
      .replace(/\*\*/g, '')
      .replace(/\*/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/\n/g, ' ')
      .trim();
    
    if (plainText.length <= maxLength) return plainText;
    return plainText.substring(0, maxLength).trim() + '...';
  };

  return (
    <Card
      onClick={onClick}
      className="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer group"
    >
      {/* Image */}
      {post.image_url ? (
        <div className="relative h-48 overflow-hidden">
          <img
            src={post.image_url}
            alt={post.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={(e) => {
              e.target.style.display = 'none';
              e.target.nextElementSibling.style.display = 'flex';
            }}
          />
          <div className="hidden w-full h-full bg-gradient-to-br from-blue-100 to-blue-200 items-center justify-center">
            <svg className="w-16 h-16 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
        </div>
      ) : (
        <div className="h-48 bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center">
          <svg className="w-16 h-16 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
      )}

      {/* Content */}
      <CardContent className="p-6">
        {/* Theme/Category Badge */}
        {post.theme && (
          <Badge variant="secondary" className="mb-3">
            {post.theme}
          </Badge>
        )}

        {/* Title */}
        <h3 className="text-xl font-bold mb-2 group-hover:text-primary transition-colors line-clamp-2">
          {post.title}
        </h3>

        {/* Excerpt */}
        <p className="text-muted-foreground text-sm mb-4 line-clamp-3">
          {getExcerpt(post.markdown_content)}
        </p>

        {/* Footer */}
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>{formatDate(post.created_at)}</span>
          <span className="text-primary group-hover:underline font-medium">
            Read more →
          </span>
        </div>
      </CardContent>
    </Card>
  );
};

export default BlogPostCard;