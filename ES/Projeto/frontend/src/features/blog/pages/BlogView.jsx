import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../services/api';
import { Card, CardContent } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Loader2 } from 'lucide-react';

const BlogView = () => {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchPublishedPosts();
  }, []);

  const fetchPublishedPosts = async () => {
    try {
      setLoading(true);
      const response = await api.get('/blog/posts');
      setPosts(response.data);
    } catch (err) {
      setError('Failed to load blog posts');
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

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="bg-white dark:bg-gray-800 border-b dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-6 py-12">
          <h1 className="text-4xl font-bold mb-2 text-gray-900 dark:text-gray-100">Blog</h1>
          <p className="text-gray-600 dark:text-gray-300">Latest news and updates</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-12">
        {posts.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">No posts published yet</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {posts.map(post => (
              <Card
                key={post.id}
                onClick={() => navigate(`/blog/${post.id}`)}
                className="cursor-pointer hover:shadow-lg transition-shadow overflow-hidden"
              >
                {post.image_url && (
                  <img
                    src={post.image_url}
                    alt={post.title}
                    className="w-full h-48 object-cover"
                  />
                )}

                <CardContent className="p-6">
                  {post.theme && (
                    <Badge variant="secondary" className="mb-3">
                      {post.theme}
                    </Badge>
                  )}

                  <h2 className="text-xl font-bold mb-2 line-clamp-2">
                    {post.title}
                  </h2>

                  <div className="flex items-center text-sm text-muted-foreground">
                    <span>{post.author_name}</span>
                    <span className="mx-2">•</span>
                    <span>{new Date(post.created_at).toLocaleDateString()}</span>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default BlogView;