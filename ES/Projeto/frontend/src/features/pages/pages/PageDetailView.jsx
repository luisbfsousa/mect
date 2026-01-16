import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { blogAPI } from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Card, CardContent } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2, ArrowLeft, AlertCircle } from 'lucide-react';

const PageDetailView = () => {
  const { slug } = useParams(); // Note: This is actually the ID from the route
  const navigate = useNavigate();
  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (slug) {
      fetchPost();
    }
  }, [slug]);

  const fetchPost = async () => {
    try {
      setLoading(true);
      setError(null);
      // The 'slug' param is actually the post ID from the URL
      const data = await blogAPI.getPostById(slug);
      setPost(data);
    } catch (err) {
      console.error('Failed to fetch page:', err);
      setError('Failed to load the blog post. It may have been removed or you may not have permission to view it.');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading post...</p>
        </div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center max-w-md">
          <Alert variant="destructive" className="p-8">
            <AlertCircle className="h-16 w-16 mx-auto mb-4" />
            <h2 className="text-2xl font-bold mb-2">Page Not Found</h2>
            <AlertDescription className="mb-6">
              {error || "The page you're looking for doesn't exist or has been removed."}
            </AlertDescription>
            <Button onClick={() => navigate('/pages')}>
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Blog
            </Button>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <Button
          variant="ghost"
          onClick={() => navigate('/pages')}
          className="mb-6"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Blog
        </Button>

        <Card className="overflow-hidden shadow-lg">
          {post.image_url && (
            <div className="relative h-96 w-full">
              <img
                src={post.image_url}
                alt={post.title}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.nextElementSibling.style.display = 'flex';
                }}
              />
              <div className="hidden w-full h-full bg-gradient-to-br from-blue-100 to-blue-200 items-center justify-center">
                <svg className="w-24 h-24 text-blue-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            </div>
          )}

          <CardContent className="p-8 md:p-12">
            {post.theme && (
              <Badge variant="secondary" className="mb-4">
                {post.theme}
              </Badge>
            )}

            <h1 className="text-4xl md:text-5xl font-bold mb-4">
              {post.title}
            </h1>

            <div className="flex items-center text-muted-foreground text-sm mb-8 pb-8 border-b">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>{formatDate(post.created_at)}</span>
              
              {post.author_name && (
                <>
                  <span className="mx-3">•</span>
                  <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span>By {post.author_name}</span>
                </>
              )}
            </div>

            <div className="prose prose-lg max-w-none dark:prose-invert">
              <ReactMarkdown
                components={{
                  h1: ({node, ...props}) => <h1 className="text-3xl font-bold mt-8 mb-4" {...props} />,
                  h2: ({node, ...props}) => <h2 className="text-2xl font-bold mt-6 mb-3" {...props} />,
                  h3: ({node, ...props}) => <h3 className="text-xl font-bold mt-4 mb-2" {...props} />,
                  p: ({node, ...props}) => <p className="mb-4 leading-relaxed text-gray-700 dark:text-gray-200" {...props} />,
                  ul: ({node, ...props}) => <ul className="list-disc list-inside mb-4 space-y-2" {...props} />,
                  ol: ({node, ...props}) => <ol className="list-decimal list-inside mb-4 space-y-2" {...props} />,
                  li: ({node, ...props}) => <li className="text-gray-700 dark:text-gray-200" {...props} />,
                  a: ({node, ...props}) => <a className="text-blue-600 hover:text-blue-700 underline dark:text-blue-200" {...props} />,
                  blockquote: ({node, ...props}) => (
                    <blockquote className="border-l-4 border-blue-500 pl-4 italic my-4 text-gray-600 dark:text-gray-200" {...props} />
                  ),
                  code: ({node, inline, ...props}) => 
                    inline ? (
                      <code className="bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded text-sm font-mono text-gray-800 dark:text-gray-100" {...props} />
                    ) : (
                      <code className="block bg-gray-100 dark:bg-gray-700 p-4 rounded my-4 overflow-x-auto font-mono text-sm text-gray-800 dark:text-gray-100" {...props} />
                    ),
                  img: ({node, ...props}) => (
                    <img className="rounded-lg my-6 w-full" {...props} />
                  ),
                }}
              >
                {post.markdown_content || 'No content available.'}
              </ReactMarkdown>
            </div>
          </CardContent>
        </Card>

        <div className="mt-8 text-center">
          <Button onClick={() => navigate('/pages')}>
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to All Posts
          </Button>
        </div>
      </div>
    </div>
  );
};

export default PageDetailView;