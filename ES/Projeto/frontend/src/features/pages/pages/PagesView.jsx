import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { blogAPI } from '../../../services/api';
import BlogPostCard from '../../blog/components/BlogPostCard';
import BlogSearchBar from '../../blog/components/BlogSearchBar';
import { Button } from '../../../components/ui/button';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2, AlertCircle } from 'lucide-react';

const PagesView = () => {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [filteredPosts, setFilteredPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchPosts();
  }, []);

  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredPosts(posts);
    } else {
      const query = searchQuery.toLowerCase();
      const filtered = posts.filter(post =>
        post.title.toLowerCase().includes(query) ||
        (post.theme && post.theme.toLowerCase().includes(query)) ||
        (post.markdown_content && post.markdown_content.toLowerCase().includes(query))
      );
      setFilteredPosts(filtered);
    }
  }, [searchQuery, posts]);

  const fetchPosts = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await blogAPI.getPublishedPosts();
      setPosts(data);
      setFilteredPosts(data);
    } catch (err) {
      console.error('Failed to fetch blog posts:', err);
      setError('Failed to load blog posts. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handlePostClick = (post) => {
    navigate(`/pages/${post.id}`);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading blog posts...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center max-w-md">
          <Alert variant="destructive" className="p-6">
            <AlertCircle className="h-16 w-16 mx-auto mb-4" />
            <h2 className="text-xl font-semibold mb-2">Unable to Load Posts</h2>
            <AlertDescription className="mb-4">{error}</AlertDescription>
            <Button onClick={fetchPosts} variant="destructive">
              Try Again
            </Button>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-gray-100 mb-2">Blog & Articles</h1>
        <p className="text-lg text-gray-600 dark:text-gray-300">Explore our latest posts and insights</p>
      </div>

      <div className="mb-8">
        <BlogSearchBar 
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
        />
      </div>

      {filteredPosts.length === 0 ? (
        <div className="text-center py-16">
          <svg className="w-24 h-24 text-gray-300 dark:text-gray-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 className="text-xl font-semibold text-gray-600 dark:text-gray-300 mb-2">
            {searchQuery ? 'No posts found' : 'No posts available'}
          </h3>
          <p className="text-gray-500 dark:text-gray-400">
            {searchQuery 
              ? 'Try adjusting your search query'
              : 'Check back later for new content'
            }
          </p>
          {searchQuery && (
            <Button onClick={() => setSearchQuery('')} className="mt-4">
              Clear Search
            </Button>
          )}
        </div>
      ) : (
        <>
          <div className="mb-4 text-sm text-gray-600">
            Showing {filteredPosts.length} {filteredPosts.length === 1 ? 'post' : 'posts'}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredPosts.map((post) => (
              <BlogPostCard
                key={post.id}
                post={post}
                onClick={() => handlePostClick(post)}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default PagesView;