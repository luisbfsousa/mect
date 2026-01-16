import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { blogAPI } from '../../../services/api';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Textarea } from '../../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../../components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Alert, AlertDescription } from '../../../components/ui/alert';
import { Loader2, AlertCircle } from 'lucide-react';

const BlogPostEditorView = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    image_url: '',
    theme: '',
    status: 'draft',
    markdown_content: '# Your Title Here\n\nStart writing your post...'
  });

  useEffect(() => {
    if (id) {
      fetchPost();
    }
  }, [id]);

  const fetchPost = async () => {
    try {
      setLoading(true);
      setError(null);
      const post = await blogAPI.getPostById(id);
      setFormData({
        title: post.title || '',
        image_url: post.image_url || '',
        theme: post.theme || '',
        status: post.status || 'draft',
        markdown_content: post.markdown_content || ''
      });
    } catch (err) {
      console.error('Failed to load post:', err);
      setError('Failed to load post');
      alert('Failed to load post: ' + err.message);
      navigate('/admin/blog');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (id) {
        await blogAPI.updatePost(id, formData);
        alert('Post updated successfully!');
      } else {
        await blogAPI.createPost(formData);
        alert('Post created successfully!');
      }
      navigate('/admin/blog');
    } catch (err) {
      console.error('Failed to save post:', err);
      setError('Failed to save post: ' + err.message);
      alert('Failed to save post: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading && id) {
    return (
      <div className="p-8 text-center">
        <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
        <p className="text-muted-foreground">Loading post...</p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6">
      <form onSubmit={handleSubmit}>
        <div className="mb-6 flex justify-between items-center">
          <h1 className="text-3xl font-bold">
            {id ? 'Edit Post' : 'New Post'}
          </h1>
          <div className="space-x-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/admin/blog')}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading}
            >
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {loading ? 'Saving...' : 'Save Post'}
            </Button>
          </div>
        </div>

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title *</Label>
                <Input
                  id="title"
                  type="text"
                  required
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                  placeholder="Post title..."
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="status">Status *</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({...formData, status: value})}
                >
                  <SelectTrigger id="status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="draft">Draft (Only visible to managers)</SelectItem>
                    <SelectItem value="published">Published (Visible on pages)</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="image_url">Image URL</Label>
                <Input
                  id="image_url"
                  type="url"
                  value={formData.image_url || ''}
                  onChange={(e) => setFormData({...formData, image_url: e.target.value})}
                  placeholder="https://example.com/image.jpg"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="theme">Theme / Category</Label>
                <Input
                  id="theme"
                  type="text"
                  value={formData.theme || ''}
                  onChange={(e) => setFormData({...formData, theme: e.target.value})}
                  placeholder="Technology, Business, etc."
                />
              </div>
            </div>

          {formData.image_url && (
            <div className="mt-4 space-y-2">
              <Label>Image Preview:</Label>
              <img
                src={formData.image_url}
                alt="Preview"
                className="h-40 w-auto rounded object-cover border"
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.nextElementSibling.style.display = 'block';
                }}
              />
              <p className="hidden text-sm text-destructive">
                Failed to load image. Please check the URL.
              </p>
            </div>
          )}
          </CardContent>
        </Card>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Markdown Editor</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Textarea
                value={formData.markdown_content}
                onChange={(e) => setFormData({...formData, markdown_content: e.target.value})}
                className="h-96 font-mono text-sm resize-none"
                placeholder="# Your title here&#10;&#10;Start writing your content..."
              />
              <Alert>
                <AlertDescription>
                  <p className="font-semibold mb-1">Markdown Tips:</p>
                  <ul className="list-disc list-inside space-y-1 text-xs">
                    <li># Heading 1, ## Heading 2, ### Heading 3</li>
                    <li>**bold text**, *italic text*</li>
                    <li>[Link text](https://url.com)</li>
                    <li>- List item or 1. Numbered item</li>
                    <li>![Alt text](image-url.jpg) for images</li>
                  </ul>
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Preview</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="prose prose-sm dark:prose-invert max-w-none overflow-auto h-[500px]">
                <ReactMarkdown
                  components={{
                    p: ({node, ...props}) => <p className="mb-4" {...props} />
                  }}
                >
                  {formData.markdown_content}
                </ReactMarkdown>
              </div>
            </CardContent>
          </Card>
        </div>
      </form>
    </div>
  );
};

export default BlogPostEditorView;