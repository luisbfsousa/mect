import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import api from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Loader2, ArrowLeft } from 'lucide-react';

const BlogPostDetailView = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPost();
  }, [id]);

  const fetchPost = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/blog/posts/${id}`);
      setPost(response.data);
    } catch (err) {
      setError('Failed to load post');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="text-xl text-red-600 mb-4">{error || 'Post not found'}</div>
          <Button onClick={() => navigate('/blog')}>
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Blog
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header with back button */}
      <div className="bg-white dark:bg-gray-800 border-b dark:border-gray-700">
        <div className="max-w-4xl mx-auto px-6 py-6">
          <Button
            variant="ghost"
            onClick={() => navigate('/blog')}
            className="mb-4"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Blog
          </Button>
        </div>
      </div>

      {/* Post Content */}
      <article className="max-w-4xl mx-auto px-6 py-12">
        {/* Featured Image */}
        {post.image_url && (
          <img
            src={post.image_url}
            alt={post.title}
            className="w-full h-96 object-cover rounded-lg mb-8"
          />
        )}

        {/* Theme Badge */}
        {post.theme && (
          <Badge variant="secondary" className="mb-4">
            {post.theme}
          </Badge>
        )}

        {/* Title */}
        <h1 className="text-4xl font-bold mb-4">{post.title}</h1>

        {/* Author and Date */}
        <div className="flex items-center text-muted-foreground mb-8 pb-8 border-b">
          <span className="font-medium text-foreground">{post.author_name}</span>
          <span className="mx-3">•</span>
          <span>{new Date(post.created_at).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
          })}</span>
        </div>

        {/* Markdown Content */}
        <div className="prose prose-lg max-w-none dark:prose-invert">
          <ReactMarkdown>{post.markdown_content}</ReactMarkdown>
        </div>
      </article>
    </div>
  );
};

export default BlogPostDetailView;